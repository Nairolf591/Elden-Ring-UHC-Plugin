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
    private String lastTimerValue = "0s"; // Stocke la dernière valeur du timer

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

    // 🔄 Met à jour le scoreboard d'un joueur (chaque joueur a un scoreboard unique)
    public void setPlayerScoreboard(Player player) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            Bukkit.getLogger().warning("[UHCPlugin] Impossible d'obtenir le ScoreboardManager !");
            return;
        }

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("uhc", "dummy", ChatColor.GOLD + "⚔ UHC Elden Ring");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 🔹 Nombre de joueurs
        objective.getScore(ChatColor.AQUA + "👥 Joueurs : " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size()).setScore(6);

        // 🔹 État de la partie
        objective.getScore(ChatColor.RED + "⚔ État : " + ChatColor.WHITE + GameManager.getGameState()).setScore(5);

        // 🔹 Host
        objective.getScore(ChatColor.LIGHT_PURPLE + "👑 Host : " + ChatColor.WHITE + "Flobill").setScore(4);

        if (GameManager.getGameState() == GameManager.GameState.STARTING) {
            // 🔹 Timer d'attribution des rôles
            int timeLeft = plugin.getConfig().getInt("role-announcement-delay", 15);
            updateRoleTimer(timeLeft);
        } else if (GameManager.getGameState() == GameManager.GameState.PLAYING) {
            // 🔹 Rôle du joueur
            String role = plugin.getRoleManager().getRole(player);
            role = (role != null) ? role : ChatColor.GRAY + "Aucun rôle";
            objective.getScore(ChatColor.YELLOW + "🎭 Ton rôle : " + ChatColor.WHITE + role).setScore(3);

            // 🔹 Camp du joueur
            Camp camp = plugin.getRoleManager().getCamp(player);
            String campName = (camp != null) ? camp.getDisplayName() : ChatColor.GRAY + "Aucun camp";

            // 🚀 Correction pour assurer que chaque joueur voit son propre camp
            Bukkit.getLogger().info("[DEBUG] Scoreboard - Mise à jour du camp pour " + player.getName() + " -> " + campName);
            objective.getScore(ChatColor.GOLD + "🏹 Camp : " + ChatColor.WHITE + campName).setScore(2);
        }

        // 🔹 Rôles activés
        List<String> activeRoles = getActiveRoles();
        if (!activeRoles.isEmpty()) {
            objective.getScore(ChatColor.GOLD + "🎭 Rôles activés :").setScore(1);
            int score = 0;
            for (String role : activeRoles) {
                objective.getScore(ChatColor.WHITE + "• " + role).setScore(-score);
                score++;
            }
        }

        // ✅ Applique le scoreboard unique au joueur
        player.setScoreboard(scoreboard);
    }

    // 🔄 Met à jour les scores dynamiques (joueurs, état du jeu, rôles activés)
    public void updateScoreboard(Player player) {
        if (objective == null) return;

        // 🔹 Réinitialise les anciens scores
        scoreboard.getEntries().forEach(scoreboard::resetScores);

        // 📌 Nombre de joueurs
        objective.getScore(ChatColor.AQUA + "👥 Joueurs : " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size()).setScore(6);

        // 📌 État de la partie
        objective.getScore(ChatColor.RED + "⚔ État : " + ChatColor.WHITE + GameManager.getGameState()).setScore(5);

        // 📌 Host (fixé à Flobill pour l'instant)
        objective.getScore(ChatColor.LIGHT_PURPLE + "👑 Host : " + ChatColor.WHITE + "Flobill").setScore(4);

        // 🔄 Gestion de l'affichage en fonction de l'état du jeu
        if (GameManager.getGameState() == GameManager.GameState.STARTING) {
            // 📌 Affichage du timer d'attribution des rôles
            int timeLeft = plugin.getConfig().getInt("role-announcement-delay", 15);
            setRoleTimer(timeLeft);
        } else if (GameManager.getGameState() == GameManager.GameState.PLAYING) {

            // 🔹 Récupération du rôle du joueur
            String role = plugin.getRoleManager().getRole(player);
            role = (role != null) ? role : ChatColor.GRAY + "Aucun rôle";
            objective.getScore(ChatColor.YELLOW + "🎭 Ton rôle : " + ChatColor.WHITE + role).setScore(3);

            // 🔹 Récupération du camp du joueur (🔄 Nouvelle correction)
            Camp camp = plugin.getRoleManager().getCamp(player);
            String campName = (camp != null) ? camp.getDisplayName() : ChatColor.GRAY + "Aucun camp";

            // 🚀 Correction pour forcer la mise à jour du scoreboard après attribution des rôles
            Bukkit.getLogger().info("[DEBUG] Scoreboard - Mise à jour du camp pour " + player.getName() + " -> " + campName);
            objective.getScore(ChatColor.GOLD + "🏹 Camp : " + ChatColor.WHITE + campName).setScore(2);
        }

        // 📌 Rôles activés
        List<String> activeRoles = getActiveRoles();
        if (!activeRoles.isEmpty()) {
            objective.getScore(ChatColor.GOLD + "🎭 Rôles activés :").setScore(1);
            int score = 0;
            for (String role : activeRoles) {
                objective.getScore(ChatColor.WHITE + "• " + role).setScore(-score);
                score++;
            }
        }
    }

    // 🔄 Met à jour le timer d'attribution des rôles
    public void setRoleTimer(int secondsLeft) {
        if (objective == null) return;

        // 🔄 Supprime uniquement l'ancien timer pour éviter l'empilement
        scoreboard.resetScores(lastTimerValue);

        // 📌 Réaffichage des infos principales pour éviter qu'elles disparaissent
        objective.getScore(ChatColor.AQUA + "👥 Joueurs : " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size()).setScore(6);
        objective.getScore(ChatColor.RED + "⚔ État : " + ChatColor.WHITE + GameManager.getGameState()).setScore(5);
        objective.getScore(ChatColor.LIGHT_PURPLE + "👑 Host : " + ChatColor.WHITE + "Flobill").setScore(4);

        // Supprime l'ancienne ligne de timer pour éviter les doublons
        if (!lastTimerValue.isEmpty()) {
            scoreboard.resetScores(lastTimerValue);
        }

// 📌 Stocke et affiche **le timer sur UNE SEULE LIGNE**
        lastTimerValue = ChatColor.LIGHT_PURPLE + "🎭 Attribution des rôles dans " + ChatColor.WHITE + secondsLeft + "s";
        objective.getScore(lastTimerValue).setScore(3);

    }

    // 🔍 Récupère les rôles activés depuis la config
    private List<String> getActiveRoles() {
        return plugin.getConfig().getConfigurationSection("roles").getKeys(false)
                .stream()
                .filter(role -> plugin.getConfig().getBoolean("roles." + role))
                .collect(Collectors.toList());
    }

    // 🔄 Met à jour tous les scoreboards
    public void updateAllScoreboards() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            setPlayerScoreboard(onlinePlayer);
        }
    }

    // 📌 Fonction pour récupérer la dernière valeur stockée du timer
    private String getLastTimerValue() {
        return lastTimerValue;
    }

    public void updateRoleTimer(int secondsLeft) {
        if (objective == null) return;

        // 🔄 Supprime l'ancien timer pour éviter qu'il ne s'affiche en double
        if (!lastTimerValue.isEmpty()) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                Scoreboard sb = onlinePlayer.getScoreboard();
                sb.resetScores(lastTimerValue);
            }
        }

        // ✅ Stocke et affiche le timer sur **une seule ligne**
        lastTimerValue = ChatColor.LIGHT_PURPLE + "🎭 Attribution des rôles dans " + ChatColor.WHITE + secondsLeft + "s";

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Scoreboard sb = onlinePlayer.getScoreboard();
            Objective obj = sb.getObjective("uhc");
            if (obj != null) {
                obj.getScore(lastTimerValue).setScore(3);
            }
        }
    }
}
