package top.syshub.lLobby.Hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import top.syshub.lLobby.Manager.ServerInfoManager;
import top.syshub.lLobby.Manager.TabManager;

import static java.util.Objects.requireNonNull;
import static top.syshub.lLobby.LLobby.plugin;
import static top.syshub.lLobby.Manager.LocationManager.nicknames;
import static top.syshub.lLobby.Manager.ServerInfoManager.serverInfo;

public class PlaceholderApi extends PlaceholderExpansion {

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return plugin.getDataFolder().getName();
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("prefix")) {
            String name = params.substring(7);
            return TabManager.prefixMap.getOrDefault(name.isEmpty() ? player.getName() : name, "");
        }
        if (params.startsWith("world")) {
            String world = requireNonNull(requireNonNull(player.getLocation()).getWorld()).getName();
            return nicknames.getOrDefault(world, world);
        }
        if (params.startsWith("server_version"))
            return serverInfo.getOrDefault(params.substring(15), new ServerInfoManager.ServerInfo("", 0, 0)).version();
        if (params.startsWith("server_online"))
            return String.valueOf(serverInfo.getOrDefault(params.substring(14), new ServerInfoManager.ServerInfo("", 0, 0)).online());
        if (params.startsWith("server_max"))
            return String.valueOf(serverInfo.getOrDefault(params.substring(11), new ServerInfoManager.ServerInfo("", 0, 0)).max());
        return null;
    }
}
