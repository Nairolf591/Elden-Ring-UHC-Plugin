package me.uhcplugin;

import org.bukkit.ChatColor; // Pour utiliser ChatColor
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration; // Pour utiliser FileConfiguration
import org.bukkit.inventory.Inventory; // Pour utiliser Inventory


public class ConfirmStuffCommand implements CommandExecutor {
    private final Main plugin;

    public ConfirmStuffCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
        sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
        return true;
    }

    Player player = (Player) sender;

    // Sauvegarde le stuff actuel
    savePlayerStuff(player);

    // Restaure l'inventaire précédent
    Main main = (Main) plugin;
    ItemStack[] savedInventory = main.getOriginalInventory(player.getUniqueId());
    ItemStack[] savedArmor = main.getOriginalArmor(player.getUniqueId());

    if (savedInventory != null && savedArmor != null) {
        player.getInventory().setContents(savedInventory);
        player.getInventory().setArmorContents(savedArmor);
        main.clearSavedInventory(player.getUniqueId());
    }

    player.sendMessage(ChatColor.GREEN + "✅ Stuff sauvegardé avec succès et inventaire restauré !");
    return true;
 }

    public void savePlayerStuff(Player player) {
        FileConfiguration config = plugin.getConfig();
        Inventory inv = player.getInventory();

        // Sauvegarder chaque slot de l'inventaire
        for (int i = 0; i < 36; i++) { // 36 slots = 9 rangées de 4
            ItemStack item = inv.getItem(i);
            config.set("stuff." + i, item);
        }
        plugin.saveConfig();
    }
}