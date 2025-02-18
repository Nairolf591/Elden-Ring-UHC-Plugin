package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class GameManager {

    public enum GameState {
        WAITING,    // En attente des joueurs
        STARTING,   // Décompte avant le début de la partie
        PLAYING,    // Partie en cours
        ENDED       // Partie terminée
    }

    private static GameState currentState = GameState.WAITING;

    // Récupérer l'état actuel du jeu
    public static GameState getGameState() {
        return currentState;
    }

    // Définir un nouvel état pour la partie
    public static void setGameState(GameState state) {
        currentState = state;
        Bukkit.broadcastMessage(ChatColor.RED + "📢 L'état du jeu est maintenant : " + state);

        Main.getInstance().getScoreboardManager().updateAllScoreboards();

        if (state == GameState.STARTING) {
            // Donner le stuff aux joueurs
            for (Player player : Bukkit.getOnlinePlayers()) {
                givePlayerStuff(player);
            }
        }

        if (state == GameState.ENDED) {
            resetPlayerStats(); //reset tout
            Bukkit.broadcastMessage(ChatColor.RED + "🏁 La partie est terminée !");
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Main.getInstance().resetUHCWorld();
                Main.getInstance().getConfig().set("savedRoles", null);
                Main.getInstance().saveConfig();
                RoleManager.getPlayerRoles().clear(); // Vide la map en mémoire
                Bukkit.broadcastMessage(ChatColor.GREEN + "✨ Les rôles ont été réinitialisés !");
            }, 200L);

        }
    }

    private static void givePlayerStuff(Player player) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            FileConfiguration config = Main.getInstance().getConfig();
            for (int i = 0; i < 36; i++) { // Parcours complet de l'inventaire
                if (config.contains("stuff." + i)) {
                    ItemStack item = config.getItemStack("stuff." + i);
                    player.getInventory().setItem(i, item);
                }
            }
        }, 100L); // 100 ticks = 5 secondes (1 tick = 1/20e de seconde)
    }

    // Vérifie si la partie est en attente
    public static boolean isWaiting() {
        return currentState == GameState.WAITING;
    }

    // Vérifie si la partie est en cours
    public static boolean isPlaying() {
        return currentState == GameState.PLAYING;
    }

    // Vérifie si la partie est terminée
    public static boolean isEnded() {
        return currentState == GameState.ENDED;
    }

    // ✅ Fonction pour reset le jeu si nécessaire
    private void resetGame() {
        Bukkit.broadcastMessage(ChatColor.GRAY + "🔄 Réinitialisation du serveur...");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        }

        // 📌 Optionnel : Remettre la bordure de map à sa taille initiale
        Bukkit.getWorld("uhc").getWorldBorder().setSize(500);

        // 📌 Remettre l'état du jeu en attente
        setGameState(GameState.WAITING);
    }

    public static void resetPlayerStats() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Réinitialiser la vie maximale
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            player.setHealth(20.0); // Rétablir la vie actuelle à 20

            // Retirer tous les effets
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }

            // Réinitialiser la vitesse de déplacement
            player.setWalkSpeed(0.2f); // Vitesse par défaut dans Minecraft
            player.getInventory().clear(); // Vide l'inventaire du joueur
        }
    }


}
