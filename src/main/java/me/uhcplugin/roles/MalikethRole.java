package me.uhcplugin.roles;

import me.uhcplugin.Main;
import me.uhcplugin.RoleManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class MalikethRole implements Listener, CommandExecutor {

    private final Main plugin;
    private final Set<UUID> hasTransformed = new HashSet<>(); // Liste des joueurs ayant déjà utilisé /maliketh_phase

    public MalikethRole(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("maliketh_phase").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                handlePhaseChange(player);
            }
            return true;
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("maliketh_phase")) {
                handlePhaseChange(player);
                return true;
            }
        }
        return false;
    }


    public void handlePhaseChange(Player player) {
        // Vérifie si le joueur est bien Maliketh
        if (!"Maliketh".equalsIgnoreCase(plugin.getRoleManager().getRole(player))) {
            player.sendMessage(ChatColor.RED + "❌ Tu n'es pas Maliketh !");
            return;
        }

        // Vérifie si le joueur a déjà transformé
        if (hasTransformed.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Tu as déjà utilisé ta transformation !");
            return;
        }

        hasTransformed.add(player.getUniqueId()); // Marque la transformation comme utilisée
        player.sendMessage(ChatColor.DARK_RED + "📢 \"Oh death, become my blade once more...\"");

        // **Effets visuels et sonores pendant la transformation**
        Location loc = player.getLocation();
        player.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        player.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
        player.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 1, 1, 1, 0.1);
        player.spawnParticle(Particle.SOUL, loc, 100, 1, 2, 1, 0.05);

        // Bloque le joueur pendant la transformation (5 sec)
        player.setWalkSpeed(0);
        player.setInvulnerable(true);

        new BukkitRunnable() {
            int countdown = 5; // Durée de la transformation (5 sec)

            @Override
            public void run() {
                if (countdown <= 0) {
                    cancel();
                    completeTransformation(player);
                } else {
                    player.sendMessage(ChatColor.DARK_RED + "⚔️ Transformation en Lame Noire... (" + countdown + "s)");
                    countdown--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void completeTransformation(Player player) {
        player.sendMessage(ChatColor.RED + "🔥 Tu es maintenant la Lame Noire !");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
        player.spawnParticle(Particle.FLAME, player.getLocation(), 150, 1, 2, 1, 0.1);

        // Débloque le joueur et applique les effets passifs de la phase 2
        player.setWalkSpeed(0.2f); // Restaure la vitesse
        player.setInvulnerable(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 999999, 0, false, false));
        // ⚠️ À FAIRE PLUS TARD : Modifier l’affichage du scoreboard pour signaler la transformation
    }

    public void giveMalikethItems(Player player) {
        // Création de la Nether Star pour l’attaque Rupture Bestiale
        ItemStack ruptureBestiale = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = ruptureBestiale.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_RED + "1️⃣ Rupture Bestiale 🦴💥");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Frappe le sol et projette une onde de choc.",
                ChatColor.RED + "Dégâts : 2 cœurs",
                ChatColor.GOLD + "Repoussement : 3 blocs",
                ChatColor.AQUA + "Recharge : 3 minutes"
        ));
        ruptureBestiale.setItemMeta(meta);

        // Donne l’item au joueur
        player.getInventory().addItem(ruptureBestiale);
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Vérifie que le joueur est bien Maliketh
        if (!"Maliketh".equalsIgnoreCase(plugin.getRoleManager().getRole(player))) return;

        // Vérifie que le joueur clique avec une Nether Star renommée "Rupture Bestiale"
        if (player.getInventory().getItemInMainHand().getType() == Material.NETHER_STAR) {
            ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
            if (meta != null && meta.getDisplayName().contains("Rupture Bestiale")) {
                activateRuptureBestiale(player);
            }
        }
    }
    private final Set<UUID> ruptureCooldown = new HashSet<>(); // Liste des joueurs en cooldown

    public void activateRuptureBestiale(Player player) {
        // Vérifie le cooldown
        if (ruptureCooldown.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Rupture Bestiale est en recharge !");
            return;
        }

        // Ajoute le joueur au cooldown (3 minutes)
        ruptureCooldown.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> ruptureCooldown.remove(player.getUniqueId()), 20L * 180);

        // Effets sonores & visuels
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 3);
        player.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 30, 1, 1, 1, 0.2, Material.STONE.createBlockData());

        // Effet sur les ennemis proches
        double radius = 5.0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player) && target.getLocation().distance(loc) <= radius) {
                target.damage(4.0); // 2 cœurs de dégâts (1 cœur = 2 de dégâts)
                target.setVelocity(target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.5));
                target.sendMessage(ChatColor.RED + "💥 Une onde de choc t’a repoussé !");
            }
        }
    }


}