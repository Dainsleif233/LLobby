package top.syshub.lLobby;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.syshub.lLobby.Command.*;
import top.syshub.lLobby.Hook.*;
import top.syshub.lLobby.Manager.*;

import java.io.IOException;

import static java.util.Objects.requireNonNull;
import static org.bukkit.Bukkit.getPluginCommand;

public final class LLobby extends JavaPlugin {

    public static LLobby plugin;
    public static FileConfiguration config;
    public static ProtocolManager protocolManager;

    public void loadConfig() throws IOException {
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();
        LocationManager.buildWorldLocationsMap();
        TabManager.initTab();
    }

    @Override
    public void onEnable() {

        plugin  = this;
        protocolManager = ProtocolLibrary.getProtocolManager();
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            new PlaceholderApi().register();

        requireNonNull(getPluginCommand("llobby")).setExecutor(new LLobbyCommand());
        requireNonNull(getPluginCommand("llobby")).setTabCompleter(new LLobbyCommand());
        requireNonNull(getPluginCommand("llobbyadmin")).setExecutor(new LLobbyAdminCommand());
        requireNonNull(getPluginCommand("llobbyadmin")).setTabCompleter(new LLobbyAdminCommand());

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeMessage());
        getServer().getPluginManager().registerEvents(new TabManager(), this);
        getServer().getPluginManager().registerEvents(new LLobbyCommand(), this);

        Bukkit.getScheduler().runTaskTimer(
                this,
                BungeeMessage::sendServerListMsg,
                0L,
                20L
        );
    }
}
