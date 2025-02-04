package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class CampManager {
    private final Main plugin;
    private final Map<UUID, Camp> playerCamps = new HashMap<>();
    private final RoleManager roleManager;

    public CampManager(Main plugin, RoleManager roleManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
    }

    // 🎭 Définition des camps
    public enum Camp {
        DEMI_DIEUX("Demi-Dieux", ChatColor.GOLD),
        FLEAUX("Fléaux", ChatColor.DARK_RED),
        SOLITAIRES("Solitaires", ChatColor.GRAY);

        private final String displayName;
        private final ChatColor color;

        Camp(String displayName, ChatColor color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return color + displayName;
        }
    }

    // 📌 Assigne un camp aux joueurs selon leur rôle
    public void assignCamp(Player player) {
        RoleManager.Role role = roleManager.getRole(player);
        if (role == null) {
            return;
        }

        Camp camp = switch (role) {
            case RADAHN, MELINA, SANSECLAT -> Camp.SOLITAIRES;
            case MOHG, MALIKETH, RYKARD -> Camp.FLEAUX;
            case RANNI, MORGOTT, GODRICK -> Camp.DEMI_DIEUX;
            default -> Camp.SOLITAIRES;
        };

        playerCamps.put(player.getUniqueId(), camp);
    }

    // 📌 Retourne le camp d'un joueur
    public Camp getPlayerCamp(Player player) {
        return playerCamps.getOrDefault(player.getUniqueId(), Camp.SOLITAIRES);
    }

    // 📌 Vérifie si un camp a gagné
    public void checkVictory() {
        Set<Camp> campsRestants = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerCamps.containsKey(player.getUniqueId())) {
                campsRestants.add(playerCamps.get(player.getUniqueId()));
            }
        }

        if (campsRestants.size() == 1) {
            Camp gagnant = campsRestants.iterator().next();
            Bukkit.broadcastMessage(ChatColor.GREEN + "🏆 Le camp " + gagnant.getDisplayName() + " remporte la partie !");
            plugin.getGameManager().endGame(gagnant);
        }
    }
}