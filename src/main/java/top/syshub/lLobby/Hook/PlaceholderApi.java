package top.syshub.lLobby.Hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import top.syshub.lLobby.Manager.TabManager;

import static java.util.Objects.requireNonNull;
import static top.syshub.lLobby.LLobby.plugin;
import static top.syshub.lLobby.Manager.LocationManager.nicknames;

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
        if (params.equalsIgnoreCase("prefix"))
            return TabManager.prefixMap.getOrDefault(player.getName(), "");
        if (params.equalsIgnoreCase("world")) {
            String world = requireNonNull(requireNonNull(player.getLocation()).getWorld()).getName();
            return nicknames.getOrDefault(world, world);
        }
        return null;
    }
}
