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
    private static final long COOLDOWN_TIME = 180 * 1000;
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
                meta.addEnchant(Enchantment.LUCK, 1, true); // Enchantement visible
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                hammer.setItemMeta(meta);
            }
            
            player.getInventory().addItem(hammer);
            player.sendMessage(ChatColor.GOLD + "§lMARTEAU DISPONIBLE ! Clic droit pour sauter !");
        }, 200L);
    }

    // Détection simplifiée pour Bedrock/Java
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().equals(player)) return;
        
        ItemStack item = event.getItem();
        if (item == null || 
            item.getType() != HAMMER_ITEM || 
            !item.hasItemMeta() || 
            !item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Marteau de Margit")) {
            return;
        }

        // Détection clic droit uniquement (même sur Bedrock)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            handleSkillActivation();
            event.setCancelled(true); // Empêche l'interaction avec les blocs
        }
    }

    private void handleSkillActivation() {
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (currentTime - lastUsed < COOLDOWN_TIME) {
            long remaining = (COOLDOWN_TIME - (currentTime - lastUsed)) / 1000;
            player.sendMessage(ChatColor.RED + "§lRecharge: " + remaining + "s");
            return;
        }

        cooldowns.put(player.getUniqueId(), currentTime);
        performHammerJump();
    }

    private void performHammerJump() {
        // Boost vertical + horizontal
        Vector boost = player.getLocation().getDirection()
            .multiply(2.0)
            .setY(1.2);
        
        player.setVelocity(boost);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        
        // Effets visuels pendant le saut
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UHCPlugin"), () -> {
            player.getWorld().spawnParticle(
                Particle.FLAME, 
                player.getLocation(), 
                30, 
                0.5, 
                0.5, 
                0.5, 
                0.1
            );
        }, 5L);

        // Retombée après 1 seconde
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UHCPlugin"), this::hammerImpact, 20);
    }

    private void hammerImpact() {
        Location impactPoint = player.getLocation();
        
        // Effets sonores/visuels
        player.getWorld().playSound(impactPoint, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.7f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, impactPoint, 1);

        // Dégâts zone + knockback
        for (Entity entity : player.getWorld().getNearbyEntities(impactPoint, 5, 3, 5)) {
            if (entity.equals(player)) continue;
            
            Vector knockback = entity.getLocation()
                .toVector()
                .subtract(impactPoint.toVector())
                .normalize()
                .multiply(1.8)
                .setY(0.6);
            
            entity.setVelocity(knockback);
            
            if (entity instanceof Player) {
                ((Player) entity).damage(6.0);
            }
        }
    }
}