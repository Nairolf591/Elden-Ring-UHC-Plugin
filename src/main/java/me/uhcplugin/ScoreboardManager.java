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
        objective.getScore(ChatColor.AQUA + "👥 Joueurs : " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size()).setScore(8);

        // 🔹 État de la partie
        objective.getScore(ChatColor.RED + "⚔ État : " + ChatColor.WHITE + GameManager.getGameState()).setScore(7);

        // 🔹 Host
        objective.getScore(ChatColor.LIGHT_PURPLE + "👑 Host : " + ChatColor.WHITE + "Flobill").setScore(6);

        if (GameManager.getGameState() == GameManager.GameState.PLAYING) {
            // 🔹 Afficher le mana
            int mana = plugin.getManaManager().getMana(player);
            int maxMana = plugin.getManaManager().getMaxMana(player); // 🔹 Récupère le mana max du joueur
            objective.getScore(ChatColor.BLUE + "✦ Mana :").setScore(5);
            objective.getScore(ChatColor.WHITE.toString() + mana + " / " + maxMana).setScore(4); // ✅ Affiche le maxMana correct
        }

        if (GameManager.getGameState() == GameManager.GameState.STARTING) {
            // 🔹 Timer d'attribution des rôles
            int timeLeft = plugin.getConfig().getInt("role-announcement-delay", 15);
            updateRoleTimer(timeLeft);
        } else if (GameManager.getGameState() == GameManager.GameState.PLAYING) {
            // 🔹 Rôle du joueur
            String role = plugin.getRoleManager().getRole(player);
            role = (role != null) ? role : ChatColor.GRAY + "Aucun rôle";
            objective.getScore(ChatColor.YELLOW + "🎭 Ton rôle :").setScore(3);
            objective.getScore(ChatColor.WHITE + role).setScore(2);

            // 🔹 Camp du joueur
            Camp camp = plugin.getRoleManager().getCamp(player);
            String campName = (camp != null) ? camp.getDisplayName() : ChatColor.GRAY + "Aucun camp";

            // 🏹 Affichage du camp (sur deux lignes)
            objective.getScore(ChatColor.GOLD + "🏹 Camp :").setScore(1);
            objective.getScore(ChatColor.WHITE + campName).setScore(0);
        }

        // 🔹 Rôles activés
        List<String> activeRoles = getActiveRoles();
        if (!activeRoles.isEmpty()) {
            objective.getScore(ChatColor.GOLD + "🎭 Rôles activés :").setScore(-1);
            int roleScore = -2; // Commence en négatif pour ne pas chevaucher les autres lignes
            for (String activeRole : activeRoles) {
                objective.getScore(ChatColor.WHITE + "• " + activeRole).setScore(roleScore);
                roleScore--;
            }
        }

        // ✅ Applique le scoreboard unique au joueur
        player.setScoreboard(scoreboard);
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