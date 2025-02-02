package me.uhcplugin;

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
import java.util.Random;
import static org.bukkit.Material.ARROW;
import static org.bukkit.Material.BOOK;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.UUID; // Pour utiliser UUID dans les maps
import java.util.ArrayList; // Pour la liste dans l'aperçu du stuff
import java.util.List; // Pour la liste dans l'aperçu du stuff
import java.util.Map; // Pour les maps
import java.util.HashMap; // Pour les maps
import org.bukkit.inventory.InventoryView; // Pour event.getView()

public class Main extends JavaPlugin implements Listener {

    private static Main instance;
    private ScoreboardManager scoreboardManager;
    private final Map<UUID, ItemStack[]> originalInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> originalArmor = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getLogger().info("[UHCPlugin] Le plugin est en cours d'activation...");
        this.getCommand("confirmstuff").setExecutor(new ConfirmStuffCommand(this));

        try {
            saveDefaultConfig();

            // ✅ Initialiser ScoreboardManager AVANT GameManager
            scoreboardManager = new ScoreboardManager(this);

            // ✅ Charger l'état du jeu APRÈS avoir initialisé ScoreboardManager
            String savedState = getConfig().getString("game-state", "WAITING");
            try {
                GameManager.setGameState(GameManager.GameState.valueOf(savedState));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[UHCPlugin] 🚨 État inconnu dans la config ! Réinitialisation à WAITING.");
                GameManager.setGameState(GameManager.GameState.WAITING);
            }

            getServer().getPluginManager().registerEvents(this, this);
            getServer().getPluginManager().registerEvents(new RoleMenu(this), this);

            // ✅ Vérification pour éviter d'appeler updateAllScoreboards() sur null
            if (scoreboardManager != null) {
                Bukkit.getOnlinePlayers().forEach(scoreboardManager::setPlayerScoreboard);
            } else {
                Bukkit.getLogger().severe("[UHCPlugin] ❌ ScoreboardManager est NULL après initialisation !");
            }

            Bukkit.getLogger().info("[UHCPlugin] ✅ Activation terminée !");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[UHCPlugin] ❌ Une erreur est survenue au démarrage !");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("[UHCPlugin] Le plugin est désactivé !");
    }

    public Location getRandomSpawnLocation(World world, Location center, double borderSize) {
        Random random = new Random();
        Location spawnLocation;

        do {
            // Génère des coordonnées aléatoires dans le rayon de la bordure
            double x = center.getX() + (random.nextDouble() * borderSize * 2 - borderSize);
            double z = center.getZ() + (random.nextDouble() * borderSize * 2 - borderSize);
            double y = world.getHighestBlockYAt((int) x, (int) z); // Trouve le sol

            spawnLocation = new Location(world, x, y + 1, z); // +1 pour éviter d'être dans le sol

        } while (spawnLocation.getBlock().getType() == Material.WATER || spawnLocation.getBlock().getType() == Material.LAVA);
        // ⚠️ Vérifie qu'on ne spawn pas dans l'eau ou la lave

        return spawnLocation;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent exécuter cette commande !");
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("jump")) {
            if (GameManager.getGameState() == GameManager.GameState.PLAYING && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "❌ Tu ne peux pas utiliser cette commande en pleine partie !");
                return true;
            }

            // Téléporte normalement si la condition est respectée
            teleportPlayer(player, command.getName().equalsIgnoreCase("jump") ? "jump-location" : "spawn-location");
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

            // 🔥 Récupère le monde UHC
            World uhcWorld = Bukkit.getWorld("uhc");
            if (uhcWorld == null) {
                player.sendMessage(ChatColor.RED + "Le monde UHC n'existe pas !");
                return true;
            }

            Location center = uhcWorld.getWorldBorder().getCenter();
            double borderSize = uhcWorld.getWorldBorder().getSize() / 2; // Rayon de la bordure

            // ✅ Centre la bordure sur le spawn actuel
            uhcWorld.getWorldBorder().setCenter(center.getX(), center.getZ());
            uhcWorld.getWorldBorder().setSize(getConfig().getInt("border-size", 500));

            Bukkit.broadcastMessage(ChatColor.RED + "🌍 La bordure a été positionnée et la partie commence !");

