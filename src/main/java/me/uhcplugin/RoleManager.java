package me.uhcplugin;

import me.uhcplugin.roles.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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

        Bukkit.getLogger().info("[DEBUG]: Tentative de restauration des rôles au démarrage...");

        // 🔄 Correction ici : Vérifier si la section existe ET n'est pas null
        ConfigurationSection savedRolesSection = plugin.getConfig().getConfigurationSection("savedRoles");
        if (savedRolesSection != null) { // ✅ Évite le NPE
            for (String uuidString : savedRolesSection.getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(uuidString);
                    String role = savedRolesSection.getString(uuidString);
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
        roleCamps.put("Melina", Camp.TABLE_RONDE);
        roleCamps.put("Sans-éclat", Camp.TABLE_RONDE);
        roleCamps.put("Ranni", Camp.SOLITAIRES);
        roleCamps.put("Godrick", Camp.DEMI_DIEUX);
        roleCamps.put("Morgott", Camp.DEMI_DIEUX);
        roleCamps.put("Margit", Camp.DEMI_DIEUX);
        roleCamps.put("D_témoin_de_la_mort", Camp.TABLE_RONDE);
        roleCamps.put("Maliketh", Camp.DEMI_DIEUX);
        roleCamps.put("Masque d'Or", Camp.TABLE_RONDE);
        roleCamps.put("Alexandre", Camp.TABLE_RONDE);
        roleCamps.put("Jar Bairn", Camp.TABLE_RONDE);


        // Ajouter les descriptions de rôle
        roleDescriptions.put("Radahn", "Tu es le puissant général Radahn, l'un des plus forts Demi-Dieux...");
        roleDescriptions.put("Melina", "Tu es Melina, une guide mystérieuse accompagnant les Sans-Éclats...");
        roleDescriptions.put("Sans-éclat", "Tu es un simple Sans-Éclat, perdu dans l'Entre-terre...");
        roleDescriptions.put("Ranni", "Tu es Ranni, la sorcière mystérieuse en quête d'un nouvel ordre...");
        roleDescriptions.put("Godrick", "Tu es Godrick le Greffé, avide de puissance et de domination...");
        roleDescriptions.put("Morgott", "Tu es Morgott, le roi des réprouvés, gardien du trône...");
        roleDescriptions.put("Margit", "Tu es Margit, le gardien du Château de Voilorage, impitoyable envers les intrus...");
        roleDescriptions.put("D_témoin_de_la_mort", "Tu es D, un chasseur de morts aux motivations mystérieuses...");
        roleDescriptions.put("Maliketh", "Tu es Maliketh...");
        roleDescriptions.put("Masque d'Or", "Tu es Masque d'or...");
        roleDescriptions.put("Alexandre", "Tu es Alexandre, le guerrier légendaire protégé par une carapace de fer...");
        roleDescriptions.put("Jar Bairn", "Tu es Jar Bairn...");

    }

    public void assignRoles() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<String> availableRoles = new ArrayList<>();

        // Récupère les rôles activés dans la config
        for (String role : roleCamps.keySet()) {
            if (plugin.getConfig().getBoolean("roles." + role, false)) {
                availableRoles.add(role);
            }
        }

        // Mélange les joueurs et les rôles
        Collections.shuffle(players);
        Collections.shuffle(availableRoles);

        // Assigne les rôles aux joueurs
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (i < availableRoles.size()) {
                String role = availableRoles.get(i);
                playerRoles.put(player.getUniqueId(), role);
                player.sendMessage("§6[UHC] §aTu es " + role + " ! Camp : " + roleCamps.get(role).getDisplayName());
                plugin.getManaManager().assignManaBasedOnRole(player);
                plugin.getScoreboardManager().setPlayerScoreboard(player);

                // Donne les artefacts spécifiques aux rôles
                if (role.equalsIgnoreCase("Ranni")) {
                    plugin.getRanniRole().giveArtifactToRanni(player);
                } else if (role.equalsIgnoreCase("Melina")) {
                    plugin.getMelinaRole().giveArtifactToMelina(player);
                } else if (role.equalsIgnoreCase("Maliketh")) {
                    plugin.getMalikethRole().giveMalikethItems(player);
                } else if (role.equalsIgnoreCase("Sans-éclat")) {
                    player.getInventory().addItem(SansEclatRole.getSansEclatItems());
                } else if (role.equalsIgnoreCase("Masque d'Or")) {
                    player.getInventory().addItem(MasqueDOrRole.getMasqueDOrItems());
                } else if (role.equalsIgnoreCase("Alexandre")) {
                    player.getInventory().addItem(AlexandreRole.getAlexandreItems());
                } else if (role.equalsIgnoreCase("Jar Bairn")) {
                    //Pas d'items
                }
            } else {
                // Si aucun rôle n'est disponible, attribue un rôle par défaut
                playerRoles.put(player.getUniqueId(), "Defaut");
                player.sendMessage("§6[UHC] §aTu es defaut ! Camp : " + Camp.SOLITAIRES.getDisplayName());
            }
        }

        // Sauvegarde les rôles dans la config
        Map<String, String> savedRolesMap = new HashMap<>();
        for (Map.Entry<UUID, String> entry : playerRoles.entrySet()) {
            savedRolesMap.put(entry.getKey().toString(), entry.getValue());
        }
        plugin.getConfig().set("savedRoles", savedRolesMap);
        plugin.saveConfig();

        Bukkit.getLogger().info("[DEBUG] Nombre de joueurs en ligne : " + players.size());
        Bukkit.getLogger().info("[DEBUG] Nombre de rôles disponibles : " + availableRoles.size());
    }

    public String getRole(Player player) {
        String role = playerRoles.get(player.getUniqueId());
        if (role == null) {
            Bukkit.getLogger().warning("[DEBUG] Aucune entrée trouvée pour " + player.getName() + " dans playerRoles !");

            // Vérifie si on peut récupérer le rôle depuis la config
            role = plugin.getConfig().getString("savedRoles." + player.getUniqueId().toString());
            if (role != null) {
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
        if (command.getName().equalsIgnoreCase("checkrole")) {
            if (!sender.hasPermission("uhcplugin.checkrole")) {
                sender.sendMessage(ChatColor.RED + "❌ Tu n'as pas la permission d'utiliser cette commande !");
                return true;
            }

            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "❌ Utilisation : /checkrole <joueur>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "❌ Le joueur " + args[0] + " n'est pas en ligne !");
                return true;
            }

            // Récupération du rôle et du camp du joueur ciblé
            String role = getRole(target);
            Camp camp = getCamp(target);

            if (role == null || camp == null) {
                sender.sendMessage(ChatColor.RED + "❌ Le joueur " + target.getName() + " n'a pas encore de rôle !");
                return true;
            }

            // 📌 Affichage des informations du joueur
            sender.sendMessage(ChatColor.GOLD + "📌 Informations sur " + ChatColor.WHITE + target.getName() + " :");
            sender.sendMessage(ChatColor.YELLOW + "🎭 Rôle : " + ChatColor.WHITE + role);
            sender.sendMessage(ChatColor.GOLD + "🏹 Camp : " + ChatColor.WHITE + camp.getDisplayName());

            return true; // ✅ Fin de la commande checkrole
        }

        // 📌 Gestion de la commande /role (exécutée uniquement par les joueurs)
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSeuls les joueurs peuvent exécuter cette commande !");
            return true;
        }

        Player player = (Player) sender;
        String role = getRole(player);
        Camp camp = getCamp(player);

        if (camp == null) {
            player.sendMessage("§6[UHC] §cErreur : Ton rôle ne semble pas avoir de camp associé !");
            return true;
        }

        String description = getRoleDescription(role);
        player.sendMessage("§6[UHC] §aTu es " + role + " !");
        player.sendMessage("§6[UHC] §aTu fais partie du camp " + camp.getDisplayName() + " !");
        player.sendMessage("§6[UHC] §e" + description);

        return true; // ✅ Fin de la commande /role
    }

    public static Map<UUID, String> getPlayerRoles() {
        return playerRoles;
    }

    public void setRole(Player player, String role) {
        playerRoles.put(player.getUniqueId(), role);
    }

    public List<String> getActiveRoles() {
        return new ArrayList<>(roleCamps.keySet()); // Retourne tous les rôles configurés
    }

}