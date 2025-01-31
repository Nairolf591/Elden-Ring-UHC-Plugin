package me.uhcplugin;

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
