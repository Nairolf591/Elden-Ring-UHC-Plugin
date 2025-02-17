package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class ManaManager {
    private final Main plugin;
    private final Map<Player, Integer> manaMap = new HashMap<>();
    private final int MAX_MANA = 100;  // Valeur par défaut max de mana
    private final int REGEN_AMOUNT = 5; // Mana régénéré toutes les X secondes
    private final int REGEN_INTERVAL = 20 * 5; // 5 secondes en ticks (1 sec = 20 ticks)
    private final Map<UUID, Integer> maxManaMap = new HashMap<>();
    private final int DEFAULT_MANA = 100;
    private HashMap<UUID, Integer> playerMana = new HashMap<>(); // Map pour stocker le mana de base des joueurs private HashMap<UUID, Integer> baseMana = new HashMap<>();
    private Map<UUID, Integer> baseMana = new HashMap<>();


    public ManaManager(Main plugin) {
        this.plugin = plugin;
        startManaRegeneration(); // Démarre la régénération automatique
    }

    // ✅ Donner un mana initial
    public void setMana(Player player, int amount) {
        int maxMana = getMaxMana(player); // Récupère la valeur max du joueur
        int newMana = Math.min(amount, maxMana); // Assure que le mana ne dépasse pas le max
        manaMap.put(player, newMana);

    }

    // ✅ Récupérer le mana d’un joueur
    public int getMana(Player player) {
        return manaMap.getOrDefault(player, 0);
    }

    // ✅ Consommer du mana
    private final Set<UUID> manaWarningCooldown = new HashSet<>();

    public boolean consumeMana(Player player, int cost) {
        int currentMana = getMana(player);
        if (currentMana >= cost) {
            setMana(player, currentMana - cost);
            return true;
        }

        // ✅ Vérifie si le joueur est déjà en cooldown pour éviter le double message
        if (manaWarningCooldown.contains(player.getUniqueId())) {
            return false;
        }

        manaWarningCooldown.add(player.getUniqueId());

        // ✅ Attente d'une seconde avant d'afficher le message si le joueur n'a toujours pas de mana
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (getMana(player) < cost) {
                player.sendMessage(ChatColor.RED + "❌ Pas assez de mana !");
            }
            manaWarningCooldown.remove(player.getUniqueId()); // ✅ Retire le cooldown après l'affichage
        }, 20L); // 1 seconde d'attente

        return false;
    }

    // ✅ Régénération automatique du mana
    private void startManaRegeneration() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (manaMap.containsKey(player)) {
                    setMana(player, getMana(player) + REGEN_AMOUNT);
                }
            }
        }, 0L, REGEN_INTERVAL);
    }

    // ✅ Mise à jour du mana dans le scoreboard
    public void updateManaDisplay(Player player) {
        plugin.getScoreboardManager().setPlayerScoreboard(player);
    }

    public void assignManaBasedOnRole(Player player) {
        String role = plugin.getRoleManager().getRole(player);
        int mana = switch (role) {
            case "Radahn" -> 150;
            case "Ranni" -> 120;
            case "Melina" -> 100;
            default -> 50; // Valeur par défaut
        };

        setMaxMana(player, mana); // Définit le maxMana
        setMana(player, mana);  // Assigne correctement le mana initial
        updateManaDisplay(player); // Met à jour l'affichage
    }

    public int getMaxMana(Player player) {
        return maxManaMap.getOrDefault(player.getUniqueId(), 100); // Par défaut 100 si non défini
    }

    public void setMaxMana(Player player, int amount) {
        maxManaMap.put(player.getUniqueId(), amount);
    }

    // Définit le mana de base du joueur et réinitialise son mana courant
    public void setBaseMana(Player player, int newBaseMana) {
        UUID uuid = player.getUniqueId();
        baseMana.put(uuid, newBaseMana);
        playerMana.put(uuid, newBaseMana);
    }
}