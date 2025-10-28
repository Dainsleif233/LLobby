package top.syshub.lLobby.Manager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.wrappers.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static top.syshub.lLobby.Hook.BungeeMessage.*;
import static top.syshub.lLobby.LLobby.protocolManager;
import static top.syshub.lLobby.Manager.TabManager.prefixMap;

public class FakePlayerManager {

    public static final Map<String, String> fakePlayers = new ConcurrentHashMap<>();

    private static void addPlayer(String player) {
        try {
            String displayName = prefixMap.get(player) + player;
            WrappedGameProfile gameProfile = new WrappedGameProfile(uuidMap.get(player), player);
            PlayerSkinManager.PlayerSkin skin = PlayerSkinManager.getPlayerSkin(player);
            if (skin != null)
                gameProfile.getProperties().put(
                        "textures",
                        new WrappedSignedProperty(
                                "textures",
                                skin.texture(),
                                skin.signature()
                        )
                );
            PlayerInfoData playerInfoData = new PlayerInfoData(
                    gameProfile,
                    0,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText(displayName)
            );
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            packet.getPlayerInfoActions().write(0, EnumSet.of(
                    EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                    EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME,
                    EnumWrappers.PlayerInfoAction.UPDATE_LISTED
            ));
            packet.getPlayerInfoDataLists().write(1, Collections.singletonList(playerInfoData));

            protocolManager.broadcastServerPacket(packet);
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
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            packet.getPlayerInfoActions().write(0, Collections.singleton(
                    EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME
            ));
            packet.getPlayerInfoDataLists().write(1, Collections.singletonList(playerInfoData));

            protocolManager.broadcastServerPacket(packet);
        } catch(FieldAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void removePlayer(String player) {
        try {
            UUID uuid = uuidMap.get(player);
            if (uuid == null) return;
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            packet.getUUIDLists().write(0, Collections.singletonList(uuid));

            protocolManager.broadcastServerPacket(packet);
        } catch(FieldAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendFakePlayersTo(Player target) {
        try {
            for (String name : fakePlayers.keySet()) {
                UUID uuid = uuidMap.get(name);
                if (uuid == null) continue;
                String displayName = fakePlayers.get(name);

                WrappedGameProfile gameProfile = new WrappedGameProfile(uuid, name);
                PlayerSkinManager.PlayerSkin skin = PlayerSkinManager.getPlayerSkin(name);
                if (skin != null)
                    gameProfile.getProperties().put(
                            "textures",
                            new WrappedSignedProperty(
                                    "textures",
                                    skin.texture(),
                                    skin.signature()
                            )
                    );
                PlayerInfoData infoData = new PlayerInfoData(
                        gameProfile,
                        0,
                        EnumWrappers.NativeGameMode.SURVIVAL,
                        WrappedChatComponent.fromText(displayName)
                );
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
                packet.getPlayerInfoActions().write(0, EnumSet.of(
                        EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                        EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME,
                        EnumWrappers.PlayerInfoAction.UPDATE_LISTED
                ));
                packet.getPlayerInfoDataLists().write(1, Collections.singletonList(infoData));

                protocolManager.sendServerPacket(target, packet);
            }
        } catch(FieldAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void syncFakePlayer() {
        Map<String, String> newFakePlayer = new HashMap<>();
        Map<String, Set<String>> listCopy = new HashMap<>(playerList);
        listCopy.remove(server);
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
                if (!playerList.get(server).contains(player)) removePlayer(player);
                it.remove();
            }
        }
    }
}
