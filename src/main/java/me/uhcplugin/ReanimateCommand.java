package me.uhcplugin;

import org.bukkit.Bukkit; import org.bukkit.ChatColor; import org.bukkit.command.Command; import org.bukkit.command.CommandExecutor; import org.bukkit.command.CommandSender; import org.bukkit.entity.Player;

public class ReanimateCommand implements CommandExecutor {
    private final Main plugin;

    public ReanimateCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("uhcplugin.reanimate")) {
            sender.sendMessage(ChatColor.RED + "Tu n'as pas la permission d'utiliser cette commande.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /reanimate <joueur>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Joueur introuvable.");
            return true;
        }
        // Appel à la méthode reanimate du DeathManager
        if (plugin.getDeathManager().reanimate(target)) {
            sender.sendMessage(ChatColor.GREEN + "Le joueur " + target.getName() + " a été réanimé.");
        } else {
            sender.sendMessage(ChatColor.RED + "Ce joueur n'est pas en attente de réanimation.");
        }
        return true;
    }

}
