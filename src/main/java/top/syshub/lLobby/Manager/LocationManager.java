package top.syshub.lLobby.Manager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static top.syshub.lLobby.LLobby.config;

public class LocationManager {

    public static Map<String, List<String>> worlds = new ConcurrentHashMap<>();

    public static Map<String, String> nicknames = new ConcurrentHashMap<>();

    public static void buildWorldLocationsMap() {
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
                String nickname = (String) worldObj.get("nick");
                if (worldName != null)
                    LocationManager.worlds.put(worldName, locationNames);
                if (nickname != null)
                    LocationManager.nicknames.put(worldName, nickname);
            }
        });
    }
}
