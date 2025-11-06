package top.syshub.lLobby.Command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static top.syshub.lLobby.LLobby.config;
import static top.syshub.lLobby.LLobby.plugin;
import static top.syshub.lLobby.Manager.LocationManager.nicknames;
import static top.syshub.lLobby.Manager.LocationManager.worlds;

public class LLobbyCommand implements TabExecutor, Listener {

    @Override
    @ParametersAreNonnullByDefault
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) return worlds.keySet().stream().toList();
        if (args.length == 2) return worlds.getOrDefault(args[0], new HashMap<>()).keySet().stream().toList();
        return List.of();
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        
        List<String> locationName;
        if (args.length == 0) locationName = getRandomLocation(null);
        else if (args.length == 1) locationName = getRandomLocation(args[0]);
        else if (args.length == 2) locationName = List.of(args[0], args[1]);
        else return false;
        
        return playerTeleport((Player) sender, locationName);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        playerTeleport(e.getPlayer(), getRandomLocation(null));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        List<String> locationName = getRandomLocation(null);
        if (locationName == null) return;
        Location location = getLocation(locationName.get(0), locationName.get(1));
        if (location == null) return;
        e.setRespawnLocation(location);
        showTitle(e.getPlayer(), locationName.get(0), locationName.get(1));
    }

    private static boolean playerTeleport(Player player, List<String> locationName) {
        if (locationName == null) return false;
        Location location = getLocation(locationName.get(0), locationName.get(1));
        if (location == null) return false;
        player.teleport(location);
        showTitle(player, locationName.get(0), locationName.get(1));
        return true;
    }

    private static List<String> getRandomLocation(String world) {
        if (worlds.isEmpty()) return null;
        Random random = ThreadLocalRandom.current();

        if (world == null) {
            List<String> worldList = new ArrayList<>(worlds.keySet());
            world = worldList.get(random.nextInt(worldList.size()));
        }

        Map<String, String> worldLocations = worlds.get(world);
        if (worldLocations == null || worldLocations.isEmpty()) return null;
        
        List<String> locations = new ArrayList<>(worldLocations.keySet());
        String name = locations.get(random.nextInt(locations.size()));
        return List.of(world, name);
    }

    private static Location getLocation(String world, String locationName) {
        if (!worlds.containsKey(world) || !worlds.get(world).containsKey(locationName)) return null;

        Map<?, ?> worldObj = config.getMapList("worlds").stream()
                .filter(p -> p.get("name").equals(world))
                .findFirst()
                .orElse(Map.of());

        List<Double> position = ((List<?>) worldObj.get("locations")).stream()
                .map(p -> (Map<?, ?>) p)
                .filter(p -> p.get("name").equals(locationName))
                .findFirst()
                .map(p -> ((List<?>) p.get("position")).stream()
                        .map(n -> ((Number) n).doubleValue())
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        return new Location(
                plugin.getServer().getWorld(world),
                position.get(0), position.get(1), position.get(2),
                position.get(3).floatValue(), position.get(4).floatValue()
        );
    }

    private static void showTitle(Player player, String world, String name) {
        player.resetTitle();
        player.sendTitle(
                worlds.get(world).get(name),
                nicknames.getOrDefault(world, world),
                10, 30, 10
        );
    }
}
