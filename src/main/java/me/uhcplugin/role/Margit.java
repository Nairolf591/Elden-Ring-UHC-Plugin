package me.uhcplugin.role;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerInteractEvent.Action;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Margit implements Listener {

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 5000L; // 5 seconds cooldown in milliseconds

    // Event listener to detect right-click with the hammer in hand
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if the player right-clicks with the hammer (in this case a GOLDEN_AXE)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item.getType() == Material.GOLDEN_AXE && item.hasItemMeta()) {
                activateSkill(player);
            }
        }
    }

    // Method to activate the skill when the hammer is clicked
    public static void activateSkill(Player player) {
        if (cooldowns.containsKey(player.getUniqueId()) && 
            System.currentTimeMillis() - cooldowns.get(player.getUniqueId()) < COOLDOWN_TIME) {
            player.sendMessage(ChatColor.RED + "You must wait before using the skill again!");
            return;
        }

        // Get the direction the player is facing
        Vector direction = player.getLocation().getDirection().normalize().multiply(10); // 10 blocks forward

        // Calculate new location 10 blocks ahead in the direction the player is facing
        player.setVelocity(new Vector(0, 1, 0)); // Propel the player upwards slightly for the jump
        player.teleport(player.getLocation().add(direction)); // Teleport the player 10 blocks ahead

        // Apply the damage to nearby entities
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) { // 5 block radius
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity livingEntity = (LivingEntity) entity;
                livingEntity.damage(6); // Deal 3 hearts of damage (6 damage points)
            }
        }

        // Play sound and particle effects to signal skill activation
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, player.getLocation(), 1);

        // Set cooldown for the player
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Message to the player
        player.sendMessage(ChatColor.GREEN + "Hammer skill activated! You jumped forward and dealt damage!");
    }
}
