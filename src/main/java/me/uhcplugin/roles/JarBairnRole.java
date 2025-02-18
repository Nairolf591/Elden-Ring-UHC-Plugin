package me.uhcplugin.roles;

import me.uhcplugin.Main;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JarBairnRole implements Listener {
    private final Main plugin;
    private final Map<UUID, Long> infoCooldowns = new HashMap<>();
    private final Map<UUID, Boolean> inheritedRole = new HashMap<>();

    public JarBairnRole(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startPassiveEffectsTask();
        startInfoTask();
    }

    private void startPassiveEffectsTask() {
        // Effets passifs (Speed 0.5 et Résistance 0.5)
    }

    private void startInfoTask() {
        // Informations sur Alexandre toutes les 20 minutes
    }

    @EventHandler
    public void onAlexandreDeath(PlayerDeathEvent event) {
        Player alexandre = event.getEntity();
        if (!"Alexandre".equalsIgnoreCase(plugin.getRoleManager().getRole(alexandre))) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if ("Jar Bairn".equalsIgnoreCase(plugin.getRoleManager().getRole(player))) {
                player.sendMessage(ChatColor.GOLD + "⚔️ Tu hérites du rôle et des capacités d'Alexandre !");

                // Retire Speed 0.5 et Résistance 0.5
                player.removePotionEffect(PotionEffectType.SPEED);
                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);

                // Applique Résistance 1
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

                // Donne les items d'Alexandre
                player.getInventory().addItem(AlexandreRole.getAlexandreItems());
                inheritedRole.put(player.getUniqueId(), true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Vérifie si le joueur est Jar Bairn et a hérité du rôle d'Alexandre
        if (!"Jar Bairn".equalsIgnoreCase(plugin.getRoleManager().getRole(player)) || !inheritedRole.getOrDefault(player.getUniqueId(), false)) {
            return;
        }

        // Vérifie que l'item utilisé est une Nether Star
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) {
            return;
        }

        // Exécute la compétence correspondante
        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.contains("Onde de Choc du Pot")) {
            handleOndeChoc(player);
        } else if (displayName.contains("Détermination du Guerrier")) {
            handleDetermination(player);
        } else if (displayName.contains("Charge du Pot Légendaire")) {
            handleCharge(player);
        }
    }

    private void handleOndeChoc(Player player) {
        // Implémentation de la compétence Onde de Choc du Pot
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 10);
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getLocation().distance(player.getLocation()) <= 5 && !target.equals(player)) {
                target.damage(4.0, player); // 2 cœurs de dégâts
                target.setVelocity(target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5).setY(0.2));
            }
        }
    }

    private void handleDetermination(Player player) {
        // Implémentation de la compétence Détermination du Guerrier
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 600, 0, false, false)); // +5% de dégâts pendant 30 secondes
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 600, 0, false, false)); // Immunité au knockback
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1.0f, 1.0f);
        player.spawnParticle(Particle.FLAME, player.getLocation(), 50, 1, 1, 1, 0.1);
    }

    private void handleCharge(Player player) {
        // Implémentation de la compétence Charge du Pot Légendaire
        Vector direction = player.getLocation().getDirection().normalize().multiply(1.5);
        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getLocation().distance(player.getLocation()) <= 2 && !target.equals(player)) {
                target.damage(4.0, player); // 2 cœurs de dégâts
                target.setVelocity(direction.multiply(0.5).setY(0.2));
            }
        }
    }
}
