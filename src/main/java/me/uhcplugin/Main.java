package me.uhcplugin;

import me.uhcplugin.role.Margit;

import org.bukkit.*;
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import java.io.*;
import static org.bukkit.Material.ARROW;
import static org.bukkit.Material.BOOK;

public class Main extends JavaPlugin implements Listener {

    private static Main instance;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
    RoleManager roleManager = new RoleManager(this);
    new Margit(this, roleManager);
        instance = this; // Stocke l'instance du plugin
        Bukkit.getLogger().info("[UHCPlugin] Le plugin est activé !");
        saveDefaultConfig();

        // ✅ Charge l'état de la partie depuis la config
        String savedState = getConfig().getString("game-state", "WAITING");

        try {
            GameManager.setGameState(GameManager.GameState.valueOf(savedState));
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[UHCPlugin] 🚨 État inconnu dans la config ! Réinitialisation à WAITING.");
            GameManager.setGameState(GameManager.GameState.WAITING);
        }

        // 🔄 Vérifie que l'état ne reste pas en ENDED après un reload
        if (GameManager.getGameState() == GameManager.GameState.ENDED) {
            GameManager.setGameState(GameManager.GameState.WAITING);
        }

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new RoleMenu(this), this);
        scoreboardManager = new ScoreboardManager(this);

        // 🔄 Met à jour le scoreboard de tous les joueurs connectés après un reload
        Bukkit.getOnlinePlayers().forEach(scoreboardManager::setPlayerScoreboard);

        Bukkit.getLogger().info("[UHCPlugin] 🎯 État actuel du jeu : " + GameManager.getGameState());
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("[UHCPlugin] Le plugin est désactivé !");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent exécuter cette commande !");
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("startuhc")) {
            if (!player.hasPermission("uhcplugin.startuhc")) {
                player.sendMessage(ChatColor.RED + "Tu n'as pas la permission de démarrer l'UHC !");
                return true;
            }

            if (GameManager.getGameState() != GameManager.GameState.WAITING) {
                player.sendMessage(ChatColor.RED + "La partie a déjà commencé ou est terminée !");
                return true;
            }

            World uhcWorld = Bukkit.getWorld("uhc");
            if (uhcWorld == null) {
                player.sendMessage(ChatColor.RED + "Le monde UHC n'existe pas !");
                return true;
            }

            Location spawn = uhcWorld.getSpawnLocation();
            WorldBorder border = uhcWorld.getWorldBorder();
            border.setCenter(spawn.getX(), spawn.getZ());
            border.setSize(getConfig().getInt("border-size", 500));

            Bukkit.broadcastMessage(ChatColor.RED + "🌍 La bordure a été positionnée sur le spawn du monde UHC !");
            GameManager.setGameState(GameManager.GameState.STARTING);
            Bukkit.broadcastMessage(ChatColor.GOLD + "L'UHC démarre dans 10 secondes !");

            Bukkit.getScheduler().runTaskLater(this, () -> {
                new RoleManager(this).assignRoles();
                GameManager.setGameState(GameManager.GameState.PLAYING);
                Bukkit.broadcastMessage(ChatColor.GOLD + "Les rôles ont été attribués !");
            }, 200L); // 10 secondes

            return true;
        }

        if (command.getName().equalsIgnoreCase("enduhc")) {
            if (!player.hasPermission("uhcplugin.enduhc")) {
                player.sendMessage(ChatColor.RED + "❌ Tu n'as pas la permission de terminer la partie !");
                return true;
            }

            GameManager.setGameState(GameManager.GameState.ENDED);
            Bukkit.broadcastMessage(ChatColor.RED + "🏁 La partie a été forcée à se terminer par " + player.getName() + " !");

            // ⏳ Ajoute un délai avant de reset le monde (ex: 10 secondes)
            Bukkit.getScheduler().runTaskLater(this, () -> {
                resetUHCWorld();
            }, 200L); // 10 secondes

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

    @EventHandler
    public void onWorldSpawnChange(org.bukkit.event.world.SpawnChangeEvent event) {
        World world = event.getWorld();

        // ⚠ Vérifie qu'on est bien dans le monde UHC avant de modifier la bordure
        if (!world.getName().equalsIgnoreCase("uhc")) return;

        Location newSpawn = world.getSpawnLocation();
        WorldBorder border = world.getWorldBorder();

        border.setCenter(newSpawn.getX(), newSpawn.getZ());
        Bukkit.broadcastMessage(ChatColor.GREEN + "🌍 La bordure du monde UHC a été recentrée sur le spawn !");
    }

    public void resetUHCWorld() {
        Bukkit.broadcastMessage(ChatColor.RED + "🔄 Réinitialisation du monde UHC...");

        File uhcWorld = new File(Bukkit.getWorldContainer(), "uhc");
        File backupWorld = new File(Bukkit.getWorldContainer(), "uhc_backup");

        if (!backupWorld.exists()) {
            Bukkit.getLogger().severe("[UHCPlugin] ❌ Aucun backup trouvé pour 'uhc_backup' !");
            return;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv delete uhc");
        deleteWorld(uhcWorld);

        try {
            copyDirectory(backupWorld, uhcWorld);
            Bukkit.getLogger().info("[UHCPlugin] ✅ Le monde UHC a été restauré avec succès !");
        } catch (IOException e) {
            Bukkit.getLogger().severe("[UHCPlugin] ❌ Erreur lors de la restauration du monde UHC !");
            e.printStackTrace();
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv import uhc normal");
        Bukkit.broadcastMessage(ChatColor.GREEN + "🌍 Le monde UHC a été restauré !");
    }

    // Méthode pour supprimer un dossier (utilisée pour supprimer le monde UHC avant de le restaurer)
    private void deleteWorld(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorld(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        path.delete();
    }

    // Méthode pour copier un dossier (utilisée pour restaurer le backup)
    private void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdirs();
            }
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    copyDirectory(new File(source, file), new File(target, file));
                }
            }
        } else {
            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        }
    }

    public static Main getInstance() {
        return instance;
    }
}
