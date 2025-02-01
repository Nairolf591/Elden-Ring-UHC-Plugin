package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

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

        if (state == GameState.ENDED) {
            Bukkit.broadcastMessage(ChatColor.RED + "🏁 La partie est terminée !");

            // ⏳ Petite pause avant de reset la map (attend 10 secondes)
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Main.getInstance().resetUHCWorld();
            }, 200L); // 200 ticks = 10 secondes
        }
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


}
