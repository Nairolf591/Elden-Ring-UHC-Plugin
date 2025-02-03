package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class ConfirmStuffCommand implements CommandExecutor {
    private final Main plugin;

    public ConfirmStuffCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "❌ Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        // 📌 Sauvegarde le stuff actuel
        savePlayerStuff(player);

        // 📌 Restaure l'inventaire précédent
        restorePlayerStuff(player);

        player.sendMessage(ChatColor.GREEN + "✅ Stuff sauvegardé avec succès et inventaire restauré !");
        return true;
    }

    private void savePlayerStuff(Player player) {
        FileConfiguration config = plugin.getConfig();
        Inventory inv = player.getInventory();
        UUID playerId = player.getUniqueId();

        // ✅ Sauvegarde chaque slot de l’inventaire (évite les items nulls)
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            config.set("stuff." + playerId + ".inv." + i, item != null ? item.serialize() : null);
        }

        // ✅ Sauvegarde l'armure
        ItemStack[] armorContents = ((org.bukkit.inventory.PlayerInventory) inv).getArmorContents();
        for (int i = 0; i < armorContents.length; i++) {
            config.set("stuff." + playerId + ".armor." + i, armorContents[i] != null ? armorContents[i].serialize() : null);
        }

        // ✅ Sauvegarde la main secondaire
        ItemStack offHand = ((org.bukkit.inventory.PlayerInventory) inv).getItemInOffHand();
        config.set("stuff." + playerId + ".offhand", offHand != null ? offHand.serialize() : null);

        plugin.saveConfig();
    }

    private void restorePlayerStuff(Player player) {
        FileConfiguration config = plugin.getConfig();
        UUID playerId = player.getUniqueId();

        // 📌 Restaure l'inventaire
        for (int i = 0; i < 36; i++) {
            if (config.contains("stuff." + playerId + ".inv." + i)) {
                player.getInventory().setItem(i, config.getItemStack("stuff." + playerId + ".inv." + i));
            }
        }

        // 📌 Restaure l'armure
        ItemStack[] armorContents = new ItemStack[4];
        for (int i = 0; i < armorContents.length; i++) {
            if (config.contains("stuff." + playerId + ".armor." + i)) {
                armorContents[i] = config.getItemStack("stuff." + playerId + ".armor." + i);
            }
        }
        player.getInventory().setArmorContents(armorContents);

        // 📌 Restaure la main secondaire
        if (config.contains("stuff." + playerId + ".offhand")) {
            player.getInventory().setItemInOffHand(config.getItemStack("stuff." + playerId + ".offhand"));
        }

        // ✅ Nettoyage après restauration
        plugin.clearSavedInventory(playerId);
    }
}