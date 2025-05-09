import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class GameClient {
    private GameInterface gameService;
    
    public GameClient() {
        try {
            // Настраиваем свойства для подключения к удаленному серверу
            System.setProperty("java.rmi.server.useLocalHostname", "false");
            
            // Подключаемся к серверу на Render
            Registry registry = LocateRegistry.getRegistry("thecitygame.onrender.com", 1099);
            gameService = (GameInterface) registry.lookup("GameService");
            System.out.println("Подключено к серверу");
        } catch (Exception e) {
            System.err.println("Ошибка подключения к серверу: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public GameInterface getGameService() {
        return gameService;
    }
    
    public static void main(String[] args) {
        try {
            GameClient client = new GameClient();
            GameInterface service = client.getGameService();
            
            // Пример использования
            String playerName = "TestPlayer";
            boolean connected = service.connectPlayer(playerName);
            if (connected) {
                System.out.println("Успешно подключились к игре");
                // Здесь можно добавить дополнительную логику
            } else {
                System.out.println("Не удалось подключиться к игре");
            }
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 