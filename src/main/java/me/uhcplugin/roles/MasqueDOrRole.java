package me.uhcplugin.roles;

import me.uhcplugin.Main;
import me.uhcplugin.Camp;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MasqueDOrRole implements Listener, CommandExecutor {

    private final Main plugin;
    private final Map<UUID, Long> meditationCooldown = new HashMap<>();
    private final Map<UUID, Long> commandCooldown = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Set<UUID> meditatingPlayers = new HashSet<>();
    private final Map<UUID, BukkitRunnable> pendingCommands = new HashMap<>();
    private static final int DETECTION_RANGE = 40;

    public MasqueDOrRole(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startPassiveRegeneration();
        startCooldownNotifier();
    }

    private void startPassiveRegeneration() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isMasqueDOr(player)) {
                        player.setHealth(Math.min(player.getHealth() + 1, player.getMaxHealth()));
                    }
                }
            }
        }.runTaskTimer(plugin, 60L, 60L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isMasqueDOr(player)) return;

        if (item.getType() == Material.NETHER_STAR && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().contains("Méditation Sacrée")) {

            startMeditation(player);
        }
    }

    private void startMeditation(Player player) {
        UUID uuid = player.getUniqueId();

        // Vérification armure
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR) {
                player.sendMessage(ChatColor.RED + "❌ Retirez votre armure pour méditer !");
                return;
            }
        }

        if (meditationCooldown.containsKey(uuid)) {
            long remaining = (meditationCooldown.get(uuid) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage(ChatColor.RED + "⏳ Méditation en cooldown : " + remaining + "s");
                return;
            }
        }

        new BukkitRunnable() {
            int timer = 0;
            Location startLoc = player.getLocation();

            @Override
            public void run() {
                if (timer >= 40) { // 2 secondes
                    completeMeditation(player);
                    cancel();
                    return;
                }

                // Vérification mouvement
                if (player.getLocation().distanceSquared(startLoc) > 0.5) {
                    player.sendMessage(ChatColor.RED + "❌ Méditation interrompue !");
                    cancel();
                    return;
                }

                // Effets visuels dorés
                player.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 5,
                        new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1));
                timer++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void completeMeditation(Player player) {
        // Effets
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1)); // Regen II pendant 5s
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 3600, 5)); // 6 coeurs pendant 3min

        // Sons/Effets
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.8f);
        player.spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.2);

        meditationCooldown.put(player.getUniqueId(), System.currentTimeMillis() + (6 * 60 * 1000));
        player.sendMessage(ChatColor.GOLD + "✨ Méditation réussie !");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!isMasqueDOr(player)) return true;

        if (commandCooldown.containsKey(player.getUniqueId())) {
            long remaining = (commandCooldown.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                player.sendMessage(ChatColor.RED + "⏳ Décret en cooldown : " + remaining + "s");
                return true;
            }
        }

        player.sendMessage(ChatColor.GOLD + "🔮 Activation du Décret dans 30 secondes...");

        new BukkitRunnable() {
            @Override
            public void run() {
                executeDecret(player);
            }
        }.runTaskLater(plugin, 600L); // 30 secondes

        commandCooldown.put(player.getUniqueId(), System.currentTimeMillis() + (20 * 60 * 1000));
        return true;
    }

    private void executeDecret(Player player) {
        int bastionCount = countBastionPlayers(player);
        int bossCount = countBossPlayers(player);

        if (bastionCount >= bossCount && bastionCount >= 4) {
            revealPlayers(player);
        } else {
            // Speed I (amplifier 0)
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 0));
            player.sendMessage(ChatColor.YELLOW + "⚡ Vitesse accrue pendant 30 secondes !");
        }
    }

    private void revealPlayers(Player masque) {
        List<String> players = new ArrayList<>();
        for (Player p : masque.getWorld().getPlayers()) {
            if (p.getLocation().distance(masque.getLocation()) <= DETECTION_RANGE) {
                players.add(p.getName());
            }
        }

        masque.sendMessage(ChatColor.GOLD + "=== Joueurs détectés (" + players.size() + ") ===");
        players.forEach(name -> masque.sendMessage(ChatColor.YELLOW + "• " + name));
        masque.sendMessage(ChatColor.GOLD + "========================");
    }

    private int countBastionPlayers(Player player) {
        int count = 0;
        for (Player p : player.getWorld().getPlayers()) {
            if (p.getLocation().distance(player.getLocation()) <= DETECTION_RANGE &&
                    plugin.getRoleManager().getCamp(p) == Camp.TABLE_RONDE) {
                count++;
            }
        }
        return count;
    }

    private int countBossPlayers(Player player) {
        // Implémenter la logique de détection des boss selon votre configuration
        return 0;
    }


    private void startCooldownNotifier() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isMasqueDOr(p)) continue;

                    UUID uuid = p.getUniqueId();

                    // Notification Méditation
                    if (meditationCooldown.containsKey(uuid) && meditationCooldown.get(uuid) <= System.currentTimeMillis()) {
                        p.sendMessage(ChatColor.GREEN + "✅ Méditation Sacrée disponible !");
                        meditationCooldown.remove(uuid);
                    }

                    // Notification Décret
                    if (commandCooldown.containsKey(uuid) && commandCooldown.get(uuid) <= System.currentTimeMillis()) {
                        p.sendMessage(ChatColor.GREEN + "✅ Décret du Savoir Divin prêt !");
                        commandCooldown.remove(uuid);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static ItemStack getMasqueDOrItems() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "🌟 Méditation Sacrée");
        meta.setLore(Arrays.asList(
                ChatColor.YELLOW + "Immobile sans armure pendant 2 secondes",
                ChatColor.LIGHT_PURPLE + "Effets: Régénération II + Absorption (6❤)",
                ChatColor.RED + "Cooldown: 6 minutes"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isMasqueDOr(Player player) {
        return "Masque d'Or".equalsIgnoreCase(plugin.getRoleManager().getRole(player));
    }
}