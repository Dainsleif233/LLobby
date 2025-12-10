package top.syshub.lLobby.Manager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static top.syshub.lLobby.LLobby.config;
import static top.syshub.lLobby.LLobby.plugin;

public class ServerInfoManager {
    public record ServerInfo(String version, int online, int max) {}

    public static Map<String, ServerInfo> serverInfo = new ConcurrentHashMap<>();

    public static void ping(String server) {
        String api = config.getString("ping-api", "");
        if (api.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Gson gson = new Gson();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(api + server))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) throw new Exception();

                JsonObject json = gson.fromJson(response.body(), JsonObject.class);

                String version = json.get("info").getAsJsonObject().get("version").getAsJsonObject().get("name").getAsString();
                int online = json.get("info").getAsJsonObject().get("players").getAsJsonObject().get("online").getAsInt();
                int max = json.get("info").getAsJsonObject().get("players").getAsJsonObject().get("max").getAsInt();

                serverInfo.put(server, new ServerInfo(version, online, max));
            } catch (Exception ignored) {}
        });
    }
}
