package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import java.util.*;

public class RoleManager implements Listener{
    private final Main plugin;
    private final Map<UUID, Role> playerRoles = new HashMap<>();
    private final List<Role> availableRoles = Arrays.asList(Role.values()); // Liste des rôles possibles

    public RoleManager(Main plugin) {
        this.plugin = plugin;
    }


    // 🎭 Enumération des rôles disponibles
    public enum Role {
        RADAHN("Radahn", ChatColor.RED),
        MELINA("Melina", ChatColor.LIGHT_PURPLE),
        SANSECLAT("Sans-Éclat", ChatColor.GRAY),
        MOHG("Mohg", ChatColor.DARK_RED),
        MALIKETH("Maliketh", ChatColor.BLACK),
        RYKARD("Rykard", ChatColor.GOLD),
        RANNI("Ranni", ChatColor.BLUE),
        MORGOTT("Morgott", ChatColor.DARK_GREEN),
        GODRICK("Godrick", ChatColor.YELLOW);

        private final String displayName;
        private final ChatColor color;

        Role(String displayName, ChatColor color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return color + displayName;
        }
    }

    // 📌 Assigne un rôle aléatoire aux joueurs
    public void assignRoles() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);
        Collections.shuffle(availableRoles);

        for (int i = 0; i < players.size(); i++) {
            if (i < availableRoles.size()) {
                playerRoles.put(players.get(i).getUniqueId(), availableRoles.get(i));
                players.get(i).sendMessage(ChatColor.GOLD + "🎭 Ton rôle est : " + availableRoles.get(i).getDisplayName());
            }
        }
    }

    // 🎭 Retourne le rôle d'un joueur
    public Role getRole(Player player) {
        return playerRoles.getOrDefault(player.getUniqueId(), null);
    }

    // 🎭 Vérifie si un joueur a un rôle donné
    public boolean hasRole(Player player, Role role) {
        return playerRoles.getOrDefault(player.getUniqueId(), null) == role;
    }

    // 🎭 Retourne le rôle sous forme de String
    public String getRoleName(Player player) {
        Role role = getRole(player);
        return role != null ? role.getDisplayName() : ChatColor.GRAY + "Aucun rôle";
    }
}