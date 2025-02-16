package me.uhcplugin;

import org.bukkit.ChatColor; import org.bukkit.command.Command; import org.bukkit.command.CommandExecutor; import org.bukkit.command.CommandSender; import org.bukkit.entity.Player;

public class GiveCompassCommand implements CommandExecutor {
    private final Main plugin;

    public GiveCompassCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent utiliser cette commande.");
            return true;
        }
        Player player = (Player) sender;
        plugin.giveCompass(player); // Assurez-vous que la méthode giveCompass est publique dans Main
        player.sendMessage(ChatColor.GREEN + "Ta boussole du menu t'a été redonnée.");
        return true;
    }

}
