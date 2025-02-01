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
        if (GameManager.getGameState() == GameManager.GameState.STARTING) {
            Bukkit.getLogger().info("Le jeu est en mode STARTING !");
        }
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

            // Récupère le délai du PvP et des rôles depuis la config
            int pvpDelay = getConfig().getInt("pvp-timer", 10) * 60; // Convertit en secondes
            int roleDelay = getConfig().getInt("role-announcement-delay", 10); // Par défaut : 10s
            long roleDelayTicks = roleDelay * 20L; // Convertit en ticks (1s = 20 ticks)

// Annonce du délai avant l'attribution des rôles
            Bukkit.broadcastMessage(ChatColor.YELLOW + "🎭 Les rôles seront révélés dans " + roleDelay + " secondes !");

            Bukkit.getScheduler().runTaskLater(this, () -> {
                // Attribution des rôles après le délai défini
                new RoleManager(this).assignRoles();
                GameManager.setGameState(GameManager.GameState.PLAYING);
                Bukkit.broadcastMessage(ChatColor.GOLD + "🎭 Les rôles ont été attribués !");

                // Annonce du délai d'activation du PvP
                Bukkit.broadcastMessage(ChatColor.RED + "⚔ Le PvP sera activé dans " + (pvpDelay / 60) + " minutes !");

                // Activation du PvP après le délai défini
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Bukkit.broadcastMessage(ChatColor.RED + "⚔ Le PvP est maintenant activé !");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule pvp true");
                }, pvpDelay * 20L); // Convertit en ticks (1 sec = 20 ticks)

            }, roleDelayTicks); // Applique le délai défini pour l'annonce des rôles

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
        // 🔹 Applique le scoreboard au joueur
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
        // Récupère l'objet cliqué
        ItemStack clickedItem = event.getCurrentItem();

        // Vérifie que l'objet n'est pas null et a bien un ItemMeta
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();

        if (inventoryTitle.equals(ChatColor.GOLD + "Menu UHC")) {
            event.setCancelled(true);
            handleMainMenuClick(player, clickedItem);

        } else if (inventoryTitle.equals(ChatColor.YELLOW + "Configuration UHC")) {
            event.setCancelled(true);
            handleConfigMenuClick(player, clickedItem);

            switch (clickedItem.getType()) {
                case ARROW: // Retour au menu principal
                    openMainMenu(player);
                    break;

                case DIAMOND_SWORD: // ⚔️ Modifier le timer du PvP
                    if (!player.hasPermission("uhcplugin.config")) {
                        player.sendMessage(ChatColor.RED + "❌ Tu n'as pas la permission de modifier la configuration !");
                        return;
                    }

                    int currentPvpTime = getConfig().getInt("pvp-timer", 10); // Par défaut 10 min
                    if (event.isLeftClick()) {
                        currentPvpTime += 1; // ⬆️ Augmente de 1 min
                    } else if (event.isRightClick() && currentPvpTime > 1) {
                        currentPvpTime -= 1; // ⬇️ Diminue de 1 min (min 1)
                    }

                    getConfig().set("pvp-timer", currentPvpTime);
                    saveConfig();
                    player.sendMessage(ChatColor.GREEN + "⏳ Temps avant PvP mis à jour : " + currentPvpTime + " minutes !");

                    // ✅ Met à jour l'affichage dans le menu
                    openConfigMenu(player);
                    break;

                case PAPER: // 📜 Modifier le délai d'annonce des rôles
                    if (!player.hasPermission("uhcplugin.config")) {
                        player.sendMessage(ChatColor.RED + "❌ Tu n'as pas la permission de modifier la configuration !");
                        return;
                    }

                    int currentRoleTime = getConfig().getInt("role-announcement-delay", 10); // Par défaut 10 sec
                    if (event.isLeftClick()) {
                        currentRoleTime += 5; // ⬆️ Augmente de 5 sec
                    } else if (event.isRightClick() && currentRoleTime > 5) {
                        currentRoleTime -= 5; // ⬇️ Diminue de 5 sec (min 5)
                    }

                    getConfig().set("role-announcement-delay", currentRoleTime);
                    saveConfig();
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "🎭 Temps avant annonce des rôles mis à jour : " + currentRoleTime + " secondes !");

                    // ✅ Met à jour l'affichage dans le menu
                    openConfigMenu(player);
                    break;
            }

            // Vérifie si le joueur clique sur le livre "Gérer les Rôles"
            if (clickedItem.getType() == Material.BOOK) {
                if (player.hasPermission("uhcplugin.config")) {
                    new RoleMenu(this).openRoleMenu(player); // Ouvre le menu des rôles
                } else {
                    player.sendMessage(ChatColor.RED + "❌ Tu n'as pas la permission d'accéder à la gestion des rôles !");
                }
            }

        } else if (inventoryTitle.equals(ChatColor.GOLD + "Activation des rôles")) {
            event.setCancelled(true); // Empêche de déplacer les items

            // Gestion du retour
            if (clickedItem.getType() == Material.ARROW) {
                openConfigMenu(player); // Retour à la config UHC
            } else {
                // Active/Désactive les rôles
                String roleName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
                boolean newState = !getConfig().getBoolean("roles." + roleName);
                getConfig().set("roles." + roleName, newState);
                saveConfig();

                // Met à jour l'affichage
                ItemStack newItem = new ItemStack(newState ? Material.LIME_DYE : Material.RED_DYE);
                ItemMeta meta = newItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName((newState ? ChatColor.GREEN : ChatColor.RED) + roleName);
                    newItem.setItemMeta(meta);
                }
                event.getInventory().setItem(event.getSlot(), newItem);
                player.sendMessage(ChatColor.GREEN + "Le rôle " + roleName + " est maintenant " + (newState ? "activé" : "désactivé") + " !");

                // ✅ Mise à jour du scoreboard pour tous les joueurs
                scoreboardManager.updateAllScoreboards();
            }
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

    private void handleConfigMenuClick(Player player, ItemStack clickedItem) {
        if (!player.hasPermission("uhcplugin.config")) {
            player.sendMessage(ChatColor.RED + "Tu n'as pas la permission de modifier la configuration !");
            return;
        }

        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (itemName.equals("Taille de la Bordure")) {
            int currentSize = getConfig().getInt("border-size", 100);
            int newSize = (currentSize == 100) ? 50 : 100; // Alterne entre 100 et 50 blocs
            getConfig().set("border-size", newSize);
            saveConfig();

            // Applique la nouvelle taille de la bordure
            Bukkit.getWorld("world").getWorldBorder().setSize(newSize);

            player.sendMessage(ChatColor.GREEN + "Taille de la bordure mise à jour : " + newSize);
        }
        else if (itemName.equals("Temps avant PvP")) {
            int currentTime = getConfig().getInt("pvp-timer", 10);
            int newTime = (currentTime == 10) ? 5 : 10;
            getConfig().set("pvp-timer", newTime);
            saveConfig();

            // 🔹 Met à jour le scoreboard après modification des rôles ou du PvP
            scoreboardManager.updateScoreboard(player);

            player.sendMessage(ChatColor.GREEN + "Temps avant PvP mis à jour : " + newTime + " minutes !");
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
                menu.setItem(i, blackGlass);
            } else {
                menu.setItem(i, redGlass);
            }
        }
        ItemStack roleItem = new ItemStack(Material.BOOK);
        ItemMeta roleMeta = roleItem.getItemMeta();
        if (roleMeta != null) {
            roleMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Gérer les Rôles");
            roleItem.setItemMeta(roleMeta);
        }

        menu.setItem(10, jumpItem);
        menu.setItem(16, spawnItem);
        menu.setItem(31, configItem);

        player.openInventory(menu);
    }

    public void openConfigMenu(Player player) {
        Inventory configMenu = Bukkit.createInventory(null, 9, ChatColor.YELLOW + "Configuration UHC");

        // Récupère les valeurs actuelles pour l'affichage
        int currentPvpTime = getConfig().getInt("pvp-timer", 10);
        int currentRoleTime = getConfig().getInt("role-announcement-delay", 10);

        // Création des items avec les valeurs dynamiques
        ItemStack borderSize = createItem(Material.BARRIER, ChatColor.RED + "Taille de la Bordure");
        ItemStack pvpTimer = createItem(Material.DIAMOND_SWORD, ChatColor.RED + "Temps avant PvP: " + ChatColor.GOLD + currentPvpTime + " min");
        ItemStack roleTimer = createItem(Material.PAPER, ChatColor.LIGHT_PURPLE + "Temps avant annonce des rôles: " + ChatColor.GOLD + currentRoleTime + " sec");
        ItemStack roleManager = createItem(Material.BOOK, ChatColor.GOLD + "📜 Gérer les Rôles");
        ItemStack backButton = createItem(Material.ARROW, ChatColor.GRAY + "Retour");

        // Placement des items dans l'inventaire
        configMenu.setItem(0, borderSize);
        configMenu.setItem(1, pvpTimer);
        configMenu.setItem(2, roleTimer); // ✅ Ajout ici
        configMenu.setItem(4, roleManager);
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


}
