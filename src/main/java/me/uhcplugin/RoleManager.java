// src/main/java/me/uhcplugin/RoleManager.java
package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.*;

public class RoleManager {
    private final Main plugin;
    private final Map<String, Boolean> roleStatus;
    private final List<String> availableRoles;
    private final Map<UUID, String> playerRoles;

    public RoleManager(Main plugin) {
        this.plugin = plugin;
        this.roleStatus = new HashMap<>();
        this.availableRoles = new ArrayList<>();
        this.playerRoles = new HashMap<>();
        loadRolesFromConfig();
    }

    private void loadRolesFromConfig() {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("roles")) return;

        for (String role : config.getConfigurationSection("roles").getKeys(false)) {
            boolean isEnabled = config.getBoolean("roles." + role);
            roleStatus.put(role, isEnabled);
            if (isEnabled) {
                availableRoles.add(role);
            }
        }
    }

    public void assignRoles() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);
        Collections.shuffle(availableRoles);

        int roleCount = Math.min(players.size(), availableRoles.size());
        for (int i = 0; i < roleCount; i++) {
            Player player = players.get(i);
            String role = availableRoles.get(i);
            playerRoles.put(player.getUniqueId(), role);
            player.sendMessage("§6[UHC] §aTu es " + role + " !");
        }
    }

    public String getRole(Player player) {
        return playerRoles.getOrDefault(player.getUniqueId(), "Sans rôle");
    }
    public void saveRolesToConfig() {
    FileConfiguration config = plugin.getConfig();
    for (Map.Entry<String, Boolean> entry : roleStatus.entrySet()) {
        config.set("roles." + entry.getKey(), entry.getValue());
    }
    plugin.saveConfig();
}
}
