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
    public void onBlockClick(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.BOOK && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.GOLD + "📜 Gérer les Rôles")) {
                    openRoleMenu(event.getPlayer()); // ✅ Ouvre le menu seulement si le livre a le bon nom
                }
            }
        }
    }
   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;

    Player player = (Player) event.getWhoClicked();
    ItemStack clickedItem = event.getCurrentItem();
    String inventoryTitle = event.getView().getTitle();

    // Vérifie si le clic est dans le menu des rôles
    if (inventoryTitle.equals(ChatColor.GOLD + "Activation des rôles")) {
        event.setCancelled(true); // Annule l'interaction pour ce menu

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        // Gestion du bouton "Retour"
        if (clickedItem.getType() == Material.ARROW) {
                Main.getInstance().openConfigMenu(player);  // Correct si Main a une instance statique

            return;
        }

        // Gestion des rôles
        String roleName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        boolean isEnabled = config.getBoolean("roles." + roleName);

        // Inverse l'état du rôle
        config.set("roles." + roleName, !isEnabled);
        plugin.saveConfig();

        // Met à jour le menu
        openRoleMenu(player);
        player.sendMessage(ChatColor.GREEN + "Rôle " + roleName + " " + (isEnabled ? "désactivé" : "activé") + " !");

        // 🔄 Met à jour le scoreboard pour tous les joueurs
        plugin.getScoreboardManager().updateAllScoreboards();
    }
 }
}