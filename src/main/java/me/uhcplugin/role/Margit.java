package me.uhcplugin.role;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Margit implements Listener {
    private final Player player;
    private static final Material HAMMER_ITEM = Material.GOLDEN_AXE; // Marteau en or
    private static final long COOLDOWN_TIME = 180 * 1000; // 3 minutes
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private boolean hammerInHand = false; // Indicateur pour vérifier si le marteau est dans la main

    public Margit(Player player) {
        this.player = player;
        applyConstantEffects();
        giveHammerItem();
    }

    // Applique un effet constant (force) au joueur
    private void applyConstantEffects() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, false, false));
    }

    // Donne le marteau au joueur après 10 secondes
    private void giveHammerItem() {
        Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("UHCPlugin"), () -> {
            ItemStack hammer = new ItemStack(HAMMER_ITEM);
            ItemMeta meta = hammer.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Marteau de Margit");
                hammer.setItemMeta(meta);
            }
            player.getInventory().addItem(hammer);
            player.sendMessage(ChatColor.GOLD + "Vous avez reçu le Marteau de Margit !");
        }, 200L);
    }

    // Activation de la compétence après 5 secondes de maintien du marteau en main
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player p = event.getPlayer();
        
        // Vérifie que c'est bien le joueur Margit et que c'est le marteau
        if (!p.equals(player)) return;
        if (p.getInventory().getItemInMainHand() == null || 
            p.getInventory().getItemInMainHand().getType() != HAMMER_ITEM) {
            hammerInHand = false;
            return;
        }

        if (!hammerInHand) {
            hammerInHand = true;
            startHammerHoldTimer(p);
        }
    }

    // Lance le timer de 5 secondes pour l'activation
    private void startHammerHoldTimer(Player player) {
        Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("UHCPlugin"), () -> {
            if (player.getInventory().getItemInMainHand() != null &&
                player.getInventory().getItemInMainHand().getType() == HAMMER_ITEM) {
                // Vérifie le cooldown
                long currentTime = System.currentTimeMillis();
                long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);
                if (currentTime - lastUsed < COOLDOWN_TIME) {
                    long remainingTime = (COOLDOWN_TIME - (currentTime - lastUsed)) / 1000;
                    player.sendMessage(ChatColor.RED + "Vous devez attendre " + remainingTime + " secondes avant d'utiliser cette compétence.");
                    hammerInHand = false;
                    return;
                }

                // Si le marteau est encore en main et que le cooldown est passé, active la compétence
                performHammerJump(player);
                cooldowns.put(player.getUniqueId(), currentTime); // Met à jour le cooldown
                hammerInHand = false; // Réinitialise l'état
            }
        }, 100L); // Délai de 5 secondes (100 ticks = 5 secondes)
    }

    // Exécute le saut avec le marteau
    private void performHammerJump(Player player) {
        Vector direction = player.getLocation().getDirection().multiply(2).setY(1.5);
        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);
        Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("UHCPlugin"), () -> hammerLanding(player), 20);
    }

    // Gestion de l'atterrissage du saut
    private void hammerLanding(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 10, 2, 0.5, 2, 0.1);
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 5, 3, 5)) {
            if (!entity.equals(player)) {
                entity.setVelocity(entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5));
                if (entity instanceof Player) {
                    ((Player) entity).damage(6.0);
                }
            }
        }
    }
}
