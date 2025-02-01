package me.uhcplugin.role;

import org.bukkit.*;
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
    private static final long COOLDOWN_TIME = 180 * 1000;
    private static final Material ABILITY_ITEM = Material.BLAZE_ROD; // Nouvel item : bâton de Blaze

    public Margit(Player player) {
        this.player = player;
        applyConstantEffects();
        giveAbilityItem();
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

    private void giveAbilityItem() {
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UHCPlugin"), () -> {
            ItemStack abilityItem = new ItemStack(ABILITY_ITEM);
            ItemMeta meta = abilityItem.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Marteau Spectral");
                meta.addEnchant(Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                abilityItem.setItemMeta(meta);
            }
            
            player.getInventory().addItem(abilityItem);
            player.sendMessage(ChatColor.GOLD + "§l➤ Compétence prête ! Clic avec le bâton doré !");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f); // Son de confirmation
        }, 200L);
    }

    @EventHandler
    public void onAbilityUse(PlayerInteractEvent event) {
        if (!event.getPlayer().equals(player)) return;

        ItemStack item = event.getItem();
        if (isValidAbilityItem(item)) {
            handleInteraction(event);
        }
    }

    private boolean isValidAbilityItem(ItemStack item) {
        return item != null 
            && item.getType() == ABILITY_ITEM
            && item.hasItemMeta()
            && item.getItemMeta().hasDisplayName()
            && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Marteau Spectral");
    }

    private void handleInteraction(PlayerInteractEvent event) {
        Action action = event.getAction();
        
        // Détection clic droit/gauche (air ou bloc)
        if (action == Action.RIGHT_CLICK_AIR || 
            action == Action.RIGHT_CLICK_BLOCK ||
            action == Action.LEFT_CLICK_AIR || 
            action == Action.LEFT_CLICK_BLOCK) {
            
            event.setCancelled(true); // Bloque toute interaction normale
            checkCooldownAndActivate();
        }
    }

    private void checkCooldownAndActivate() {
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (currentTime - lastUsed < COOLDOWN_TIME) {
            sendCooldownMessage(currentTime - lastUsed);
            return;
        }

        activateAbility();
        cooldowns.put(player.getUniqueId(), currentTime);
    }

    private void sendCooldownMessage(long elapsed) {
        long remaining = (COOLDOWN_TIME - elapsed) / 1000;
        player.sendMessage(ChatColor.RED + "§l⏳ " + remaining + " secondes avant réutilisation !");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }

    private void activateAbility() {
        // Boost directionnel + effets
        Vector boost = player.getLocation().getDirection()
            .multiply(2.2)
            .setY(1.3);
        
        player.setVelocity(boost);
        playActivationEffects();
        
        // Programmation de l'impact
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UHCPlugin"), 
            this::executeImpact, 
            20 // 1 seconde = 20 ticks
        );
    }

    private void playActivationEffects() {
        Location loc = player.getLocation();
        
        // Sons et particules
        player.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.7f);
        player.spawnParticle(Particle.FLAME, loc, 30, 0.3, 0.3, 0.3, 0.05);
        
        // Feedback visuel sur l'item
        player.sendTitle("", ChatColor.YELLOW + "⚡ Compétence activée !", 5, 15, 5);
    }

    private void executeImpact() {
        Location impactPoint = player.getLocation();
        
        // Explosion magique
        impactPoint.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, impactPoint, 100, 2, 1, 2, 0.3);
        impactPoint.getWorld().playSound(impactPoint, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);

        // Affectation des entités
        impactPoint.getWorld().getNearbyEntities(impactPoint, 5, 3, 5).forEach(entity -> {
            if (entity.equals(player)) return;
            
            applyKnockback(entity, impactPoint);
            if (entity instanceof Player) {
                ((Player) entity).damage(6.0);
                ((Player) entity).sendMessage(ChatColor.RED + "§l☠ Frappé par le Marteau Spectral !");
            }
        });
    }

    private void applyKnockback(Entity entity, Location source) {
        Vector direction = entity.getLocation().toVector()
            .subtract(source.toVector())
            .normalize()
            .multiply(1.8)
            .setY(0.6);
        
        entity.setVelocity(direction);
    }
}