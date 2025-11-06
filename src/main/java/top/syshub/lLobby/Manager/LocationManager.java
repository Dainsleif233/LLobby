package top.syshub.lLobby.Manager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static top.syshub.lLobby.LLobby.config;

public class LocationManager {

    public static Map<String, Map<String, String>> worlds = new ConcurrentHashMap<>();

    public static Map<String, String> nicknames = new ConcurrentHashMap<>();

    public static void buildWorldLocationsMap() {
        worlds.clear();
        nicknames.clear();
        List<Map<?, ?>> worldList = config.getMapList("worlds");

        worldList.forEach(world -> {
            if (world instanceof Map<?, ?> worldObj) {
                Map<String, String> locations = ((List<?>) worldObj.get("locations")).stream()
                        .filter(Objects::nonNull).filter(p -> p instanceof Map<?, ?>)
                        .filter(l -> ((Map<?, ?>) l).containsKey("name"))
                        .collect(Collectors.toMap(
                                l -> ((Map<?, ?>) l).get("name").toString(),
                                l -> {
                                    Map<?, ?> loc = (Map<?, ?>) l;
                                    Object nick = loc.get("nick");
                                    return nick != null ? nick.toString() : loc.get("name").toString();
                                },
                                (existing, replacement) -> existing
                        ));

                String worldName = (String) worldObj.get("name");
                String nickname = (String) worldObj.get("nick");
                if (worldName != null)
                    worlds.put(worldName, locations);
                if (nickname != null)
                    nicknames.put(worldName, nickname);
            }
        });
    }
}
