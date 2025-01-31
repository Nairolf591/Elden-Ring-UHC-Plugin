package me.uhcplugin.roles;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import me.uhcplugin.Main;

public class Godfrey implements Listener {

    private final Main plugin;
    private final long cooldownTime = 300; // 5 minutes en secondes
    private boolean isOnCooldown = false;

    public Godfrey(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        givePermanentEffects();
    }

    // Applique la force 10% à l'infini
    private void givePermanentEffects() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.getRoleManager().getRole(player).equals("Godfrey")) {
                        player.addPotionEffect(new PotionEffect(
                            PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, true, false
                        ));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // Vérifie toutes les 5 secondes
    }

    // Gestion de la compétence "Tremblement de terre"
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Vérifie si le joueur est Godfrey
        if (!plugin.getRoleManager().getRole(player).equals("Godfrey")) return;

        // Vérifie si c'est un clic droit avec un bloc dans la main
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
            player.getInventory().getItemInMainHand().getType().isBlock()) {

            // Vérifie si la compétence est en cooldown
            if (isOnCooldown) {
                player.sendMessage(ChatColor.RED + "Compétence en cooldown ! Attends encore " + cooldownTime + " secondes.");
                return;
            }

            // Déclenche le tremblement de terre
            triggerEarthquake(player);
            startCooldown();
        }
    }

    // Déclenche le tremblement de terre
    private void triggerEarthquake(Player player) {
        Location location = player.getLocation();

        // Sons et particules
        player.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 100, 5, 0.5, 5);

        // Applique des dégâts aux joueurs dans un rayon de 5 blocs
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Player) {
                Player nearbyPlayer = (Player) entity;
                nearbyPlayer.damage(6.0); // 3 cœurs de dégâts
                nearbyPlayer.sendMessage(ChatColor.RED + "Le sol tremble sous vos pieds !");
            }
        }

        player.sendMessage(ChatColor.GOLD + "Vous avez déclenché un tremblement de terre !");
    }

    // Gestion du cooldown
    private void startCooldown() {
        isOnCooldown = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                isOnCooldown = false;
                Bukkit.broadcastMessage(ChatColor.GREEN + "La compétence de Godfrey est à nouveau disponible !");
            }
        }.runTaskLater(plugin, cooldownTime * 20L); // Convertit les secondes en ticks
    }
}
