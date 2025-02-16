package me.uhcplugin;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DeathManager implements Listener {

    private final Main plugin;
    // Stocke le compte à rebours de la mort pour chaque joueur
    private final Map<UUID, BukkitRunnable> deathCountdowns = new HashMap<>();
    // Stocke les items du joueur au moment de la mort
    private final Map<UUID, List<ItemStack>> savedItems = new HashMap<>();
    // Flag pour activer/désactiver le système de mort custom
    private boolean enabled = true;

    public DeathManager(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Si le système est désactivé, laisser le comportement par défaut
        if (!enabled) return;

        Player player = event.getEntity();
        if (!GameManager.isPlaying()) return;

        // Récupérer et sauvegarder les items (la liste des drops) pour restauration ultérieure
        List<ItemStack> droppedItems = new ArrayList<>(event.getDrops());
        savedItems.put(player.getUniqueId(), new ArrayList<>(droppedItems));
        event.getDrops().clear();
        event.setDeathMessage(null);
        Location deathLocation = player.getLocation();

        // Téléportation immédiate vers le spawn en hauteur
        Location deathSpawn = getDeathSpawnLocation(player.getWorld());
        player.teleport(deathSpawn);

        // Rendre le joueur invincible et invisible pendant 8 secondes
        player.setInvulnerable(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 1, false, false));

        // Récupération du rôle et de sa couleur
        String role = plugin.getRoleManager().getRole(player);
        ChatColor roleColor = getRoleColor(role);

        // Message de mort stylisé sur 3 lignes
        String deathMessage = ChatColor.GRAY + "------------------------------------\n" +
                ChatColor.RED + "☠ " + ChatColor.WHITE + "Le joueur " + ChatColor.GOLD + player.getName() +
                ChatColor.WHITE + " est mort, il était " + roleColor + role + ChatColor.WHITE + ".\n" +
                ChatColor.GRAY + "------------------------------------";

        // Création du compte à rebours de mort (8 secondes = 160 ticks)
        BukkitRunnable deathTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Retirer invulnérabilité et invisibilité
                player.setInvulnerable(false);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);

                // Diffuser le message de mort et jouer le son d'éclair
                Bukkit.broadcastMessage(deathMessage);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.playSound(deathLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }

                // Déposer les items en filtrant les Nether Stars
                List<ItemStack> items = savedItems.get(player.getUniqueId());
                if (items != null) {
                    for (ItemStack item : items) {
                        if (item != null && item.getType() != Material.NETHER_STAR) {
                            player.getWorld().dropItemNaturally(deathLocation, item);
                        }
                    }
                    savedItems.remove(player.getUniqueId());
                }

                // Passage en mode spectateur (définit définitivement la mort)
                player.setGameMode(GameMode.SPECTATOR);
                deathCountdowns.remove(player.getUniqueId());
            }
        };
        deathCountdowns.put(player.getUniqueId(), deathTask);
        deathTask.runTaskLater(plugin, 160L);
    }

    private Location getDeathSpawnLocation(World world) {
        Location spawn = world.getSpawnLocation();
        spawn.setY(spawn.getY() + 50);
        return spawn;
    }

    private ChatColor getRoleColor(String role) {
        if (role == null) return ChatColor.WHITE;
        String r = role.toLowerCase();
        // Rôles du Bastion (affichés en vert) sauf Ranni
        if (r.equals("melina") || r.equals("sans-éclat") || r.equals("d_témoin_de_la_mort") ||
                r.equals("blaid") || r.equals("lionel")) {
            return ChatColor.GREEN;
        }
        // Rôles solo (affichés en jaune) : ici Ranni et Radahn
        else if (r.equals("ranni") || r.equals("radahn")) {
            return ChatColor.YELLOW;
        }
        // Rôles boss (affichés en rouge)
        else if (r.equals("godrick") || r.equals("morgott") || r.equals("margit") || r.equals("maliketh")) {
            return ChatColor.RED;
        }
        return ChatColor.WHITE;
    }

    // Méthode pour annuler le compte à rebours et réanimer un joueur
    public boolean reanimate(Player player) {
        BukkitRunnable task = deathCountdowns.get(player.getUniqueId());
        if (task != null) {
            task.cancel();
            deathCountdowns.remove(player.getUniqueId());
            // Retirer invulnérabilité et invisibilité
            player.setInvulnerable(false);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            // Restaurer l'inventaire du joueur
            List<ItemStack> items = savedItems.get(player.getUniqueId());
            if (items != null) {
                for (ItemStack item : items) {
                    player.getInventory().addItem(item);
                }
                savedItems.remove(player.getUniqueId());
            }
            // Réanimer en mode SURVIVAL
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(ChatColor.GREEN + "Tu as été réanimé !");
            return true;
        }
        return false;
    }

    // Méthodes pour activer/désactiver le système de mort custom
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

}