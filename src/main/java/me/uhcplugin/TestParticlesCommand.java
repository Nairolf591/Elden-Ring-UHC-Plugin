package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TestParticlesCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public TestParticlesCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Seuls les joueurs peuvent exécuter cette commande !");
            return true;
        }

        Player player = (Player) sender;

        // Envoie différents types de particules autour du joueur
        player.spawnParticle(Particle.FLAME, player.getLocation(), 20);
        player.spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 10);
        player.spawnParticle(Particle.END_ROD, player.getLocation(), 15);
        player.spawnParticle(Particle.CRIT, player.getLocation(), 30);
        player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 15);

        player.sendMessage(ChatColor.GREEN + "✔ Particules de test générées ! Vérifie si elles s'affichent bien sur Bedrock.");

        return true;
    }
}
