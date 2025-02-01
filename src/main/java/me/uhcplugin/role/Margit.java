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
    private static final Material HAMMER_ITEM = Material.GOLDEN_AXE;

    public Margit(Player player) {
        this.player = player;
        applyConstantEffects();
        giveHammerItem();
    }

    private void applyConstantEffects() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, false, false));
    }

    private void giveHammerItem() {
        Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("UHCPlugin"), () -> {
            ItemStack hammer = new ItemStack(HAMMER_ITEM);
            ItemMeta meta = hammer.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Marteau de Margit");
                meta.addEnchant(Enchantment.DURABILITY, 1, false); // Enchantement visuel
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Cache l'effet si besoin
                hammer.setItemMeta(meta);
            }
            player.getInventory().addItem(hammer);
            player.sendMessage(ChatColor.GOLD + "Vous avez reçu le Marteau de Margit ! Utilisez-le avec un clic gauche.");
        }, 200L);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (event.getItem() == null || event.getItem().getType() != HAMMER_ITEM) return;
        if (!event.getAction().toString().contains("LEFT_CLICK")) return; // Détection du clic gauche

        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (currentTime - lastUsed < COOLDOWN_TIME) {
            long remainingTime = (COOLDOWN_TIME - (currentTime - lastUsed)) / 1000;
            player.sendMessage(ChatColor.RED + "Vous devez attendre " + remainingTime + " secondes avant de réutiliser cette compétence.");
            return;
        }

        performHammerJump();
        cooldowns.put(player.getUniqueId(), currentTime);
    }

    private void performHammerJump() {
        Vector direction = player.getLocation().getDirection().multiply(2).setY(1.5);
        player.setVelocity(direction);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

        Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("UHCPlugin"), this::hammerLanding, 20);
    }

    private void hammerLanding() {
        Location location = player.getLocation();

        player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, location, 10, 2, 0.5, 2, 0.1);

        for (Entity entity : player.getWorld().getNearbyEntities(location, 5, 3, 5)) {
            if (!entity.equals(player)) {
                entity.setVelocity(entity.getLocation().toVector().subtract(location.toVector()).normalize().multiply(1.5));
                if (entity instanceof Player) {
                    ((Player) entity).damage(6.0);
                }
            }
        }
    }
}
