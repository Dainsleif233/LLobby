package top.syshub.lLobby.Command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static top.syshub.lLobby.LLobby.plugin;
import static top.syshub.lLobby.Manager.LocationManager.worlds;

public class LLobbyCommand implements TabExecutor {

    @Override
    @ParametersAreNonnullByDefault
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) return worlds.keySet().stream().toList();
        if (args.length == 2) return worlds.getOrDefault(args[0], Collections.emptyList()).stream().toList();
        return List.of();
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        if (args.length == 0) return executeRandomTeleport((Player) sender, null);
        if (args.length == 1) return executeRandomTeleport((Player) sender, args[0]);
        if (args.length == 2) return executeTeleport((Player) sender, args[0], args[1]);
        return false;
    }

    private static boolean executeRandomTeleport(Player sender, String world) {
        if (worlds.isEmpty()) return false;
        Random random = ThreadLocalRandom.current();
        if (world == null) {
            List<String> worldList = new ArrayList<>(worlds.keySet());
            world = worldList.get(random.nextInt(worldList.size()));
        }
        List<String> locations = worlds.get(world);
        if (locations == null || locations.isEmpty()) return false;
        String location = locations.get(random.nextInt(locations.size()));
        return executeTeleport(sender, world, location);
    }

    private static boolean executeTeleport(Player sender, String world, String locationName) {

        if (!worlds.containsKey(world) || !worlds.get(world).contains(locationName)) return false;

        Map<?, ?> worldObj = top.syshub.lLobby.LLobby.config.getMapList("worlds").stream()
                .filter(p -> p.get("name").equals(world)).findFirst().orElse(Map.of());
        List<Double> location = ((List<?>) worldObj.get("locations")).stream()
                .map(p -> (Map<?, ?>) p)
                .filter(p -> p.get("name").equals(locationName)).findFirst()
                .map(p -> ((List<?>) p.get("position")).stream()
                        .map(n -> ((Number) n).doubleValue())
                        .collect(Collectors.toList())).orElse(Collections.emptyList());

        sender.teleport(new Location(plugin.getServer().getWorld(world), location.get(0), location.get(1), location.get(2), location.get(3).floatValue(), location.get(4).floatValue()));
        return true;
    }
}
