

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameService extends UnicastRemoteObject implements GameInterface {
    private static final long serialVersionUID = 1L;
    private final Map<String, String> playerGameMap;
    private final Map<String, GameState> games;
    private final Random random;
    private static final int MIN_PLAYERS = 3;
    private static final int MAX_PLAYERS = 5;
    private static final int TURN_TIMEOUT = 30; // секунды

    public GameService() throws RemoteException {
        super();
        this.playerGameMap = new ConcurrentHashMap<>();
        this.games = new ConcurrentHashMap<>();
        this.random = new Random();
        System.out.println("GameService инициализирован");
    }

    @Override
    public boolean connectPlayer(String playerName) throws RemoteException {
        System.out.println("Попытка подключения игрока: " + playerName);
        if (playerName == null || playerName.trim().isEmpty()) {
            System.out.println("Имя игрока не может быть пустым");
            return false;
        }
        
        if (playerGameMap.containsKey(playerName)) {
            System.out.println("Игрок " + playerName + " уже существует");
            return false;
        }
        
        try {
            playerGameMap.put(playerName, "");
            System.out.println("Игрок " + playerName + " успешно подключен");
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка при подключении игрока: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> getAvailableGames() throws RemoteException {
        System.out.println("Запрос списка доступных игр");
        List<String> availableGames = new ArrayList<>();
        try {
            for (Map.Entry<String, GameState> entry : games.entrySet()) {
                GameState game = entry.getValue();
                if (game != null && game.getPlayers() != null && 
                    game.getPlayers().size() < MAX_PLAYERS && !game.isGameOver()) {
                    availableGames.add(game.getGameId());
                }
            }
            System.out.println("Найдено доступных игр: " + availableGames.size());
            return availableGames;
        } catch (Exception e) {
            System.err.println("Ошибка при получении списка игр: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public String createNewGame() throws RemoteException {
        System.out.println("Создание новой игры");
        try {
            String gameId = UUID.randomUUID().toString();
            GameState gameState = new GameState(gameId);
            gameState.setPlayers(new ArrayList<>());
            gameState.setUsedCities(new ArrayList<>());
            gameState.setCurrentPlayer(null);
            games.put(gameId, gameState);
            System.out.println("Создана новая игра с ID: " + gameId);
            return gameId;
        } catch (Exception e) {
            System.err.println("Ошибка при создании игры: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Не удалось создать игру", e);
        }
    }

    @Override
    public boolean joinGame(String gameId, String playerName) throws RemoteException {
        System.out.println("Попытка присоединения игрока " + playerName + " к игре " + gameId);
        try {
            if (gameId == null || playerName == null) {
                System.out.println("ID игры или имя игрока не могут быть null");
                return false;
            }

            GameState game = games.get(gameId);
            if (game == null) {
                System.out.println("Игра не найдена");
                return false;
            }
            if (game.isGameOver()) {
                System.out.println("Игра уже завершена");
                return false;
            }
            if (game.isGameStarted()) {
                System.out.println("Игра уже начата");
                return false;
            }
            if (game.getPlayers() == null) {
                game.setPlayers(new ArrayList<>());
            }
            if (game.getPlayers().size() >= MAX_PLAYERS) {
                System.out.println("Игра уже заполнена");
                return false;
            }
            
            String currentGame = playerGameMap.get(playerName);
            if (currentGame == null || !currentGame.isEmpty()) {
                System.out.println("Игрок не может присоединиться к игре");
                return false;
            }
            
            game.getPlayers().add(playerName);
            playerGameMap.put(playerName, gameId);
            System.out.println("Игрок " + playerName + " присоединился к игре");
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка при присоединении к игре: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean submitCity(String gameId, String playerName, String city) throws RemoteException {
        try {
            GameState game = games.get(gameId);
            if (game == null) {
                System.out.println("Игра не найдена: " + gameId);
                return false;
            }

            if (game.isGameOver()) {
                System.out.println("Игра уже завершена");
                return false;
            }

            if (!game.getCurrentPlayer().equals(playerName)) {
                System.out.println("Не ваш ход: " + playerName);
                return false;
            }

            String lastCity = game.getLastCity();
            if (!CityDatabase.isValidNextCity(lastCity, city)) {
                System.out.println("Неверный город: " + city + " (после " + lastCity + ")");
                return false;
            }

            if (game.getUsedCities().contains(city)) {
                System.out.println("Город уже использован: " + city);
                return false;
            }

            game.addUsedCity(city);
            game.setLastCity(city);
            
            // Проверяем, не остался ли только один игрок
            if (game.getPlayers().size() == 1) {
                game.setGameOver(true);
                game.setWinner(game.getPlayers().get(0));
                System.out.println("Игра завершена - победитель: " + game.getWinner());
            } else {
                game.setCurrentPlayer(getNextPlayer(game));
            }
            
            System.out.println("Город принят: " + city + " от " + playerName);
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка при отправке города: " + e.getMessage());
            return false;
        }
    }

    @Override
    public GameState getGameState(String gameId) throws RemoteException {
        try {
            GameState game = games.get(gameId);
            if (game == null) {
                System.out.println("Игра не найдена: " + gameId);
                return null;
            }
            // Создаем копию состояния игры для безопасной передачи
            GameState gameStateCopy = new GameState(game.getGameId());
            gameStateCopy.setPlayers(new ArrayList<>(game.getPlayers()));
            gameStateCopy.setUsedCities(new ArrayList<>(game.getUsedCities()));
            gameStateCopy.setCurrentPlayer(game.getCurrentPlayer());
            gameStateCopy.setLastCity(game.getLastCity());
            gameStateCopy.setGameOver(game.isGameOver());
            gameStateCopy.setWinner(game.getWinner());
            gameStateCopy.setGameStarted(game.isGameStarted());
            return gameStateCopy;
        } catch (Exception e) {
            System.err.println("Ошибка при получении состояния игры: " + e.getMessage());
            throw new RemoteException("Ошибка при получении состояния игры", e);
        }
    }

    @Override
    public void disconnectPlayer(String playerName) throws RemoteException {
        System.out.println("Отключение игрока: " + playerName);
        String gameId = playerGameMap.remove(playerName);
        if (gameId != null) {
            GameState game = games.get(gameId);
            if (game != null) {
                game.getPlayers().remove(playerName);
                
                // Проверяем, остался ли только один игрок
                if (game.getPlayers().size() == 1) {
                    game.setGameOver(true);
                    game.setWinner(game.getPlayers().get(0));
                    System.out.println("Игра завершена - победитель: " + game.getWinner());
                } else if (game.getPlayers().isEmpty()) {
                    // Если не осталось игроков, завершаем игру без победителя
                    game.setGameOver(true);
                    game.setWinner(null);
                    System.out.println("Игра завершена - не осталось игроков");
                } else {
                    // Если текущий игрок вышел, передаем ход следующему
                    if (game.getCurrentPlayer() != null && game.getCurrentPlayer().equals(playerName)) {
                        String nextPlayer = getNextPlayer(game);
                        game.setCurrentPlayer(nextPlayer);
                        System.out.println("Ход передан игроку: " + nextPlayer);
                    }
                }
            }
        }
        System.out.println("Игрок " + playerName + " отключен");
    }

    @Override
    public void clearPlayerState(String playerName) throws RemoteException {
        System.out.println("Очистка состояния игрока: " + playerName);
        if (playerName != null) {
            playerGameMap.remove(playerName);
            System.out.println("Состояние игрока " + playerName + " очищено");
        }
    }

    @Override
    public void checkPlayerTimeout(String gameId, String playerName) throws RemoteException {
        System.out.println("Проверка таймаута для игрока " + playerName + " в игре " + gameId);
        try {
            GameState game = games.get(gameId);
            if (game == null) {
                System.out.println("Игра не найдена: " + gameId);
                return;
            }

            if (game.isGameOver()) {
                System.out.println("Игра уже завершена");
                return;
            }

            if (!game.getCurrentPlayer().equals(playerName)) {
                System.out.println("Не ход игрока: " + playerName);
                return;
            }

            // Удаляем игрока из игры
            game.getPlayers().remove(playerName);
            playerGameMap.remove(playerName);
            System.out.println("Игрок " + playerName + " выбыл из игры по таймауту");

            // Проверяем, остались ли игроки
            if (game.getPlayers().isEmpty()) {
                game.setGameOver(true);
                System.out.println("Игра завершена - не осталось игроков");
                return;
            }

            // Если остался один игрок - он победитель
            if (game.getPlayers().size() == 1) {
                game.setGameOver(true);
                game.setWinner(game.getPlayers().get(0));
                System.out.println("Игра завершена - победитель: " + game.getWinner());
                return;
            }

            // Передаем ход следующему игроку
            String nextPlayer = getNextPlayer(game);
            game.setCurrentPlayer(nextPlayer);
            System.out.println("Ход передан игроку: " + nextPlayer);
        } catch (Exception e) {
            System.err.println("Ошибка при проверке таймаута: " + e.getMessage());
            throw new RemoteException("Ошибка при проверке таймаута", e);
        }
    }

    @Override
    public boolean startGame(String gameId) throws RemoteException {
        System.out.println("Попытка начать игру: " + gameId);
        try {
            GameState game = games.get(gameId);
            if (game == null) {
                System.out.println("Игра не найдена");
                return false;
            }
            if (game.isGameStarted()) {
                System.out.println("Игра уже начата");
                return false;
            }
            if (game.getPlayers().size() < MIN_PLAYERS) {
                System.out.println("Недостаточно игроков");
                return false;
            }
            
            game.setGameStarted(true);
            int firstPlayerIndex = random.nextInt(game.getPlayers().size());
            game.setCurrentPlayer(game.getPlayers().get(firstPlayerIndex));
            System.out.println("Игра началась! Первый ход: " + game.getCurrentPlayer());
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка при начале игры: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean passTurn(String gameId) throws RemoteException {
        try {
            GameState state = games.get(gameId);
            if (state == null) {
                System.out.println("Игра не найдена: " + gameId);
                return false;
            }

            if (state.isGameOver()) {
                System.out.println("Игра уже завершена");
                return false;
            }

            List<String> players = state.getPlayers();
            if (players == null || players.isEmpty()) {
                System.out.println("Нет активных игроков");
                return false;
            }

            String currentPlayer = state.getCurrentPlayer();
            int currentIndex = players.indexOf(currentPlayer);
            if (currentIndex == -1) {
                System.out.println("Текущий игрок не найден в списке");
                return false;
            }

            // Находим следующего активного игрока
            int nextIndex = (currentIndex + 1) % players.size();
            String nextPlayer = players.get(nextIndex);

            // Обновляем состояние игры
            state.setCurrentPlayer(nextPlayer);
            System.out.println("Ход передан игроку: " + nextPlayer);
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка при передаче хода: " + e.getMessage());
            return false;
        }
    }

    private String getNextPlayer(GameState game) {
        List<String> players = game.getPlayers();
        if (players.isEmpty()) {
            return null;
        }
        int currentIndex = players.indexOf(game.getCurrentPlayer());
        int nextIndex = (currentIndex + 1) % players.size();
        return players.get(nextIndex);
    }

    public static void main(String[] args) {
        try {
            String hostAddress = System.getenv("RENDER_EXTERNAL_HOSTNAME");
            if (hostAddress == null) {
                hostAddress = "localhost";
            }
            
            int port = 1099;
            String portStr = System.getenv("PORT");
            if (portStr != null) {
                port = Integer.parseInt(portStr);
            }
            
            // Устанавливаем системное свойство для RMI
            System.setProperty("java.rmi.server.hostname", hostAddress);
            
            GameService gameService = new GameService();
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("GameService", gameService);
            System.out.println("Сервер запущен на " + hostAddress + ":" + port);
        } catch (Exception e) {
            System.err.println("Ошибка запуска сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
