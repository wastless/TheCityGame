import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface GameInterface extends Remote {
    boolean connectPlayer(String playerName) throws RemoteException;
    List<String> getAvailableGames() throws RemoteException;
    String createNewGame() throws RemoteException;
    boolean joinGame(String gameId, String playerName) throws RemoteException;
    boolean submitCity(String gameId, String playerName, String city) throws RemoteException;
    GameState getGameState(String gameId) throws RemoteException;
    void disconnectPlayer(String playerName) throws RemoteException;
    void checkPlayerTimeout(String gameId, String playerName) throws RemoteException;
    void clearPlayerState(String playerName) throws RemoteException;
    boolean startGame(String gameId) throws RemoteException;
    boolean passTurn(String gameId) throws RemoteException;
} 