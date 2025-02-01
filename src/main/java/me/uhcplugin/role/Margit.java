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
import org.bukkit.event.player.PlayerToggleSneakEvent;
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
    private static final long COOLDOWN_TIME = 180 * 1000; // 3 minutes en ms
    private static final Material HAMMER_ITEM = Material.GOLDEN_AXE; // Marteau = hache en or

    public Margit(Player player) {
        this.player = player;
        applyConstantEffects();
        giveHammerItem();
    }

    // Applique Force 1 en permanence
    private void applyConstantEffects() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, false, false));
    }

    // Donne l'item après 10 secondes avec un enchantement pour le rendre visible
    private void giveHammerItem() {
        Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("UHCPlugin"), () -> {
            ItemStack hammer = new ItemStack(HAMMER_ITEM);
            ItemMeta meta = hammer.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Marteau de Margit");
                meta.addEnchant(Enchantment.KNOCKBACK, 1, true); // Enchantement pour le rendre brillant
                hammer.setItemMeta(meta);
            }
            player.getInventory().addItem(hammer);
            player.sendMessage(ChatColor.GOLD + "Vous avez reçu le Marteau de Margit !");
        }, 200L); // 10 secondes = 200 ticks
    }

    // Détection du CLIC GAUCHE et CLIC DROIT avec le marteau
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!p.equals(player)) return;

        if (event.getItem() == null || event.getItem().getType() != HAMMER_ITEM) return;

        if (event.getAction().toString().contains("LEFT_CLICK") || event.getAction().toString().contains("RIGHT_CLICK")) {
            p.sendMessage(ChatColor.YELLOW + "Détection du clic !");
            activateSkill(p);
        }
    }

    // Détection du SNEAK avec le marteau
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player p = event.getPlayer();
        if (!p.equals(player)) return;

        if (!p.isSneaking()) return; // Vérifie qu'il s'accroupit

        if (p.getInventory().getItemInMainHand().getType() != HAMMER_ITEM) return;

        p.sendMessage(ChatColor.YELLOW + "Détection du sneak !");
        activateSkill(p);
    }

    // Vérifie le cooldown et active la compétence
    private void activateSkill(Player p) {
        long currentTime = System.currentTimeMillis();
        long lastUsed = cooldowns.getOrDefault(p.getUniqueId(), 0L);

        if (currentTime - lastUsed < COOLDOWN_TIME) {
            long remainingTime = (COOLDOWN_TIME - (currentTime - lastUsed)) / 1000;
            p.sendMessage(ChatColor.RED + "Cooldown actif : " + remainingTime + " secondes restantes.");
            return;
        }

        p.sendMessage(ChatColor.GREEN + "Compétence activée !");
        performHammerJump();
        cooldowns.put(p.getUniqueId(), currentTime);
    }

    // Effectue le saut du marteau
    private void performHammerJump() {
        Vector direction = player.getLocation().getDirection().multiply(2).setY(1.5);
        player.setVelocity(direction);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

        // Effet de retombée après 1 seconde
        Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("UHCPlugin"), this::hammerLanding, 20);
    }

    // Effet d'impact du marteau
    private void hammerLanding() {
        Location location = player.getLocation();

        player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, location, 10, 2, 0.5, 2, 0.1);

        for (Entity entity : player.getWorld().getNearbyEntities(location, 5, 3, 5)) {
            if (!entity.equals(player)) {
                entity.setVelocity(entity.getLocation().toVector().subtract(location.toVector()).normalize().multiply(1.5));
                if (entity instanceof Player) {
                    ((Player) entity).damage(6.0); // 3 cœurs de dégâts
                }
            }
        }
    }
}
