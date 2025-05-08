import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface GameInterface extends Remote {
    // Метод для подключения игрока
    boolean connectPlayer(String playerName) throws RemoteException;
    
    // Метод для получения списка доступных игр
    List<String> getAvailableGames() throws RemoteException;
    
    // Метод для создания новой игры
    String createNewGame() throws RemoteException;
    
    // Метод для присоединения к существующей игре
    boolean joinGame(String gameId, String playerName) throws RemoteException;
    
    // Метод для отправки названия города
    boolean submitCity(String gameId, String playerName, String city) throws RemoteException;
    
    // Метод для получения текущего состояния игры
    GameState getGameState(String gameId) throws RemoteException;
    
    // Метод для отключения игрока
    void disconnectPlayer(String playerName) throws RemoteException;
    
    // Метод для проверки и выбывания игрока по таймауту
    void checkPlayerTimeout(String gameId, String playerName) throws RemoteException;
    
    // Метод для очистки состояния игрока
    void clearPlayerState(String playerName) throws RemoteException;
    
    // Метод для начала игры
    boolean startGame(String gameId) throws RemoteException;
    
    // Метод для передачи хода следующему игроку
    boolean passTurn(String gameId) throws RemoteException;
} 