            // 🏁 Changer l'état de la partie en STARTING
            GameManager.setGameState(GameManager.GameState.STARTING);
            Bukkit.broadcastMessage(ChatColor.GOLD + "L'UHC démarre dans 10 secondes !");

            // 🚀 Téléporte chaque joueur aléatoirement dans la bordure
            for (Player p : Bukkit.getOnlinePlayers()) {
                Location randomSpawn = getRandomSpawnLocation(uhcWorld, center, borderSize);
                p.teleport(randomSpawn);
                p.sendMessage(ChatColor.GREEN + "📌 Tu as été téléporté à un emplacement aléatoire !");
            }

            // ⏳ Début du timer pour l'assignation des rôles et le passage à PLAYING
            int roleDelay = getConfig().getInt("role-announcement-delay", 10); // Récupère la valeur depuis config.yml (10 par défaut)
            int ticks = roleDelay * 20; // Convertit en ticks (1s = 20 ticks)

            Bukkit.broadcastMessage(ChatColor.GOLD + "📢 Attribution des rôles dans " + roleDelay + " secondes...");
            new CountdownTimer(this, roleDelay, (secondsLeft) -> {
                scoreboardManager.updateRoleTimer(secondsLeft);
            }).start();

            Bukkit.getScheduler().runTaskLater(this, () -> {
                new RoleManager(this).assignRoles();
                GameManager.setGameState(GameManager.GameState.PLAYING);
                Bukkit.broadcastMessage(ChatColor.GOLD + "🎭 Les rôles ont été attribués !");
            }, ticks);

        }

        if (command.getName().equalsIgnoreCase("enduhc")) {
            if (!player.hasPermission("uhcplugin.enduhc")) {
                player.sendMessage(ChatColor.RED + "❌ Tu n'as pas la permission de terminer la partie !");
                return true;
            }

            GameManager.setGameState(GameManager.GameState.ENDED);
            Bukkit.broadcastMessage(ChatColor.RED + "🏁 La partie a été forcée à se terminer par " + player.getName() + " !");

            return true;
        }

        return false;
    }

    private void teleportPlayer(Player player, String locationKey) {
        if (!getConfig().contains(locationKey)) {
            player.sendMessage(ChatColor.RED + "❌ La localisation '" + locationKey + "' n'est pas définie !");
            return;
        }

        String worldName = getConfig().getString(locationKey + ".world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage(ChatColor.RED + "❌ Le monde '" + worldName + "' n'existe pas !");
            return;
        }

        double x = getConfig().getDouble(locationKey + ".x");
        double y = getConfig().getDouble(locationKey + ".y");
        double z = getConfig().getDouble(locationKey + ".z");
        float yaw = (float) getConfig().getDouble(locationKey + ".yaw");
        float pitch = (float) getConfig().getDouble(locationKey + ".pitch");

        Location teleportLocation = new Location(world, x, y, z, yaw, pitch);
        player.teleport(teleportLocation);
        player.sendMessage(ChatColor.GREEN + "✅ Téléporté à " + locationKey.replace("-", " ") + " !");
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
        ItemStack clickedItem = event.getCurrentItem(); // ✅ Déclaré une seule fois ici

        // 🔍 Debug : Voir si l'événement est bien capté
        Bukkit.getLogger().info("DEBUG - onInventoryClick détecté");

        // Vérifie que l'objet n'est pas null et a bien un ItemMeta
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();

        // 🔍 Debug : Voir quel menu est détecté
        Bukkit.getLogger().info("DEBUG - Menu détecté : " + inventoryTitle);

        // 🛑 Bloque le déplacement des items pour tous les menus
        if (inventoryTitle.contains("Configuration") || inventoryTitle.contains("Stuff") || inventoryTitle.contains("UHC")) {
            event.setCancelled(true);
        }

        // 📌 Gestion du menu "Configuration UHC"
        if (inventoryTitle.equals(ChatColor.YELLOW + "Configuration UHC")) {
            event.setCancelled(true);
            Bukkit.getLogger().info("DEBUG - Clic dans Configuration UHC");
            handleConfigMenuClick(player, clickedItem, event);
            return;
        }

        // 📌 Gestion du menu "Configuration du Stuff"
        if (inventoryTitle.equals(ChatColor.GOLD + "Configuration du Stuff")) {
            event.setCancelled(true);
            Bukkit.getLogger().info("DEBUG - Clic dans Configuration du Stuff");
            handleStuffConfigMenuClick(player, clickedItem);
            return;
        }

        // 📌 Gestion du menu "Aperçu du Stuff"
        if (inventoryTitle.equals(ChatColor.GOLD + "Aperçu du Stuff")) {
            event.setCancelled(true);
            if (clickedItem.getType() == Material.ARROW) {
                Bukkit.getLogger().info("DEBUG - Retour Aperçu du Stuff");
                openStuffConfigMenu(player);
            }
            return;
        }

        // 📌 Gestion du menu "Menu UHC"
        if (inventoryTitle.equals(ChatColor.GOLD + "Menu UHC")) {
            event.setCancelled(true);
            Bukkit.getLogger().info("DEBUG - Clic dans Menu UHC");
            handleMainMenuClick(player, clickedItem);
            return;
        }

        // ✅ Ajout des boutons spécifiques au menu Configuration
        switch (clickedItem.getType()) {
            case ARROW:
                Bukkit.getLogger().info("DEBUG - Bouton Retour cliqué");
                openMainMenu(player);
                break;

            case BARRIER:
                Bukkit.getLogger().info("DEBUG - Bouton Bordure cliqué");
                if (!player.hasPermission("uhcplugin.config")) {
                    player.sendMessage(ChatColor.RED + "❌ Tu n'as pas la permission de modifier la configuration !");
                    return;
                }
                int currentSize = getConfig().getInt("border-size", 500);
                if (event.isLeftClick() && currentSize < 2000) currentSize += 100;
                else if (event.isRightClick() && currentSize > 100) currentSize -= 100;

                getConfig().set("border-size", currentSize);
                saveConfig();
                Bukkit.getWorld("uhc").getWorldBorder().setSize(currentSize);
                player.sendMessage(ChatColor.GREEN + "🌍 Taille de la bordure mise à jour : " + currentSize + " blocs.");
                openConfigMenu(player);
                break;

            case DIAMOND_SWORD:
                Bukkit.getLogger().info("DEBUG - Bouton PvP Timer cliqué");
                if (!player.hasPermission("uhcplugin.config")) return;
                int currentPvpTime = getConfig().getInt("pvp-timer", 10);
                if (event.isLeftClick()) currentPvpTime += 1;
                else if (event.isRightClick() && currentPvpTime > 1) currentPvpTime -= 1;
                getConfig().set("pvp-timer", currentPvpTime);
                saveConfig();
                player.sendMessage(ChatColor.GREEN + "⏳ Temps avant PvP mis à jour : " + currentPvpTime + " minutes !");
                openConfigMenu(player);
                break;

            case PAPER:
                Bukkit.getLogger().info("DEBUG - Bouton Temps rôles cliqué");
                if (!player.hasPermission("uhcplugin.config")) return;
                int currentRoleTime = getConfig().getInt("role-announcement-delay", 10);
                if (event.isLeftClick()) currentRoleTime += 5;
                else if (event.isRightClick() && currentRoleTime > 5) currentRoleTime -= 5;
                getConfig().set("role-announcement-delay", currentRoleTime);
                saveConfig();
                player.sendMessage(ChatColor.LIGHT_PURPLE + "🎭 Temps avant annonce des rôles mis à jour : " + currentRoleTime + " secondes !");
                openConfigMenu(player);
                break;

            case BOOK:
                Bukkit.getLogger().info("DEBUG - Bouton Gérer les Rôles cliqué");
                if (player.hasPermission("uhcplugin.config")) {
                    new RoleMenu(this).openRoleMenu(player);
                } else {
                    player.sendMessage(ChatColor.RED + "❌ Tu n'as pas la permission d'accéder à la gestion des rôles !");
                }
                break;
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

    private void handleConfigMenuClick(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        if (!player.hasPermission("uhcplugin.config")) {
            player.sendMessage(ChatColor.RED + "❌ Tu n'as pas la permission de modifier la configuration !");
            return;
        }

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            Bukkit.getLogger().info("DEBUG - Objet cliqué null ou sans meta");
            return;
        }

        String itemName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
        Bukkit.getLogger().info("DEBUG - Item cliqué : " + itemName);

        // 📏 Modifier la taille de la bordure
        if (itemName.contains("Bordure")) {
            int currentSize = getConfig().getInt("border-size", 500);
            int minSize = 100, maxSize = 2000;

            if (player.isSneaking()) {
                currentSize += 500; // ⬆️ Augmente de 500 blocs en sneaking
            } else if (event.isLeftClick()) {
                currentSize += 100; // ⬆️ Augmente de 100 blocs
            } else if (event.isRightClick() && currentSize > minSize) {
                currentSize -= 100; // ⬇️ Diminue de 100 blocs
            }

            currentSize = Math.max(minSize, Math.min(maxSize, currentSize));

            getConfig().set("border-size", currentSize);
            saveConfig();
            Bukkit.getWorld("uhc").getWorldBorder().setSize(currentSize);

            player.sendMessage(ChatColor.GREEN + "🌍 Taille de la bordure mise à jour : " + currentSize + " blocs !");
            openConfigMenu(player);

            // ⚔️ Modifier le timer du PvP
        } else if (itemName.contains("PvP")) {
            int currentTime = getConfig().getInt("pvp-timer", 10);
            if (event.isLeftClick()) {
                currentTime += 1;
            } else if (event.isRightClick() && currentTime > 1) {
                currentTime -= 1;
            }

            getConfig().set("pvp-timer", currentTime);
            saveConfig();
            player.sendMessage(ChatColor.GREEN + "⏳ Temps avant PvP mis à jour : " + currentTime + " minutes !");
            openConfigMenu(player);

            // 📜 Modifier le délai d’annonce des rôles
        } else if (itemName.contains("rôles")) {
            int currentRoleTime = getConfig().getInt("role-announcement-delay", 10);
            if (event.isLeftClick()) {
                currentRoleTime += 5;
            } else if (event.isRightClick() && currentRoleTime > 5) {
                currentRoleTime -= 5;
            }

            getConfig().set("role-announcement-delay", currentRoleTime);
            saveConfig();
            player.sendMessage(ChatColor.LIGHT_PURPLE + "🎭 Temps avant annonce des rôles mis à jour : " + currentRoleTime + " sec !");
            openConfigMenu(player);

            // 🎒 Ouvrir la gestion du stuff
        } else if (itemName.contains("Stuff")) {
            openStuffConfigMenu(player);

            // 📜 Ouvrir la gestion des rôles
        } else if (itemName.contains("Rôles")) {
            new RoleMenu(this).openRoleMenu(player);
        }
    }

    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.COMPASS && item.hasItemMeta() &&
                item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Menu UHC")) {
            openMainMenu(player);

            // Vérifie si la partie est en cours et si le joueur n'est pas OP
            if (GameManager.getGameState() == GameManager.GameState.PLAYING && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "❌ Tu ne peux pas ouvrir le menu en pleine partie !");
                event.setCancelled(true);
                return;
            }
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

        int currentPvpTime = getConfig().getInt("pvp-timer", 10);
        int currentRoleTime = getConfig().getInt("role-announcement-delay", 10);
        int currentBorderSize = getConfig().getInt("border-size", 500); // Taille de la bordure actuelle

        ItemStack borderSize = createItem(Material.BARRIER, ChatColor.RED + "📏 Bordure : " + ChatColor.GOLD + currentBorderSize + " blocs");
        ItemStack pvpTimer = createItem(Material.DIAMOND_SWORD, ChatColor.RED + "⚔️ Temps avant PvP: " + ChatColor.GOLD + currentPvpTime + " min");
        ItemStack roleTimer = createItem(Material.PAPER, ChatColor.LIGHT_PURPLE + "🎭 Temps avant rôles: " + ChatColor.GOLD + currentRoleTime + " sec");
        ItemStack roleManager = createItem(Material.BOOK, ChatColor.GOLD + "📜 Gérer les Rôles");
        ItemStack stuffManager = createItem(Material.CHEST, ChatColor.GOLD + "🎒 Configurer le Stuff"); // Nouvel item pour configurer le stuff
        ItemStack backButton = createItem(Material.ARROW, ChatColor.GRAY + "Retour");

        configMenu.setItem(0, borderSize);
        configMenu.setItem(1, pvpTimer);
        configMenu.setItem(2, roleTimer);
        configMenu.setItem(3, stuffManager);
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

        // 📌 Téléporte tous les joueurs vers le spawn du monde principal avant la suppression
        World mainWorld = Bukkit.getWorlds().get(0); // Prend le premier monde du serveur
        Location safeSpawn = mainWorld.getSpawnLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(safeSpawn);
            player.sendMessage(ChatColor.YELLOW + "🚀 Téléportation temporaire pendant la réinitialisation du monde UHC...");
        }

        // 🔴 Supprimer le monde UHC de Multiverse
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv remove uhc");
        deleteWorld(uhcWorld);

        // ✅ Restaurer le backup
        try {
            copyDirectory(backupWorld, uhcWorld);
            Bukkit.getLogger().info("[UHCPlugin] ✅ Le monde UHC a été restauré avec succès !");
        } catch (IOException e) {
            Bukkit.getLogger().severe("[UHCPlugin] ❌ Erreur lors de la restauration du monde UHC !");
            e.printStackTrace();
        }

        // 🔄 Réimporter le monde avec Multiverse
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

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public void openStuffConfigMenu(Player player) {
        Inventory stuffMenu = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Configuration du Stuff");

        // Bouton "Choisir le stuff"
        ItemStack chooseStuff = createItem(Material.CHEST, ChatColor.GREEN + "Choisir le Stuff");
        stuffMenu.setItem(11, chooseStuff);

        // Bouton "Aperçu du stuff"
        ItemStack previewStuff = createItem(Material.CHEST_MINECART, ChatColor.YELLOW + "Aperçu du Stuff");
        stuffMenu.setItem(13, previewStuff); // Position centrale

        // Bouton "Retour"
        ItemStack backButton = createItem(Material.ARROW, ChatColor.GRAY + "Retour");
        stuffMenu.setItem(15, backButton);

        player.openInventory(stuffMenu);
    }

    private void handleStuffConfigMenuClick(Player player, ItemStack clickedItem) {
        // 📌 Choisir le stuff
        if (clickedItem.getType() == Material.CHEST) {
            // ✅ Sauvegarde temporaire de l’inventaire du joueur
            originalInventories.put(player.getUniqueId(), player.getInventory().getContents());
            originalArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());

            // ✅ Vide l’inventaire pour laisser le joueur choisir
            player.getInventory().clear();
            player.sendMessage(ChatColor.GREEN + "🎒 Configurez votre stuff, puis utilisez /confirmstuff pour sauvegarder !");
        }

        // 📌 Aperçu du stuff
        else if (clickedItem.getType() == Material.CHEST_MINECART) {
            openStuffPreview(player);
        }

        // 📌 Retour au menu de config
        else if (clickedItem.getType() == Material.ARROW) {
            openConfigMenu(player);
        }
    }


    // Méthodes pour gérer l'inventaire original
    public ItemStack[] getOriginalInventory(UUID playerId) {
        return originalInventories.get(playerId);
    }

    public ItemStack[] getOriginalArmor(UUID playerId) {
        return originalArmor.get(playerId);
    }

    public void clearSavedInventory(UUID playerId) {
        originalInventories.remove(playerId);
        originalArmor.remove(playerId);
    }

    public void openStuffPreview(Player player) {
        Inventory previewInventory = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Aperçu du Stuff");

        // Récupère le stuff sauvegardé depuis la config
        FileConfiguration config = getConfig();
        for (int i = 0; i < 36; i++) { // 36 slots pour l'inventaire normal
            if (config.contains("stuff." + i)) {
                ItemStack item = config.getItemStack("stuff." + i);
                previewInventory.setItem(i, item);
            }
        }

        // Bouton "Retour"
        ItemStack backButton = createItem(Material.ARROW, ChatColor.GRAY + "Retour");
        previewInventory.setItem(53, backButton); // En bas à droite
        player.openInventory(previewInventory);
    }
}