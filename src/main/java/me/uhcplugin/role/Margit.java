package me.uhcplugin.role;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class Margit implements Listener {
    private final Player player;

    public Margit(Player player) {
        this.player = player;
        giveTestItem();
    }

    private void giveTestItem() {
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UHCPlugin"), () -> {
            ItemStack item = new ItemStack(Material.STICK);
            item.setItemMeta(Bukkit.getItemFactory().getItemMeta(Material.STICK));
            item.getItemMeta().setDisplayName(ChatColor.GOLD + "TEST");
            player.getInventory().addItem(item);
            player.sendMessage("§aItem donné !");
        }, 20L); // 1 seconde pour tester rapidement
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;
        
        Bukkit.broadcastMessage("§eÉvénement déclenché pour: " + item.getType() + " | Nom: " + item.getItemMeta().getDisplayName());

        if (item.getType() == Material.STICK && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("TEST")) {
            p.sendMessage("§2SUCCÈS ! Interaction détectée !");
            event.setCancelled(true);
        }
    }
}