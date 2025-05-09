import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    private String gameId;
    private List<String> players;
    private List<String> usedCities;
    private String currentPlayer;
    private String lastCity;
    private boolean gameOver;
    private String winner;
    private boolean gameStarted;

    public GameState(String gameId) {
        this.gameId = gameId;
        this.players = new ArrayList<>();
        this.usedCities = new ArrayList<>();
        this.gameOver = false;
        this.gameStarted = false;
        this.currentPlayer = null;
        this.lastCity = null;
        this.winner = null;
    }

    // Геттеры и сеттеры
    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
    }

    public List<String> getUsedCities() {
        return usedCities;
    }

    public void setUsedCities(List<String> usedCities) {
        this.usedCities = usedCities;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(String currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public String getLastCity() {
        return lastCity;
    }

    public void setLastCity(String lastCity) {
        this.lastCity = lastCity;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public void addUsedCity(String city) {
        this.usedCities.add(city);
    }
} 