package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

public class Main extends JavaPlugin implements Listener { // ✅ Ajout de implements Listener

    @Override
    public void onEnable() {
        Bukkit.getLogger().info("[UHCPlugin] Le plugin est activé !");
        GameManager.setGameState(GameManager.GameState.WAITING);
        Bukkit.getLogger().info("[UHCPlugin] État du jeu défini sur WAITING.");

        // Charger la configuration
        saveDefaultConfig();

        // Enregistrer les événements
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
        if (command.getName().equalsIgnoreCase("startuhc")) {
            if (GameManager.getGameState() != GameManager.GameState.WAITING) {
                sender.sendMessage(ChatColor.RED + "La partie a déjà commencé ou est terminée !");
                return true;
            }

            GameManager.setGameState(GameManager.GameState.STARTING);
            Bukkit.broadcastMessage(ChatColor.GREEN + "[UHC] La partie va commencer dans 10 secondes !");

            // Ajout d'un délai avant le début
            Bukkit.getScheduler().runTaskLater(this, () -> {
                GameManager.setGameState(GameManager.GameState.PLAYING);
                Bukkit.broadcastMessage(ChatColor.GOLD + "[UHC] La partie commence maintenant !");
            }, 200L); // 10 secondes (20 ticks = 1 seconde)

            return true;
        }

        // Gestion de la commande /uhctest
        if (command.getName().equalsIgnoreCase("uhctest")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage(ChatColor.GREEN + "Le plugin fonctionne !");
            } else {
                sender.sendMessage(ChatColor.RED + "Cette commande ne peut être exécutée que par un joueur !");
            }
            return true;
        }

        return false;
    }

    // ✅ Donne une boussole aux joueurs quand ils rejoignent la phase WAITING
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Vérifie si la partie est en attente
        if (GameManager.getGameState() == GameManager.GameState.WAITING) {
            // ✅ Vider l'inventaire du joueur
            player.getInventory().clear();

            // ✅ Donner la boussole
            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta compassMeta = compass.getItemMeta();
            if (compassMeta != null) {
                compassMeta.setDisplayName(ChatColor.GOLD + "Menu UHC");
                compass.setItemMeta(compassMeta);
            }

            // ✅ Ajouter la boussole au slot 4
            player.getInventory().setItem(4, compass);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Vérifie si le menu ouvert est le "Menu UHC"
        if (event.getView().getTitle().equals(ChatColor.GOLD + "Menu UHC")) {
            event.setCancelled(true); // ✅ Annule le déplacement d'items
        }
    }

    // ✅ Événement quand un joueur clique avec la boussole
    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Vérifie si l'item est une boussole et s'appelle "Menu UHC"
        if (item.getType() == Material.COMPASS && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getDisplayName().equals(ChatColor.GOLD + "Menu UHC")) {

                // Création de l'inventaire GUI
                Inventory menu = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Menu UHC");

                // Ajout des objets dans le menu
                ItemStack jumpItem = new ItemStack(Material.BLUE_CONCRETE);
                ItemMeta jumpMeta = jumpItem.getItemMeta();
                if (jumpMeta != null) {
                    jumpMeta.setDisplayName(ChatColor.RED + "Jump");
                    jumpItem.setItemMeta(jumpMeta);
                }

                ItemStack spawnItem = new ItemStack(Material.RED_BED);
                ItemMeta spawnMeta = spawnItem.getItemMeta();
                if (spawnMeta != null) {
                    spawnMeta.setDisplayName(ChatColor.RED + "Spawn");
                    spawnItem.setItemMeta(spawnMeta);
                }

                // Placer les objets dans le menu
                menu.setItem(0, jumpItem);
                menu.setItem(4, spawnItem);

                // Ouvrir le menu pour le joueur
                player.openInventory(menu);
            }
        }
    }
}
