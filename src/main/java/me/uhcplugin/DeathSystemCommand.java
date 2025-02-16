package me.uhcplugin;

import org.bukkit.ChatColor; import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class DeathSystemCommand implements CommandExecutor {
    private final Main plugin;

    public DeathSystemCommand(Main plugin) {  // Le nom du constructeur doit correspondre à celui de la classe
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("uhcplugin.deathsystem")) {
            sender.sendMessage(ChatColor.RED + "Tu n'as pas la permission d'utiliser cette commande.");
            return true;
        }
        if (args.length != 1 || (!args[0].equalsIgnoreCase("enable") && !args[0].equalsIgnoreCase("disable"))) {
            sender.sendMessage(ChatColor.RED + "Usage: /deathsystem <enable|disable>");
            return true;
        }
        boolean enable = args[0].equalsIgnoreCase("enable");
        plugin.getDeathManager().setEnabled(enable);
        sender.sendMessage(ChatColor.GREEN + "Le système de mort custom a été " + (enable ? "activé" : "désactivé") + ".");
        return true;
    }
}
