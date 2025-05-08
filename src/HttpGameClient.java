import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class HttpGameClient {
    private final String serverUrl;
    private final Gson gson;

    public HttpGameClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.gson = new Gson();
    }

    private String sendRequest(String path, Map<String, String> data) throws IOException {
        URL url = new URL(serverUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonData = gson.toJson(data);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonData.getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        }

        return response.toString();
    }

    public boolean connectPlayer(String playerName) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("playerName", playerName);
        String response = sendRequest("/connect", data);
        return Boolean.parseBoolean(response);
    }

    public List<String> getAvailableGames() throws IOException {
        String response = sendRequest("/games", new HashMap<>());
        return gson.fromJson(response, new TypeToken<List<String>>(){}.getType());
    }

    public String createNewGame() throws IOException {
        String response = sendRequest("/game/new", new HashMap<>());
        return gson.fromJson(response, String.class);
    }

    public boolean joinGame(String gameId, String playerName) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("gameId", gameId);
        data.put("playerName", playerName);
        String response = sendRequest("/game/join", data);
        return Boolean.parseBoolean(response);
    }

    public boolean submitCity(String gameId, String playerName, String city) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("gameId", gameId);
        data.put("playerName", playerName);
        data.put("city", city);
        String response = sendRequest("/game/submit", data);
        return Boolean.parseBoolean(response);
    }

    public GameState getGameState(String gameId) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("gameId", gameId);
        String response = sendRequest("/game/state", data);
        return gson.fromJson(response, GameState.class);
    }

    public boolean startGame(String gameId) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("gameId", gameId);
        String response = sendRequest("/game/start", data);
        return Boolean.parseBoolean(response);
    }

    public void disconnectPlayer(String playerName) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("playerName", playerName);
        sendRequest("/player/disconnect", data);
    }

    public void clearPlayerState(String playerName) throws IOException {
        Map<String, String> data = new HashMap<>();
        data.put("playerName", playerName);
        sendRequest("/player/clear", data);
    }
} 