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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.Material.ARROW;
import static org.bukkit.Material.BOOK;

public class Main extends JavaPlugin implements Listener {

    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        Bukkit.getLogger().info("[UHCPlugin] Le plugin est activé !");
        GameManager.setGameState(GameManager.GameState.WAITING);
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new RoleMenu(this), this);
        scoreboardManager = new ScoreboardManager(this);
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("[UHCPlugin] Le plugin est désactivé !");
        GameManager.setGameState(GameManager.GameState.ENDED);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent exécuter cette commande !");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("jump")) {
            teleportPlayer(player, "jump-location");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            teleportPlayer(player, "spawn-location");
            return true;
        }

        if (command.getName().equalsIgnoreCase("startuhc")) {
            if (!player.hasPermission("uhcplugin.startuhc")) {
                player.sendMessage(ChatColor.RED + "Tu n'as pas la permission de démarrer l'UHC !");
                return true;
            }

            if (GameManager.getGameState() != GameManager.GameState.WAITING) {
                player.sendMessage(ChatColor.RED + "La partie a déjà commencé ou est terminée !");
                return true;
            }

            GameManager.setGameState(GameManager.GameState.STARTING);
            Bukkit.broadcastMessage(ChatColor.GOLD + "L'UHC démarre dans 10 secondes !");

            int pvpDelay = getConfig().getInt("pvp-timer", 10) * 60;
            int roleDelay = getConfig().getInt("role-announcement-delay", 10);
            long roleDelayTicks = roleDelay * 20L;

            Bukkit.broadcastMessage(ChatColor.YELLOW + "🎭 Les rôles seront révélés dans " + roleDelay + " secondes !");

            Bukkit.getScheduler().runTaskLater(this, () -> {
                new RoleManager(this).assignRoles();
                GameManager.setGameState(GameManager.GameState.PLAYING);
                Bukkit.broadcastMessage(ChatColor.GOLD + "🎭 Les rôles ont été attribués !");

                Bukkit.broadcastMessage(ChatColor.RED + "⚔ Le PvP sera activé dans " + (pvpDelay / 60) + " minutes !");

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Bukkit.broadcastMessage(ChatColor.RED + "⚔ Le PvP est maintenant activé !");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule pvp true");
                }, pvpDelay * 20L);

            }, roleDelayTicks);

            return true;
        }

        return false;
    }

    private void teleportPlayer(Player player, String locationKey) {
        String worldName = getConfig().getString(locationKey + ".world");
        double x = getConfig().getDouble(locationKey + ".x");
        double y = getConfig().getDouble(locationKey + ".y");
        double z = getConfig().getDouble(locationKey + ".z");
        float yaw = (float) getConfig().getDouble("jump-location.yaw") + 230;
        float pitch = (float) getConfig().getDouble(locationKey + ".pitch");

        if (worldName != null) {
            player.teleport(new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch));
            player.sendMessage(ChatColor.GREEN + "Téléporté à " + locationKey.replace("-", " ") + " !");
        } else {
            player.sendMessage(ChatColor.RED + "Le monde spécifié dans la config n'existe pas !");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (GameManager.getGameState() == GameManager.GameState.WAITING) {
            player.getInventory().clear();
            giveCompass(player);
        }
        scoreboardManager.setPlayerScoreboard(player);
    }

    private void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName(ChatColor.GOLD + "Menu UHC");
            compass.setItemMeta(compassMeta);
        }
        player.getInventory().setItem(4, compass);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();

        if (inventoryTitle.equals(ChatColor.GOLD + "Menu UHC")) {
            event.setCancelled(true);
            handleMainMenuClick(player, clickedItem);
        } else if (inventoryTitle.equals(ChatColor.YELLOW + "Configuration UHC")) {
            handleConfigMenu(event, player, clickedItem);
        } else if (inventoryTitle.equals(ChatColor.GOLD + "Activation des rôles")) {
            handleRoleActivation(event, player, clickedItem);
        }
    }

    private void handleMainMenuClick(Player player, ItemStack clickedItem) {
        if (clickedItem.getType() == Material.BLUE_CONCRETE) {
            player.performCommand("jump");
        } else if (clickedItem.getType() == Material.RED_BED) {
            player.performCommand("spawn");
        } else if (clickedItem.getType() == Material.COMMAND_BLOCK) {
            openConfigMenu(player);
        }
    }

    private void handleConfigMenu(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        event.setCancelled(true);
        switch (clickedItem.getType()) {
            case ARROW:
                openMainMenu(player);
                break;
            case DIAMOND_SWORD:
                handlePvpTimer(player, event);
                break;
            case PAPER:
                handleRoleTimer(player, event);
                break;
        }
        if (clickedItem.getType() == Material.BOOK) {
            if (player.hasPermission("uhcplugin.config")) {
                new RoleMenu(this).openRoleMenu(player);
            } else {
                player.sendMessage(ChatColor.RED + "❌ Permission manquante !");
            }
        }
    }

    private void handleRoleActivation(InventoryClickEvent event, Player player, ItemStack clickedItem) {
        event.setCancelled(true);
        if (clickedItem.getType() == Material.ARROW) {
            openConfigMenu(player);
        } else {
            String roleName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            boolean newState = !getConfig().getBoolean("roles." + roleName);
            getConfig().set("roles." + roleName, newState);
            saveConfig();
            updateRoleActivationDisplay(event, clickedItem, roleName, newState);
            scoreboardManager.updateAllScoreboards();
        }
    }

    private void updateRoleActivationDisplay(InventoryClickEvent event, ItemStack clickedItem, String roleName, boolean newState) {
        ItemStack newItem = new ItemStack(newState ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = newItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((newState ? ChatColor.GREEN : ChatColor.RED) + roleName);
            newItem.setItemMeta(meta);
        }
        event.getInventory().setItem(event.getSlot(), newItem);
        player.sendMessage(ChatColor.GREEN + "Rôle " + roleName + " " + (newState ? "activé" : "désactivé") + " !");
    }

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.COMPASS 
            && item.hasItemMeta()
            && item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Menu UHC")) {
            openMainMenu(player);
        }
    }

    public void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Menu UHC");
        setupMenuLayout(menu);
        player.openInventory(menu);
    }

    private void setupMenuLayout(Inventory menu) {
        ItemStack blackGlass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack redGlass = createItem(Material.RED_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            menu.setItem(i, (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) ? blackGlass : redGlass);
        }

        menu.setItem(10, createItem(Material.BLUE_CONCRETE, ChatColor.RED + "Jump"));
        menu.setItem(16, createItem(Material.RED_BED, ChatColor.RED + "Spawn"));
        menu.setItem(31, createItem(Material.COMMAND_BLOCK, ChatColor.YELLOW + "Configuration UHC"));
        menu.setItem(22, createRoleItem());
    }

    private ItemStack createRoleItem() {
        ItemStack roleItem = new ItemStack(Material.BOOK);
        ItemMeta meta = roleItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Gérer les Rôles");
            roleItem.setItemMeta(meta);
        }
        return roleItem;
    }

    public void openConfigMenu(Player player) {
        Inventory configMenu = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Configuration UHC");
        
        configMenu.setItem(0, createItem(Material.BARRIER, ChatColor.RED + "Taille de la Bordure"));
        configMenu.setItem(1, createPvpTimerItem());
        configMenu.setItem(2, createRoleTimerItem());
        configMenu.setItem(4, createItem(Material.BOOK, ChatColor.GOLD + "📜 Gérer les Rôles"));
        configMenu.setItem(8, createItem(Material.ARROW, ChatColor.GRAY + "Retour"));
        
        player.openInventory(configMenu);
    }

    private ItemStack createPvpTimerItem() {
        int currentPvpTime = getConfig().getInt("pvp-timer", 10);
        return createItem(Material.DIAMOND_SWORD, 
            ChatColor.RED + "Temps avant PvP: " + ChatColor.GOLD + currentPvpTime + " min");
    }

    private ItemStack createRoleTimerItem() {
        int currentRoleTime = getConfig().getInt("role-announcement-delay", 10);
        return createItem(Material.PAPER, 
            ChatColor.LIGHT_PURPLE + "Temps avant rôles: " + ChatColor.GOLD + currentRoleTime + " sec");
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
}