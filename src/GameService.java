import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class GameService {
    private final Map<String, String> playerGameMap;
    private final Map<String, GameState> games;
    private final Random random;
    private static final int MIN_PLAYERS = 3;
    private static final int MAX_PLAYERS = 5;
    private static final int TURN_TIMEOUT = 30; // секунды
    private final Gson gson;

    public GameService() {
        this.playerGameMap = new ConcurrentHashMap<>();
        this.games = new ConcurrentHashMap<>();
        this.random = new Random();
        this.gson = new Gson();
        System.out.println("GameService инициализирован");
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            String response = "";
            int responseCode = 200;
            
            if ("POST".equals(method)) {
                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody());
                Map<String, String> request = gson.fromJson(reader, Map.class);
                
                switch (path) {
                    case "/connect":
                        String playerName = request.get("playerName");
                        response = gson.toJson(connectPlayer(playerName));
                        break;
                    case "/games":
                        response = gson.toJson(getAvailableGames());
                        break;
                    case "/game/new":
                        response = gson.toJson(createNewGame());
                        break;
                    case "/game/join":
                        response = gson.toJson(joinGame(request.get("gameId"), request.get("playerName")));
                        break;
                    case "/game/submit":
                        response = gson.toJson(submitCity(
                            request.get("gameId"),
                            request.get("playerName"),
                            request.get("city")
                        ));
                        break;
                    case "/game/state":
                        response = gson.toJson(getGameState(request.get("gameId")));
                        break;
                    case "/game/start":
                        response = gson.toJson(startGame(request.get("gameId")));
                        break;
                    case "/player/disconnect":
                        disconnectPlayer(request.get("playerName"));
                        response = "{}";
                        break;
                    case "/game/timeout":
                        checkPlayerTimeout(request.get("gameId"), request.get("playerName"));
                        response = "{}";
                        break;
                    case "/game/pass":
                        response = gson.toJson(passTurn(request.get("gameId")));
                        break;
                    default:
                        responseCode = 404;
                        response = "Not Found";
                }
            } else if ("GET".equals(method) && "/".equals(path)) {
                response = "Server is running";
            } else {
                responseCode = 405;
                response = "Method Not Allowed";
            }
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseCode, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (Exception e) {
            String error = gson.toJson(Map.of("error", e.getMessage()));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, error.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(error.getBytes());
            }
        }
    }

    public boolean connectPlayer(String playerName) {
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

    public List<String> getAvailableGames() {
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

    public String createNewGame() {
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
            throw new RuntimeException("Не удалось создать игру", e);
        }
    }

    public boolean joinGame(String gameId, String playerName) {
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

    public boolean submitCity(String gameId, String playerName, String city) {
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

    public GameState getGameState(String gameId) {
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
            throw new RuntimeException("Ошибка при получении состояния игры", e);
        }
    }

    public void disconnectPlayer(String playerName) {
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

    public void clearPlayerState(String playerName) {
        System.out.println("Очистка состояния игрока: " + playerName);
        if (playerName != null) {
            playerGameMap.remove(playerName);
            System.out.println("Состояние игрока " + playerName + " очищено");
        }
    }

    public void checkPlayerTimeout(String gameId, String playerName) {
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
        }
    }

    public boolean startGame(String gameId) {
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

    public boolean passTurn(String gameId) {
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
            
            int port = 8080;
            String portStr = System.getenv("PORT");
            if (portStr != null) {
                port = Integer.parseInt(portStr);
            }
            
            System.out.println("Starting server with host: " + hostAddress + " and port: " + port);
            
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(10));
            
            GameService gameService = new GameService();
            
            server.createContext("/", gameService::handleRequest);
            
            server.start();
            System.out.println("HTTP Server is running on " + hostAddress + ":" + port);
            
        } catch (Exception e) {
            System.err.println("Server startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
