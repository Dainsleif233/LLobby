package top.syshub.lLobby;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public final class LLobby extends JavaPlugin implements PluginMessageListener {

    public static LLobby plugin;
    public static FileConfiguration config;
    public static Map<String, List<String>> worlds;
    public static ProtocolManager protocolManager;

    private static Map<String, List<String>> buildWorldLocationsMap() {
        Map<String, List<String>> result = new HashMap<>();
        List<Map<?, ?>> worlds = config.getMapList("worlds");

        worlds.forEach(world -> {
            if (world instanceof Map<?, ?> worldObj) {
                List<String> locationNames = ((List<?>) worldObj.get("locations"))
                        .stream()
                        .filter(l -> l instanceof Map<?, ?>)
                        .map(l -> ((Map<?, ?>) l).get("name"))
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.toList());

                String worldName = (String) worldObj.get("name");
                if (worldName != null) {
                    result.put(worldName, locationNames);
                }
            }
        });
        return result;
    }

    public void load() throws IOException {

        if (Files.exists(plugin.getDataFolder().toPath().resolve("config.yml"))) Files.copy(
                plugin.getDataFolder().toPath().resolve("config.yml"),
                plugin.getDataFolder().toPath().resolve("config.yml.bak"),
                StandardCopyOption.REPLACE_EXISTING
        );
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();
        worlds = buildWorldLocationsMap();
    }

    @Override
    public void onEnable() {

        plugin  = this;
        protocolManager = ProtocolLibrary.getProtocolManager();
        try {
            load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            new PAPI().register();

        Objects.requireNonNull(Bukkit.getPluginCommand("llobby")).setExecutor(new Command.LLobbyCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("llobby")).setTabCompleter(new Command.LLobbyCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("llobbyadmin")).setExecutor(new Command.LLobbyAdminCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("llobbyadmin")).setTabCompleter(new Command.LLobbyAdminCommand());

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
        getServer().getPluginManager().registerEvents(new Tab(), this);

        Bukkit.getScheduler().runTaskTimer(
                this,
                Tab::sendServerListMsg,
                0L,
                20L
        );
    }

    @Override
    public void onPluginMessageReceived(String channel, @Nonnull Player player, @Nonnull byte[] message) {
        if (!channel.equals("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("GetServers")) {
            String servers = in.readUTF();

            String[] serverList = servers.isEmpty() ? new String[0] : servers.split(", ");
            for (String s : serverList) Tab.sendPlayerListMsg(s);
            Tab.sendServerMsg();
        }

        if (subChannel.equals("PlayerList")) {
            String server = in.readUTF();
            String players = in.readUTF();

            List<String> playerList = List.of(players.isEmpty() ? new String[0] : players.split(", "));
            for(String s : playerList) Tab.sendUUIDMsg(s);
            Tab.serversList.put(server, new HashSet<>(playerList));
        }

        if (subChannel.equals("UUIDOther")) {
            String name = in.readUTF();
            String uuidStr = in.readUTF();

            String formattedUUID = uuidStr.substring(0, 8) + "-" +
                    uuidStr.substring(8, 12) + "-" +
                    uuidStr.substring(12, 16) + "-" +
                    uuidStr.substring(16, 20) + "-" +
                    uuidStr.substring(20, 32);
            if (!Tab.uuidMap.containsKey(name)) Tab.uuidMap.put(name, UUID.fromString(formattedUUID));
            Tab.refreshTab();
            Tab.syncFakePlayer();
        }

        if (subChannel.equals("GetServer"))
            Tab.currentServer = in.readUTF();
    }

    @Override
    public void onDisable() {}
}
