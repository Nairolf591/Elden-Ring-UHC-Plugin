package me.uhcplugin.role;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.uhcplugin.Main;
import me.uhcplugin.RoleManager;

public class Margit implements Listener {

    private final Main plugin;
    private final RoleManager roleManager;
    private final Material specialItem = Material.BLAZE_ROD; // Bâton de Blaze comme item spécial
    private final long cooldown = 180 * 20; // 3 minutes en ticks (20 ticks = 1 seconde)

    public Margit(Main plugin, RoleManager roleManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Applique l'effet de force accrue à Margit
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (roleManager.getRole(player).equals("Margit")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, true, false));
            giveSpecialItem(player);
        }
    }

    // Donne l'item spécial à Margit 10 secondes après le début de la partie
    private void giveSpecialItem(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (roleManager.getRole(player).equals("Margit")) {
                    ItemStack item = new ItemStack(specialItem);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GOLD + "Marteau de Margit");
                        item.setItemMeta(meta);
                    }
                    player.getInventory().addItem(item);
                }
            }
        }.runTaskLater(plugin, 200L); // 10 secondes (200 ticks)
    }

    // Gestion de l'activation de la compétence avec l'item spécial
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == specialItem && roleManager.getRole(player).equals("Margit")) {
            if (player.hasCooldown(specialItem)) {
                player.sendMessage(ChatColor.RED + "Compétence en cooldown !");
                return;
            }

            // Active la compétence
            activateMargitHammer(player);
            player.setCooldown(specialItem, (int) cooldown);
        }
    }

    // Activation de la compétence "Marteau de Margit"
    private void activateMargitHammer(Player player) {
        // Effets visuels et sonores pendant le saut
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 1, 1, 1, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);

        // Saut puissant dans la direction du regard
        Vector direction = player.getLocation().getDirection().multiply(2).setY(1.5);
        player.setVelocity(direction);

        // Effets à la retombée
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) {
                    player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 10, 2, 2, 2, 0.1);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

                    // Inflige des dégâts et repousse les entités dans un rayon de 5 blocs
                    for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                        if (entity instanceof Player) {
                            Player target = (Player) entity;
                            target.damage(6, player); // 3 cœurs de dégâts
                            Vector knockback = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
                            target.setVelocity(knockback);
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}