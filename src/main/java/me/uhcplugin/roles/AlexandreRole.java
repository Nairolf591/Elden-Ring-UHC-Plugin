// Fichier: src/main/java/me/uhcplugin/roles/AlexandreRole.java
package me.uhcplugin.roles;

import me.uhcplugin.Main;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AlexandreRole implements Listener {
    private final Main plugin;
    private final Map<UUID, Long> ondeCooldown = new HashMap<>();
    private final Map<UUID, Long> determinationCooldown = new HashMap<>();
    private final Map<UUID, Long> chargeCooldown = new HashMap<>();
    private final Map<UUID, Boolean> determinationActive = new HashMap<>();
    private final Map<UUID, Long> messageCooldowns = new HashMap<>();

    public AlexandreRole(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static ItemStack[] getAlexandreItems() {
        // Création des items
        ItemStack ondeItem = createAbilityItem("1️⃣ Onde de Choc du Pot", "💥", Arrays.asList(
                ChatColor.GRAY + "Dégâts: 2 cœurs dans 5 blocs",
                ChatColor.RED + "Repoussement: 8 blocs",
                ChatColor.GOLD + "Recharge: 1 jour"
        ));

        ItemStack determinationItem = createAbilityItem("2️⃣ Détermination du Guerrier", "⚔️", Arrays.asList(
                ChatColor.GRAY + "+5% Force pendant 30s",
                ChatColor.BLUE + "Immunité au Knockback",
                ChatColor.GOLD + "Recharge: 4 minutes"
        ));

        ItemStack chargeItem = createAbilityItem("3️⃣ Charge du Pot Légendaire", "🏺", Arrays.asList(
                ChatColor.GRAY + "Charge sur 15 blocs",
                ChatColor.RED + "Dégâts: 2 cœurs",
                ChatColor.GOLD + "Recharge: 1 jour"
        ));

        return new ItemStack[]{ondeItem, determinationItem, chargeItem};
    }

    // Méthode utilitaire pour créer les items
    private static ItemStack createAbilityItem(String name, String emoji, List<String> lore) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + emoji + " " + name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }



    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!"Alexandre".equalsIgnoreCase(plugin.getRoleManager().getRole(player))) return;

        if (item.getType() == Material.NETHER_STAR && item.hasItemMeta()) {
            String displayName = item.getItemMeta().getDisplayName();

            if (displayName.contains("Onde de Choc")) {
                handleOndeChoc(player);
            } else if (displayName.contains("Détermination")) {
                handleDetermination(player);
            } else if (displayName.contains("Charge")) {
                handleCharge(player);
            }
        }
    }

    private void handleOndeChoc(Player player) {
        UUID uuid = player.getUniqueId();

        if (checkCooldown(uuid, ondeCooldown, 1200, "Onde de Choc")) return;

        // Effets sonores et visuels
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.8f);
        player.spawnParticle(Particle.EXPLOSION_HUGE, player.getLocation(), 5);

        // Appliquer les effets
        for (Entity entity : player.getWorld().getEntities()) {
            if (entity.getLocation().distance(player.getLocation()) <= 5 && !entity.equals(player)) {
                if (entity instanceof LivingEntity) { // Vérifie si c'est un LivingEntity
                    LivingEntity livingEntity = (LivingEntity) entity;
                    livingEntity.damage(4.0, player);
                    Vector direction = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                    entity.setVelocity(direction.multiply(2.5).setY(0.8));
                }
            }
        }

        startCooldown(uuid, ondeCooldown, 1200); // 20 minutes
    }

    private void handleDetermination(Player player) {
        UUID uuid = player.getUniqueId();

        if (checkCooldown(uuid, determinationCooldown, 240, "Détermination")) return;

        // Activer les buffs
        determinationActive.put(uuid, true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 600, 0));

        // Effets
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.5f, 0.5f);
        player.spawnParticle(Particle.FLAME, player.getLocation(), 100, 1, 1, 1, 0.1);

        new BukkitRunnable() {
            public void run() {
                determinationActive.remove(uuid);
                player.sendMessage(ChatColor.GOLD + "⚔️ La détermination s'estompe...");
            }
        }.runTaskLater(plugin, 600);

        startCooldown(uuid, determinationCooldown, 240); // 4 minutes
    }

    private void handleCharge(Player player) {
        UUID uuid = player.getUniqueId();

        if (checkCooldown(uuid, chargeCooldown, 1200, "Charge")) return;

        // Préparation de la charge
        Vector direction = player.getLocation().getDirection().normalize().multiply(1.5);
        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 2.0f, 0.5f);

        new BukkitRunnable() {
            int ticks = 0;

            public void run() {
                if (ticks++ >= 60 || player.isOnGround()) { // 3 secondes
                    cancel();
                    return;
                }

                // Effets pendant la charge
                player.spawnParticle(Particle.CLOUD, player.getLocation(), 10);
                player.spawnParticle(Particle.FLAME, player.getLocation(), 5);

                // Dégâts aux entités proches
                for (Entity entity : player.getNearbyEntities(2, 2, 2)) {
                    if (!entity.equals(player) && entity instanceof LivingEntity) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        livingEntity.damage(4.0, player);
                        if (entity instanceof Player) {
                            ((Player) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 0));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        startCooldown(uuid, chargeCooldown, 1200); // 20 minutes
    }


    @EventHandler
    public void onKnockback(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (determinationActive.containsKey(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean checkCooldown(UUID uuid, Map<UUID, Long> cooldownMap, long cooldownSeconds, String abilityName) {
        if (cooldownMap.containsKey(uuid)) {
            long remaining = (cooldownMap.get(uuid) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                if (!messageCooldowns.containsKey(uuid) || System.currentTimeMillis() - messageCooldowns.get(uuid) > 2000) {
                    plugin.getServer().getPlayer(uuid).sendMessage(ChatColor.RED + "⏳ " + abilityName + " : " + remaining + "s restantes");
                    messageCooldowns.put(uuid, System.currentTimeMillis());
                }
                return true;
            }
        }
        return false;
    }

    private void startCooldown(UUID uuid, Map<UUID, Long> cooldownMap, long cooldownSeconds) {
        cooldownMap.put(uuid, System.currentTimeMillis() + (cooldownSeconds * 1000));
    }
}