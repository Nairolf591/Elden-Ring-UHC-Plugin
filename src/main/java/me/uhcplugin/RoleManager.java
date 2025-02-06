package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class RoleManager implements CommandExecutor {
    private final Main plugin;
    private final Map<String, Camp> roleCamps;
    private final Map<String, String> roleDescriptions;
    private static Map<UUID, String> playerRoles = new HashMap<>();

    public RoleManager(Main plugin) {
        this.plugin = plugin;
        this.roleCamps = new HashMap<>();
        this.roleDescriptions = new HashMap<>();
        this.playerRoles = new HashMap<>();
        loadRolesFromConfig();

        // ✅ Restauration des rôles depuis la config au démarrage
        Bukkit.getLogger().info("[DEBUG]: Tentative de restauration des rôles au démarrage...");
        if (plugin.getConfig().contains("savedRoles")) {
            for (String uuidString : plugin.getConfig().getConfigurationSection("savedRoles").getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(uuidString);
                    String role = plugin.getConfig().getString("savedRoles." + uuidString);
                    playerRoles.put(playerUUID, role);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[DEBUG]: Erreur de conversion d'UUID : " + uuidString);
                }
            }
        }

        Bukkit.getLogger().info("[DEBUG]: Rôles restaurés depuis la config : " + playerRoles);
    }

    private void loadRolesFromConfig() {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("roles")) return;

        // Associer chaque rôle à son camp
        roleCamps.put("Radahn", Camp.DEMI_DIEUX);
        roleCamps.put("Melina", Camp.SOLITAIRES);
        roleCamps.put("Sans-éclat", Camp.SOLITAIRES);
        roleCamps.put("Ranni", Camp.DEMI_DIEUX);
        roleCamps.put("Godrick", Camp.DEMI_DIEUX);
        roleCamps.put("Morgott", Camp.DEMI_DIEUX);
        roleCamps.put("Margit", Camp.DEMI_DIEUX);
        roleCamps.put("D_témoin_de_la_mort", Camp.FLEAUX);

        // Ajouter les descriptions de rôle
        roleDescriptions.put("Radahn", "Tu es le puissant général Radahn, l'un des plus forts Demi-Dieux...");
        roleDescriptions.put("Melina", "Tu es Melina, une guide mystérieuse accompagnant les Sans-Éclats...");
        roleDescriptions.put("Sans-éclat", "Tu es un simple Sans-Éclat, perdu dans l'Entre-terre...");
        roleDescriptions.put("Ranni", "Tu es Ranni, la sorcière mystérieuse en quête d'un nouvel ordre...");
        roleDescriptions.put("Godrick", "Tu es Godrick le Greffé, avide de puissance et de domination...");
        roleDescriptions.put("Morgott", "Tu es Morgott, le roi des réprouvés, gardien du trône...");
        roleDescriptions.put("Margit", "Tu es Margit, le gardien du Château de Voilorage, impitoyable envers les intrus...");
        roleDescriptions.put("D_témoin_de_la_mort", "Tu es D, un chasseur de morts aux motivations mystérieuses...");
    }

    public void assignRoles() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<String> availableRoles = new ArrayList<>();

        Bukkit.getLogger().info("[DEBUG] Liste des rôles et leur camp : " + roleCamps);

        for (String role : roleCamps.keySet()) {
            if (plugin.getConfig().getBoolean("roles." + role, false)) {
                availableRoles.add(role);
            }
        }

        Collections.shuffle(players);
        Collections.shuffle(availableRoles);

        int roleCount = Math.min(players.size(), availableRoles.size());
        for (int i = 0; i < roleCount; i++) {
            Player player = players.get(i);
            String role = availableRoles.get(i);

            boolean isEnabled = plugin.getConfig().getBoolean("roles." + role, false);
            Bukkit.getLogger().info("[DEBUG] Rôles disponibles après filtrage : " + availableRoles);

            if (!isEnabled) {
                continue;
            }

            playerRoles.put(player.getUniqueId(), role);
            player.sendMessage("§6[UHC] §aTu es " + role + " ! Camp : " + roleCamps.get(role).getDisplayName());
            Bukkit.getLogger().info("[DEBUG] " + player.getName() + " reçoit le rôle " + role);
        }

        // ✅ CORRECTION : Sauvegarde proprement les rôles en convertissant UUID en String
        Map<String, String> savedRolesMap = new HashMap<>();
        for (Map.Entry<UUID, String> entry : playerRoles.entrySet()) {
            savedRolesMap.put(entry.getKey().toString(), entry.getValue());
        }

        plugin.getConfig().set("savedRoles", savedRolesMap);
        plugin.saveConfig();

        Bukkit.getLogger().info("[DEBUG] ✅ Les rôles ont été sauvegardés dans la config.");
    }

    public String getRole(Player player) {
        String role = playerRoles.get(player.getUniqueId());
        if (role == null) {
            Bukkit.getLogger().warning("[DEBUG] Aucune entrée trouvée pour " + player.getName() + " dans playerRoles !");

            // Vérifie si on peut récupérer le rôle depuis la config
            role = plugin.getConfig().getString("savedRoles." + player.getUniqueId().toString());
            if (role != null) {
                Bukkit.getLogger().info("[DEBUG] Rôle restauré pour " + player.getName() + " : " + role);
                playerRoles.put(player.getUniqueId(), role); // Restaure en mémoire
            } else {
                Bukkit.getLogger().warning("[DEBUG] Impossible de restaurer le rôle pour " + player.getName());
            }
        }
        return role;
    }

    public Camp getCamp(Player player) {
        String role = getRole(player);
        if (role == null) {
            Bukkit.getLogger().warning("[DEBUG] " + player.getName() + " n'a pas de rôle !");
            return null;
        }

        Camp camp = roleCamps.get(role);
        if (camp == null) {
            Bukkit.getLogger().warning("[DEBUG] Aucun camp trouvé pour le rôle : " + role);
            return Camp.SOLITAIRES; // Par défaut
        }
        Bukkit.getLogger().info("[DEBUG] Récupération du camp pour " + player.getName() + " -> Role: " + role + " | Camp: " + (camp != null ? camp.getDisplayName() : "NULL"));
        return camp;
    }

    public String getRoleDescription(String role) {
        return roleDescriptions.getOrDefault(role, "Aucune description disponible pour ce rôle.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSeuls les joueurs peuvent exécuter cette commande !");
            return true;
        }

        Player player = (Player) sender;
        String role = getRole(player);
        Camp camp = getCamp(player);
        if (camp == null) {
            Bukkit.getLogger().warning("[DEBUG] Le joueur " + player.getName() + " n'a pas de camp !");
            player.sendMessage("§6[UHC] §cErreur : Ton rôle ne semble pas avoir de camp associé !");
            return true;
        }
        String description = getRoleDescription(role);

        player.sendMessage("§6[UHC] §aTu es " + role + " !");
        player.sendMessage("§6[UHC] §aTu fais partie du camp " + camp.getDisplayName() + " !");
        player.sendMessage("§6[UHC] §e" + description);

        return true;
    }

    public static Map<UUID, String> getPlayerRoles() {
        return playerRoles;
    }

}
