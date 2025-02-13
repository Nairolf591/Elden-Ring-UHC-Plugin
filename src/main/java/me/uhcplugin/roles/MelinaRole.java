package me.uhcplugin.roles;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import me.uhcplugin.Main;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MelinaRole implements Listener, CommandExecutor {
    private final Main plugin;
    private final HashMap<UUID, Long> healCooldowns = new HashMap<>(); // Cooldown des soins
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();

    public MelinaRole(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private final Map<UUID, Long> visionCooldowns = new HashMap<>(); // Cooldown de la vision

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "❌ Seuls les joueurs peuvent exécuter cette commande !");
            return true;
        }

        Player melina = (Player) sender;

        // ✅ Vérifie que le joueur est bien Mélina
        if (!plugin.getRoleManager().getRole(melina).equalsIgnoreCase("Melina")) {
            melina.sendMessage(ChatColor.RED + "❌ Seule Mélina peut utiliser cette commande !");
            return true;
        }

        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "soin":
                return executeHealing(melina, args);

            case "vision":
                return executeVision(melina, args);
        }

        return false;
    }

    /**
     * 🔮 Exécute la capacité Vision des Âmes 👁️
     */
    private boolean executeVision(Player melina, String[] args) {
        // ✅ Vérifie si un joueur a été spécifié
        if (args.length != 1) {
            melina.sendMessage(ChatColor.RED + "❌ Utilisation : /vision <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        // ✅ Vérifie si le joueur cible est valide
        if (target == null || !target.isOnline()) {
            melina.sendMessage(ChatColor.RED + "❌ Le joueur " + args[0] + " n’est pas en ligne !");
            return true;
        }

        // ✅ Vérifie si la capacité est en cooldown
        UUID melinaUUID = melina.getUniqueId();
        if (visionCooldowns.containsKey(melinaUUID)) {
            long cooldownEnd = visionCooldowns.get(melinaUUID);
            long timeLeft = (cooldownEnd - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                melina.sendMessage(ChatColor.RED + "❌ Vision des Âmes est encore en cooldown pour " + timeLeft + " secondes !");
                return true;
            }
        }

        // ✅ Vérifie si Mélina a assez de mana
        if (!plugin.getManaManager().consumeMana(melina, 60)) {
            melina.sendMessage(ChatColor.RED + "❌ Tu n’as pas assez de mana pour utiliser Vision des Âmes !");
            return true;
        }

        // ✅ Récupère le camp du joueur ciblé
        String camp;
        if (plugin.getRoleManager().getRole(target).equalsIgnoreCase("Maliketh")) {
            // Vérifie si Maliketh est en Phase 1
            MalikethRole malikethRole = plugin.getMalikethRole(); // Assurez-vous que cette méthode existe dans Main
            if (malikethRole != null && malikethRole.isInPhase1(target.getUniqueId())) {
                camp = "Table ronde"; // Maliketh en Phase 1 appartient à la Table Ronde
            } else {
                camp = plugin.getRoleManager().getCamp(target).getDisplayName(); // Sinon, camp d'origine
            }
        } else {
            camp = plugin.getRoleManager().getCamp(target).getDisplayName(); // Camp d'origine pour les autres joueurs
        }

        // ✅ Affiche le camp du joueur ciblé
        melina.sendMessage(ChatColor.AQUA + "🔮 Vision des Âmes activée...");
        melina.sendMessage(ChatColor.LIGHT_PURPLE + "✨ Le joueur " + ChatColor.WHITE + target.getName() + ChatColor.LIGHT_PURPLE + " appartient au camp : " + ChatColor.GOLD + camp);

        // ✅ Applique le cooldown de 6 minutes
        visionCooldowns.put(melinaUUID, System.currentTimeMillis() + (6 * 60 * 1000));

        // ✅ Met à jour le mana affiché
        plugin.getManaManager().updateManaDisplay(melina);

        return true;
    }

    private boolean executeHealing(Player melina, String[] args) {
        if (args.length != 1) {
            melina.sendMessage(ChatColor.RED + "❌ Utilisation : /soin <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            melina.sendMessage(ChatColor.RED + "❌ Le joueur " + args[0] + " n’est pas en ligne !");
            return true;
        }

        // Vérification du cooldown
        UUID melinaUUID = melina.getUniqueId();
        if (healCooldowns.containsKey(melinaUUID)) {
            long remaining = (healCooldowns.get(melinaUUID) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                melina.sendMessage(ChatColor.RED + "⏳ Tu dois encore attendre " + remaining + " secondes avant d’utiliser Soin Divin !");
                return true;
            }
        }

        // Vérification du mana
        if (!plugin.getManaManager().consumeMana(melina, 35)) {
            melina.sendMessage(ChatColor.RED + "❌ Tu n’as pas assez de mana pour utiliser Soin Divin !");
            return true;
        }

        // ✅ Applique le soin progressif (6 cœurs en 10 secondes)
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 10) {
                    return;
                }
                target.setHealth(Math.min(target.getHealth() + 1.2, target.getMaxHealth())); // Ajoute 1,2 PV par seconde
                ticks++;
            }
        }, 0L, 20L);

        // ✅ Active le cooldown
        healCooldowns.put(melinaUUID, System.currentTimeMillis() + (5 * 60 * 1000));

        // ✅ Messages de confirmation
        melina.sendMessage(ChatColor.AQUA + "✨ Tu as utilisé " + ChatColor.GOLD + "Soin Divin ✨💖" + ChatColor.AQUA + " sur " + ChatColor.WHITE + target.getName() + " !");
        target.sendMessage(ChatColor.LIGHT_PURPLE + "💖 Une lumière dorée t’enveloppe, tu récupères peu à peu tes forces...");
        plugin.getManaManager().updateManaDisplay(melina);

        return true;
    }

    @EventHandler
    public void onDashUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // ✅ Vérifie que l'event vient bien d'un clic droit et de la main principale
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;

        // ✅ Vérifie que c'est bien Mélina et qu'elle a l'artefact
        String role = plugin.getRoleManager().getRole(player);
        ItemStack item = player.getInventory().getItemInMainHand();

        if (role == null || !role.equalsIgnoreCase("Melina")) return; // ✅ Vérifie null
        if (item.getType() != Material.NETHER_STAR || !item.hasItemMeta() ||
                !item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "⚡ Lumière de Grâce")) {
            return;
        }

        // ✅ Vérifie le cooldown
        UUID playerUUID = player.getUniqueId();
        if (dashCooldowns.containsKey(playerUUID)) {
            long timeLeft = (dashCooldowns.get(playerUUID) - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "❌ Tu dois encore attendre " + timeLeft + " secondes avant de réutiliser cette capacité !");
                return;
            }
        }

        // ✅ Vérifie le mana
        if (!plugin.getManaManager().consumeMana(player, 10)) {
            player.sendMessage(ChatColor.RED + "❌ Tu n’as pas assez de mana pour utiliser la Lumière de Grâce !");
            return;
        }
        plugin.getManaManager().updateManaDisplay(player); // ✅ Met à jour le scoreboard

        // ✅ Active le cooldown
        dashCooldowns.put(playerUUID, System.currentTimeMillis() + (4 * 60 * 1000)); // 4 minutes de cooldown

        // ✅ Calcule la destination (15 blocs en avant)
        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize().multiply(15);
        Location destination = start.clone().add(direction);

        // ✅ Évite que Mélina se coince dans un mur
        if (destination.getBlock().getType().isSolid()) {
            destination.setY(destination.getWorld().getHighestBlockYAt(destination) + 1);
        }

        // ✅ Génère des particules sur la trajectoire
        new BukkitRunnable() {
            double t = 0;
            Location loc = start.clone();

            @Override
            public void run() {
                if (t >= 1) {
                    this.cancel();
                    return;
                }

                loc.add(direction.clone().multiply(t));
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 10, 0.1, 0.1, 0.1, 0.01);

                t += 0.1;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // ✅ Téléporte Mélina avec un léger boost
        player.setVelocity(direction.multiply(0.3));
        player.teleport(destination);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        player.sendMessage(ChatColor.GOLD + "✨ Tu utilises la " + ChatColor.YELLOW + "⚡ Lumière de Grâce" + ChatColor.GOLD + " et bondis en avant !");
    }

    public static ItemStack getMelinaArtifact() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "⚡ Lumière de Grâce");
            meta.setLore(Collections.singletonList(ChatColor.YELLOW + "Utilise cet artefact pour te propulser en avant."));
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void giveArtifactToMelina(Player player) {
        if (plugin.getRoleManager().getRole(player).equalsIgnoreCase("Melina")) {
            player.getInventory().addItem(getMelinaArtifact());
            player.sendMessage(ChatColor.AQUA + "✨ Un étrange artefact apparaît dans ton inventaire...");
        }
    }


}