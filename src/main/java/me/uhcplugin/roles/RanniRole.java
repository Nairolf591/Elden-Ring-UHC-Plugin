package me.uhcplugin.roles;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import me.uhcplugin.Main;
import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class RanniRole implements Listener {
    private final Main plugin;
    private final HashMap<UUID, UUID> partners = new HashMap<>();

    public RanniRole(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>(); // Sauvegarde des inventaires

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return; // Si ce n'est pas un joueur qui a tué, on ignore.

        String killerRole = plugin.getRoleManager().getRole(killer);
        if (killerRole.equalsIgnoreCase("Ranni") && !partners.containsKey(killer.getUniqueId())) {
            // 🔹 Sauvegarde l'inventaire du joueur AVANT sa mort
            savedInventories.put(victim.getUniqueId(), victim.getInventory().getContents().clone());

            // 🔹 Empêche le drop du stuff du joueur
            event.getDrops().clear();

            // 🔹 Stocke la victime comme partenaire
            partners.put(killer.getUniqueId(), victim.getUniqueId());
            plugin.getConfig().set("partners." + killer.getUniqueId().toString(), victim.getUniqueId().toString());
            plugin.saveConfig();

            // 🔹 Ressuscite le joueur immédiatement
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                victim.spigot().respawn();
                victim.teleport(killer.getLocation()); // TP au killer
                victim.sendMessage(ChatColor.LIGHT_PURPLE + "🌙 Une étrange puissance lunaire t’enveloppe...");
                victim.sendMessage(ChatColor.AQUA + "✨ Ranni t’a lié à son destin, vous êtes désormais unis !");
                victim.sendMessage(ChatColor.GOLD + "⚔ Ton objectif : Remporter la victoire à ses côtés !");

                // 🔹 Restaure son inventaire après la résurrection
                if (savedInventories.containsKey(victim.getUniqueId())) {
                    victim.getInventory().setContents(savedInventories.get(victim.getUniqueId()));
                    savedInventories.remove(victim.getUniqueId()); // Nettoie après la restauration
                }

                // 🔹 Préserve le rôle du joueur ressuscité
                String victimRole = plugin.getRoleManager().getRole(victim);
                plugin.getRoleManager().setRole(victim, victimRole);
            }, 20L); // 20 ticks après pour éviter les conflits
        }
    }


    public boolean hasPartner(Player ranni) {
        return partners.containsKey(ranni.getUniqueId());
    }

    public Player getPartner(Player ranni) {
        UUID partnerUUID = partners.get(ranni.getUniqueId());
        return partnerUUID != null ? Bukkit.getPlayer(partnerUUID) : null;
    }
}