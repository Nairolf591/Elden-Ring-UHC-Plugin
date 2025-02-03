package me.uhcplugin;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.List;

public class UHCManager {

    private final JavaPlugin plugin;

    public UHCManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ✅ Désactive la régénération naturelle
    public void disableNaturalRegen() {
        Bukkit.getWorlds().forEach(world -> world.setGameRule(GameRule.NATURAL_REGENERATION, false));
        Bukkit.broadcastMessage(ChatColor.RED + "🚫 Régénération naturelle désactivée !");
    }

    // ✅ Active/Désactive le PvP
    public void setPvPEnabled(boolean enabled) {
        Bukkit.getWorlds().forEach(world -> world.setPVP(enabled));
    }

    // ✅ Gère la bordure
    public void setBorderSize(int size) {
        World uhcWorld = Bukkit.getWorld("uhc");
        if (uhcWorld != null) {
            uhcWorld.getWorldBorder().setSize(size);
            Bukkit.broadcastMessage(ChatColor.GREEN + "🌍 Bordure ajustée à " + size + " blocs.");
        }
    }

    // ✅ Nettoie les items au sol
    public void clearDroppedItems() {
        Bukkit.getWorlds().forEach(world ->
                world.getEntities().stream()
                        .filter(entity -> entity instanceof org.bukkit.entity.Item)
                        .forEach(entity -> entity.remove())
        );
        Bukkit.broadcastMessage(ChatColor.YELLOW + "🧹 Tous les items au sol ont été supprimés !");
    }

    // ✅ Gère le timer d'invincibilité
    public void startInvincibility(int duration) {
        Bukkit.broadcastMessage(ChatColor.GOLD + "🛡 Invincibilité activée pour " + duration + " secondes !");

        // Applique l'invincibilité
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setInvulnerable(true);
        }

        // Désactive l'invincibilité après le délai
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setInvulnerable(false);
                }
                Bukkit.broadcastMessage(ChatColor.RED + "⚠ Fin de l'invincibilité !");
            }
        }.runTaskLater(plugin, duration * 20L);
    }

    public void startPvPTimer(int minutes) {
        Bukkit.broadcastMessage(ChatColor.RED + "⚔️ Le PvP s'activera dans " + minutes + " minutes !");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            setPvPEnabled(true);
            Bukkit.broadcastMessage(ChatColor.RED + "⚔️ Le PvP est maintenant activé !");
        }, minutes * 60 * 20);
        // Convertit minutes en ticks
    }

}
