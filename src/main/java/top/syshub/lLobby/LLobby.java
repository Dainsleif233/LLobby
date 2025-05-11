package top.syshub.lLobby;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public final class LLobby extends JavaPlugin {

    public static LLobby plugin;
    public static FileConfiguration config;
    public static Map<String, List<String>> worlds;

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
        config = getConfig();
        worlds = buildWorldLocationsMap();
    }

    @Override
    public void onEnable() {

        plugin  = this;
        try {
            load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Objects.requireNonNull(Bukkit.getPluginCommand("llobby")).setExecutor(new Command.LLobbyCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("llobby")).setTabCompleter(new Command.LLobbyCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("llobbyadmin")).setExecutor(new Command.LLobbyAdminCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("llobbyadmin")).setTabCompleter(new Command.LLobbyAdminCommand());
    }

    @Override
    public void onDisable() {}
}
