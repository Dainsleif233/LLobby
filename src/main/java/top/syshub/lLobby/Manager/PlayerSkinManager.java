package top.syshub.lLobby.Manager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static top.syshub.lLobby.Hook.BungeeMessage.playerList;
import static top.syshub.lLobby.Hook.BungeeMessage.uuidMap;
import static top.syshub.lLobby.LLobby.config;

public class PlayerSkinManager {

    public record PlayerSkin(String texture, String signature) {}

    private static final Map<String, PlayerSkin> skinCache = new ConcurrentHashMap<>();

    public static PlayerSkin getPlayerSkin(String player) {
        skinCache.keySet().stream()
                .filter(p -> playerList.values().stream()
                        .noneMatch(players -> players.contains(p)))
                .forEach(skinCache::remove);
        if (skinCache.containsKey(player))
            return skinCache.get(player);

        String yggdrasilApi = config.getString("yggdrasil-api", "");
        String skinApi = yggdrasilApi.isEmpty() ?
                "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false" :
                yggdrasilApi + "/sessionserver/session/minecraft/profile/%s?unsigned=false";
        String profileApi = String.format(skinApi, uuidMap.get(player).toString().replace("-", ""));

        Gson gson = new Gson();
        HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(profileApi))
                .build();

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return skinCache.put(player, null);
            String result = response.body();
            JsonObject profile = gson.fromJson(result, JsonObject.class);


            for (JsonElement property : profile.getAsJsonArray("properties"))
                if (property.getAsJsonObject().get("name").getAsString().equals("textures")) {
                    PlayerSkin skin = new PlayerSkin(
                            property.getAsJsonObject().get("value").getAsString(),
                            property.getAsJsonObject().get("signature").getAsString()
                    );
                    skinCache.put(player, skin);
                    return skin;
                }
        } catch (IOException | InterruptedException e) {
            skinCache.put(player, null);
            throw new RuntimeException(e);
        }
        return skinCache.put(player, null);
    }
}
