package top.syshub.lLobby.Manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static top.syshub.lLobby.Hook.BungeeMessage.*;
import static top.syshub.lLobby.LLobby.config;
import static top.syshub.lLobby.LLobby.plugin;

public class TabManager implements Listener {

    public static final Map<String, String> prefixMap = new ConcurrentHashMap<>();

    public static void refreshTab() {
        List<Map<?, ?>> servers = config.getMapList("servers");

        for (int i = 0; i < servers.size(); i++) {
            Map<?, ?> s = servers.get(i);

            if (s.get("server").equals(server)) {
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
                Set<String> list = playerList.get(s.get("server").toString());
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
        FakePlayerManager.syncFakePlayer();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.getServer().getOnlinePlayers().size() == 1) return;
        Player player = e.getPlayer();
        FakePlayerManager.sendFakePlayersTo(player);
    }
}
