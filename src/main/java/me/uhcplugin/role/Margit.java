package me.uhcplugin.role;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
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
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 180 * 1000; // 3 minutes en millisecondes
    private static final Material HAMMER_ITEM = Material.GOLDEN_AXE;

    public Margit(Player player) {
        this.player = player;
        applyConstantEffects();
        giveHammerItem();
    }

    private void applyConstantEffects() {
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INCREASE_DAMAGE, 
            Integer.MAX_VALUE, 
            0, 
            false, 
            false
        ));
    }

    private void giveHammerItem() {
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UHCPlugin"), () -> {
            ItemStack hammer = new ItemStack(HAMMER_ITEM);
            ItemMeta meta = hammer.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Marteau de Margit");
                meta.addEnchant(Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Enchantement visible mais texte caché
                hammer.setItemMeta(meta);
            }
            
            player.getInventory().addItem(hammer);
            player.sendMessage(ChatColor.GOLD + "§l➤ Votre marteau est prêt ! Clic pour sauter !");
        }, 200L);
    }

    @EventHandler
    public void onHammerUse(PlayerInteractEvent event) {
        Player user = event.getPlayer();
        if (!user.getUniqueId().equals(player.getUniqueId())) return;

        ItemStack item = event.getItem();
        if (item == null || 
            item.getType() != HAMMER_ITEM || 
            !item.hasItemMeta() || 
            !item.getItemMeta().hasDisplayName() || 
            !item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Marteau de Margit")) {
            return;
        }

        // Détection de tous types de clics
        if (event.getAction() == Action.LEFT_CLICK_AIR || 
            event.getAction() == Action.LEFT_CLICK_BLOCK ||
            event.getAction() == Action.RIGHT_CLICK_AIR || 
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            handleSkillActivation(user);
            event.setCancelled(true); // Bloque l'action normale
        }
    }

    private void handleSkillActivation(Player user) {
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(user.getUniqueId(), 0L);

        if (currentTime - lastUsed < COOLDOWN_TIME) {
            long remaining = (COOLDOWN_TIME - (currentTime - lastUsed)) / 1000;
            user.sendMessage(ChatColor.RED + "§l⌛ Recharge : " + remaining + "s");
            return;
        }

        cooldowns.put(user.getUniqueId(), currentTime);
        performHammerJump(user);
    }

    private void performHammerJump(Player user) {
        // Boost directionnel
        Vector boost = user.getLocation().getDirection()
            .multiply(2.0)
            .setY(1.2);
        
        user.setVelocity(boost);
        
        // Effets sonores/visuels
        user.getWorld().playSound(user.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        user.getWorld().spawnParticle(Particle.SMOKE_LARGE, user.getLocation(), 15, 0.5, 0.5, 0.5, 0.2);

        // Retombée après 1 seconde
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UHCPlugin"), () -> {
            hammerImpact(user.getLocation());
        }, 20);
    }

    private void hammerImpact(Location impactPoint) {
        // Explosion visuelle
        impactPoint.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, impactPoint, 1);
        impactPoint.getWorld().playSound(impactPoint, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

        // Dégâts zone + knockback
        for (Entity entity : impactPoint.getWorld().getNearbyEntities(impactPoint, 5, 3, 5)) {
            if (entity.equals(player)) continue;
            
            Vector knockback = entity.getLocation()
                .toVector()
                .subtract(impactPoint.toVector())
                .normalize()
                .multiply(1.5)
                .setY(0.4);
            
            entity.setVelocity(knockback);
            
            if (entity instanceof Player) {
                ((Player) entity).damage(6.0); // 3 cœurs
                ((Player) entity).sendMessage(ChatColor.RED + "§l☠ Frappé par Margit !");
            }
        }
    }
}