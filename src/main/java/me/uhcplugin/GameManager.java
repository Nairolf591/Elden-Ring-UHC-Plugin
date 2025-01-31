package me.uhcplugin;

public class GameManager {

    public enum GameState {
        WAITING,
        STARTING,
        PLAYING,
        ENDED
    }

    private static GameState currentState = GameState.WAITING;

    public static GameState getGameState() {
        return currentState;
    }

    public static void setGameState(GameState state) {
        currentState = state;
    }
}


