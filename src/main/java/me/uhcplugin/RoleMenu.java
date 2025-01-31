package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

public class RoleMenu implements Listener {

    private final Main plugin;
    private final FileConfiguration config;

    public RoleMenu(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    // Ouvre le menu des rôles
    public void openRoleMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Activation des rôles");

        Set<String> roles = config.getConfigurationSection("roles").getKeys(false);
        int slot = 0;
        for (String role : roles) {
            boolean isEnabled = config.getBoolean("roles." + role);
            ItemStack item = new ItemStack(isEnabled ? Material.LIME_DYE : Material.RED_DYE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((isEnabled ? ChatColor.GREEN : ChatColor.RED) + role);
                item.setItemMeta(meta);
            }
            menu.setItem(slot, item);
            slot++;
        }

        // Ajout d'un bouton retour
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GRAY + "Retour");
            backButton.setItemMeta(backMeta);
        }
        menu.setItem(26, backButton);

        player.openInventory(menu);
    }

    // Gestion du clic sur le menu
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.GOLD + "Activation des rôles")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String roleName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (roleName.equals("Retour")) {
            player.closeInventory();
            return;
        }

        boolean newState = !config.getBoolean("roles." + roleName);
        config.set("roles." + roleName, newState);
        plugin.saveConfig();

        // Met à jour l'item dans le menu
        ItemStack newItem = new ItemStack(newState ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = newItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((newState ? ChatColor.GREEN : ChatColor.RED) + roleName);
            newItem.setItemMeta(meta);
        }
        event.getInventory().setItem(event.getSlot(), newItem);

        player.sendMessage("§aLe rôle " + roleName + " est maintenant " + (newState ? "activé" : "désactivé") + " !");
    }

    // Ajoute un bloc spécial qui ouvre le menu des rôles
    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.BOOK) {
                openRoleMenu(event.getPlayer());
            }
        }
    }
}
