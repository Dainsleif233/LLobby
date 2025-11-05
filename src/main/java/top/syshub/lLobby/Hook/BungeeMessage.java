package top.syshub.lLobby.Hook;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import top.syshub.lLobby.Manager.FakePlayerManager;
import top.syshub.lLobby.Manager.TabManager;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static top.syshub.lLobby.LLobby.plugin;

public class BungeeMessage implements PluginMessageListener {

    public static Map<String, Set<String>> playerList = new ConcurrentHashMap<>();

    public static Map<String, UUID> uuidMap = new ConcurrentHashMap<>();

    public static String server;

    @Override
    public void onPluginMessageReceived(String channel, @Nonnull Player player, @Nonnull byte[] message) {
        if (!channel.equals("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("GetServers")) {
            String servers = in.readUTF();

            String[] serverList = servers.isEmpty() ? new String[0] : servers.split(", ");
            for (String s : serverList) sendPlayerListMsg(s);
            sendServerMsg();
        }

        if (subChannel.equals("PlayerList")) {
            String server = in.readUTF();
            String players = in.readUTF();

            Set<String> playerSet = Set.of(players.isEmpty() ? new String[0] : players.split(", "));
            for(String s : playerSet) sendUUIDMsg(s);
            playerList.put(server, new HashSet<>(playerSet));
            TabManager.refreshTab();
        }

        if (subChannel.equals("UUIDOther")) {
            String name = in.readUTF();
            String uuidStr = in.readUTF();

            String formattedUUID = uuidStr.substring(0, 8) + "-" +
                    uuidStr.substring(8, 12) + "-" +
                    uuidStr.substring(12, 16) + "-" +
                    uuidStr.substring(16, 20) + "-" +
                    uuidStr.substring(20, 32);
            if (!uuidMap.containsKey(name)) uuidMap.put(name, UUID.fromString(formattedUUID));
        }

        if (subChannel.equals("GetServer"))
            server = in.readUTF();
    }

    public static void sendServerListMsg() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServers");

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            FakePlayerManager.fakePlayers.clear();
            return;
        }
        players.iterator().next()
                .sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public static void sendPlayerListMsg(String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerList");
        out.writeUTF(serverName);

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) return;
        players.iterator().next()
                .sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public static void sendServerMsg() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServer");

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) return;
        players.iterator().next()
                .sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public static void sendUUIDMsg(String player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("UUIDOther");
        out.writeUTF(player);

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) return;
        players.iterator().next()
                .sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}
