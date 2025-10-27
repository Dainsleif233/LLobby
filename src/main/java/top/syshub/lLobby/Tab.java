package top.syshub.lLobby;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.*;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Tab implements Listener {

    public static class PlayerSkin {
        private final String texture;
        private final String signature;

        public PlayerSkin(String texture, String signature) {
            this.texture = texture;
            this.signature = signature;
        }
    }

    public static final Map<String, Set<String>> serversList = new ConcurrentHashMap<>();

    public static final Map<String, UUID> uuidMap = new ConcurrentHashMap<>();

    public static final Map<String, String> prefixMap = new ConcurrentHashMap<>();

    public static String currentServer;

    public static final Map<String, String> fakePlayers = new ConcurrentHashMap<>();

    public static void sendServerListMsg() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServers");

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            fakePlayers.clear();
            return;
        }
        players.iterator().next()
                .sendPluginMessage(LLobby.plugin, "BungeeCord", out.toByteArray());
    }

    public static void sendPlayerListMsg(String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList");
        out.writeUTF(serverName);

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) return;
        players.iterator().next()
                .sendPluginMessage(LLobby.plugin, "BungeeCord", out.toByteArray());
    }

    public static void sendServerMsg() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) return;
        players.iterator().next()
                .sendPluginMessage(LLobby.plugin, "BungeeCord", out.toByteArray());
    }

    public static void sendUUIDMsg(String player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UUIDOther");
        out.writeUTF(player);

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) return;
        players.iterator().next()
                .sendPluginMessage(LLobby.plugin, "BungeeCord", out.toByteArray());
    }

    private static PlayerSkin getPlayerSkin(String player) {
        String yggdrasilApi = LLobby.config.getString("yggdrasil-api", "");
        String skinApi = yggdrasilApi.isEmpty() ?
                "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false" :
                yggdrasilApi + "/sessionserver/session/minecraft/profile/%s?unsigned=false";
        String profileApi = String.format(skinApi, uuidMap.get(player).toString().replace("-", ""));

        Gson gson = new Gson();
        HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(profileApi))
                .build();

        try(HttpClient client = HttpClient.newHttpClient()) {
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;
            String result = response.body();
            JsonObject profile = gson.fromJson(result, JsonObject.class);


            for (JsonElement property : profile.getAsJsonArray("properties"))
                if (property.getAsJsonObject().get("name").getAsString().equals("textures"))
                    return new PlayerSkin(
                            property.getAsJsonObject().get("value").getAsString(),
                            property.getAsJsonObject().get("signature").getAsString()
                    );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private static void addPlayer(String player) {
        try {
            String displayName = prefixMap.get(player) + player;
            WrappedGameProfile gameProfile = new WrappedGameProfile(uuidMap.get(player), player);
            PlayerSkin skin = getPlayerSkin(player);
            if (skin != null)
                gameProfile.getProperties().put(
                        "textures",
                        new WrappedSignedProperty(
                                "textures",
                                skin.texture,
                                skin.signature
                        )
                );
            PlayerInfoData playerInfoData = new PlayerInfoData(
                    gameProfile,
                    0,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(displayName)
            );
            PacketContainer packet = LLobby.protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            packet.getPlayerInfoActions().write(0, EnumSet.of(
                    EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                    EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME,
                    EnumWrappers.PlayerInfoAction.UPDATE_LISTED
            ));
            packet.getPlayerInfoDataLists().write(1, Collections.singletonList(playerInfoData));

            LLobby.protocolManager.broadcastServerPacket(packet);
        } catch(FieldAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void updatePlayer(String player) {
        try {
            String displayName = prefixMap.get(player) + player;
            WrappedGameProfile gameProfile = new WrappedGameProfile(uuidMap.get(player), "");
            PlayerInfoData playerInfoData = new PlayerInfoData(
                    gameProfile,
                    0,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(displayName)
            );
            PacketContainer packet = LLobby.protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            packet.getPlayerInfoActions().write(0, Collections.singleton(
                    EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME
            ));
            packet.getPlayerInfoDataLists().write(1, Collections.singletonList(playerInfoData));

            LLobby.protocolManager.broadcastServerPacket(packet);
        } catch(FieldAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void removePlayer(String player) {
        try {
            UUID uuid = uuidMap.get(player);
            if (uuid == null) return;
            PacketContainer packet = LLobby.protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            packet.getUUIDLists().write(0, Collections.singletonList(uuid));

            LLobby.protocolManager.broadcastServerPacket(packet);
        } catch(FieldAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendFakePlayersTo(Player target) {
        try {
            for (String name : fakePlayers.keySet()) {
                UUID uuid = uuidMap.get(name);
                if (uuid == null) continue;
                String displayName = fakePlayers.get(name);

                WrappedGameProfile gameProfile = new WrappedGameProfile(uuid, name);
                PlayerSkin skin = getPlayerSkin(name);
                if (skin != null)
                    gameProfile.getProperties().put(
                            "textures",
                            new WrappedSignedProperty(
                                    "textures",
                                    skin.texture,
                                    skin.signature
                            )
                    );
                PlayerInfoData infoData = new PlayerInfoData(
                        gameProfile,
                        0,
                        EnumWrappers.NativeGameMode.SURVIVAL,
                        WrappedChatComponent.fromText(displayName)
                );
                PacketContainer packet = LLobby.protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
                packet.getPlayerInfoActions().write(0, EnumSet.of(
                        EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                        EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME,
                        EnumWrappers.PlayerInfoAction.UPDATE_LISTED
                ));
                packet.getPlayerInfoDataLists().write(1, Collections.singletonList(infoData));

                LLobby.protocolManager.sendServerPacket(target, packet);
            }
        } catch(FieldAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void syncFakePlayer() {
        Map<String, String> newFakePlayer = new HashMap<>();
        Map<String, Set<String>> listCopy = new HashMap<>(serversList);
        listCopy.remove(currentServer);
        listCopy.values().forEach(l -> l.forEach(player -> {
            String prefix = prefixMap.get(player);
            if (prefix == null) return;
            newFakePlayer.put(player, prefix + player);
        }));

        for (Map.Entry<String, String> e : newFakePlayer.entrySet()) {
            String player = e.getKey();
            String displayName = e.getValue();
            UUID uuid = uuidMap.get(player);

            if (uuid !=null && !fakePlayers.containsKey(player)) {
                addPlayer(player);
                fakePlayers.put(player, displayName);
            } else if (uuid != null && !fakePlayers.get(player).equals(displayName)) {
                updatePlayer(player);
                fakePlayers.put(player, displayName);
            }
        }

        Iterator<Map.Entry<String, String>> it = fakePlayers.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            String player = e.getKey();
            if (!newFakePlayer.containsKey(player)) {
                if (!serversList.get(currentServer).contains(player)) removePlayer(player);
                it.remove();
            }
        }
    }

    public static void refreshTab() {
        List<Map<?, ?>> servers = LLobby.config.getMapList("servers");

        for (int i = 0; i < servers.size(); i++) {
            Map<?, ?> s = servers.get(i);

            if (s.get("server").equals(currentServer)) {
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                if (players.isEmpty()) continue;
                String prefix = s.get("prefix").toString();
//                int I = i;
                players.forEach(p -> {
                    prefixMap.put(p.getName(), prefix);
//                    setPriority(p, I);
                    p.setPlayerListName(prefix + p.getName());
                });
            } else {
                Set<String> list = serversList.get(s.get("server").toString());
                if (list == null || list.isEmpty()) continue;
                String prefix = s.get("prefix").toString();
//                int I = i;
                list.forEach(l -> {
                    prefixMap.put(l, prefix);
//                    if (fakePlayers.containsKey(l))
//                        setPriority(Objects.requireNonNull(LLobby.plugin.getServer().getPlayer(uuidMap.get(l))), I);
                });
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (LLobby.plugin.getServer().getOnlinePlayers().size() == 1) return;
        Player player = e.getPlayer();
        sendFakePlayersTo(player);
    }
}
