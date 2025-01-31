package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.inventory.InventoryClickEvent;

public class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getLogger().info("[UHCPlugin] Le plugin est activé !");
        GameManager.setGameState(GameManager.GameState.WAITING);
        Bukkit.getLogger().info("[UHCPlugin] État du jeu défini sur WAITING.");
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("[UHCPlugin] Le plugin est désactivé !");
        GameManager.setGameState(GameManager.GameState.ENDED);
        Bukkit.getLogger().info("[UHCPlugin] État du jeu défini sur ENDED.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent exécuter cette commande !");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("jump")) {
            String worldName = getConfig().getString("jump-location.world");
            double x = getConfig().getDouble("jump-location.x");
            double y = getConfig().getDouble("jump-location.y");
            double z = getConfig().getDouble("jump-location.z");
            float yaw = (float) getConfig().getDouble("jump-location.yaw") + 230;
            float pitch = (float) getConfig().getDouble("jump-location.pitch");

            if (worldName != null) {
                player.teleport(new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch));
                player.sendMessage(ChatColor.AQUA + "Tu as été téléporté au Jump !");
            } else {
                player.sendMessage(ChatColor.RED + "Le monde spécifié dans la config n'existe pas !");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            String worldName = getConfig().getString("spawn-location.world");
            double x = getConfig().getDouble("spawn-location.x");
            double y = getConfig().getDouble("spawn-location.y");
            double z = getConfig().getDouble("spawn-location.z");

            if (worldName != null) {
                player.teleport(new Location(Bukkit.getWorld(worldName), x, y, z));
                player.sendMessage(ChatColor.GREEN + "Tu as été téléporté au Spawn !");
            } else {
                player.sendMessage(ChatColor.RED + "Le monde spécifié dans la config n'existe pas !");
            }
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (GameManager.getGameState() == GameManager.GameState.WAITING) {
            player.getInventory().clear();
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta compassMeta = compass.getItemMeta();
            if (compassMeta != null) {
                compassMeta.setDisplayName(ChatColor.GOLD + "Menu UHC");
                compass.setItemMeta(compassMeta);
            }
            player.getInventory().setItem(4, compass);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String inventoryTitle = event.getView().getTitle();

        if (inventoryTitle.equals(ChatColor.GOLD + "Menu UHC")) {
            event.setCancelled(true);
            switch (clickedItem.getType()) {
                case BLUE_CONCRETE:
                    player.performCommand("jump");
                    break;
                case RED_BED:
                    player.performCommand("spawn");
                    break;
                case COMMAND_BLOCK:
                    openConfigMenu(player);
                    break;
            }
        } else if (inventoryTitle.equals(ChatColor.YELLOW + "Configuration UHC")) {
            event.setCancelled(true);
            switch (clickedItem.getType()) {
                case ARROW:
                    openMainMenu(player);
                    break;
            }
        }
    }

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.COMPASS && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Menu UHC")) {
            openMainMenu(player);
        }
    }

    public void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Menu UHC");

        ItemStack jumpItem = createItem(Material.BLUE_CONCRETE, ChatColor.RED + "Jump");
        ItemStack spawnItem = createItem(Material.RED_BED, ChatColor.RED + "Spawn");
        ItemStack configItem = createItem(Material.COMMAND_BLOCK, ChatColor.YELLOW + "Configuration UHC");

        ItemStack blackGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack redGlass = createItem(Material.RED_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                menu.setItem(i, blackGlass); // Bordure en noir
            } else {
                menu.setItem(i, redGlass); // Remplissage en rouge
            }
        }

        menu.setItem(10, jumpItem);
        menu.setItem(16, spawnItem);
        menu.setItem(31, configItem);

        player.openInventory(menu);
    }

    public void openConfigMenu(Player player) {
        Inventory configMenu = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Configuration UHC");

        ItemStack borderSize = createItem(Material.BARRIER, ChatColor.RED + "Taille de la Bordure");
        ItemStack pvpTimer = createItem(Material.DIAMOND_SWORD, ChatColor.RED + "Temps avant PvP");
        ItemStack backButton = createItem(Material.ARROW, ChatColor.GRAY + "Retour");

        configMenu.setItem(0, borderSize);
        configMenu.setItem(1, pvpTimer);
        configMenu.setItem(8, backButton);

        player.openInventory(configMenu);
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
    // src/main/java/me/uhcplugin/Main.java
        Player player = (Player) sender;
        if (!player.hasPermission("uhcplugin.startuhc")) {
            player.sendMessage(ChatColor.RED + "Permission insuffisante !");
            return true;
        }

        GameManager.setGameState(GameManager.GameState.STARTING);
        Bukkit.broadcastMessage(ChatColor.GOLD + "L'UHC démarre dans 10 secondes !");

        // Attribuer les rôles après 10 secondes
        Bukkit.getScheduler().runTaskLater(this, () -> {
            new RoleManager(this).assignRoles();
            GameManager.setGameState(GameManager.GameState.PLAYING);
            Bukkit.broadcastMessage(ChatColor.GOLD + "Les rôles ont été attribués !");
        }, 200L); // 200 ticks = 10 secondes

        return true;
    }
    return false;
}
@Override
public void onEnable() {
    Bukkit.getLogger().info("[UHCPlugin] Le plugin est activé !");
    GameManager.setGameState(GameManager.GameState.WAITING);
    Bukkit.getLogger().info("[UHCPlugin] État du jeu défini sur WAITING.");
    saveDefaultConfig();
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getPluginManager().registerEvents(new RoleMenu(this), this); // Ajoutez cette ligne
}
}
