package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardManager {
    private final Main plugin;
    private Scoreboard scoreboard;
    private Objective objective;

    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        setupScoreboard();
    }

    // 🛠️ Initialise le scoreboard principal
    private void setupScoreboard() {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            Bukkit.getLogger().warning("[UHCPlugin] Impossible d'obtenir le ScoreboardManager !");
            return;
        }

        scoreboard = manager.getNewScoreboard();
        objective = scoreboard.registerNewObjective("uhc", "dummy", ChatColor.GOLD + "⚔ UHC Elden Ring");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    // 🔄 Met à jour le scoreboard d'un joueur
    public void setPlayerScoreboard(Player player) {
        if (scoreboard != null) {
            player.setScoreboard(scoreboard);
            updateScoreboard(player);
        }
    }

    // 🔄 Met à jour les scores dynamiques (joueurs, état du jeu, rôles activés)
    public void updateScoreboard(Player player) {
        if (objective == null) return;

        // 🔹 Réinitialise les anciens scores
        scoreboard.getEntries().forEach(scoreboard::resetScores);

        // 📌 Nombre de joueurs
        objective.getScore(ChatColor.AQUA + "👥 Joueurs : " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size()).setScore(5);

        // 📌 État de la partie
        objective.getScore(ChatColor.RED + "⚔ État : " + ChatColor.WHITE + GameManager.getGameState()).setScore(4);

        // 📌 Host (fixé à Flobill pour l'instant)
        objective.getScore(ChatColor.LIGHT_PURPLE + "👑 Host : " + ChatColor.WHITE + "Flobill").setScore(3);

        // 📌 Rôles activés
        List<String> activeRoles = getActiveRoles();
        if (!activeRoles.isEmpty()) {
            objective.getScore(ChatColor.GOLD + "🎭 Rôles activés :").setScore(2);

            int score = 1;
            for (String role : activeRoles) {
                objective.getScore(ChatColor.WHITE + "» " + role).setScore(score);
                score--;
            }
        }
    }

    // 🔍 Récupère les rôles activés depuis la config
    private List<String> getActiveRoles() {
        return plugin.getConfig().getConfigurationSection("roles").getKeys(false)
                .stream()
                .filter(role -> plugin.getConfig().getBoolean("roles." + role))
                .collect(Collectors.toList());
    }

    public void updateAllScoreboards() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            setPlayerScoreboard(onlinePlayer);
        }
    }
  

}
