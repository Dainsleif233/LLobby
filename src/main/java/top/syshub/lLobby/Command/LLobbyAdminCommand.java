package top.syshub.lLobby.Command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static top.syshub.lLobby.LLobby.config;
import static top.syshub.lLobby.LLobby.plugin;
import static top.syshub.lLobby.Manager.LocationManager.worlds;

public class LLobbyAdminCommand implements TabExecutor {

    @Override
    @ParametersAreNonnullByDefault
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) return List.of("reload", "add", "remove");
        if (args.length == 2 && args[0].equals("remove")) return worlds.keySet().stream().toList();
        if (args.length == 3 && args[0].equals("remove")) return worlds.get(args[1]);
        return List.of();
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        try {
            if (args.length == 1 && args[0].equals("reload")) {
                plugin.loadConfig();
                return true;
            }
            if (args.length == 2 && args[0].equals("add")) {
                executeAdd((Player) sender, args[1]);
                return true;
            }
            if (args.length == 3 && args[0].equals("remove")) {
                executeRemove((Player) sender, args[1], args[2]);
                return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private static void executeAdd(Player sender, String locationName) throws IOException {
        Location senderLocation = sender.getLocation();
        String world = requireNonNull(senderLocation.getWorld()).getName();
        if (worlds.containsKey(world) && worlds.get(world).contains(locationName)) {
            sender.sendMessage(ChatColor.RED + locationName + "已存在");
            return;
        }
        List<? extends Number> location = List.of(
                senderLocation.getX(),
                senderLocation.getY(),
                senderLocation.getZ(),
                senderLocation.getYaw(),
                senderLocation.getPitch()
        );
        List<Map<?, ?>> oldWorlds = config.getMapList("worlds");
        if (oldWorlds.stream().filter(p -> world.equals(p.get("name"))).findFirst().isEmpty()) {
            oldWorlds.add(Map.of(
                    "name", world,
                    "locations", List.of(Map.of("name", locationName, "position", location))
            ));
            config.set("worlds", oldWorlds);
        }else {
            List<?> oldLocations = oldWorlds.stream()
                    .filter(p -> p.get("name").equals(world)).findFirst()
                    .map(p -> ((List<?>) p.get("locations"))).orElse(new ArrayList<>());
            List<Object> newLocations = new ArrayList<>(oldLocations);
            newLocations.add(Map.of("name", locationName, "position", location));
            setNewWorlds(oldWorlds, world, newLocations);
        }
        plugin.saveConfig();
        plugin.loadConfig();
        sender.sendMessage(ChatColor.GREEN + locationName + "已添加");
    }

    private static void executeRemove(Player sender, String world, String locationName) throws IOException {
        if (!worlds.containsKey(world) || !worlds.get(world).contains(locationName)) {
            sender.sendMessage(ChatColor.RED + locationName + "不存在");
            return;
        }
        List<Map<?, ?>> oldWorlds = config.getMapList("worlds");
        List<?> newLocations = new ArrayList<>(oldWorlds.stream()
                .filter(p -> p.get("name").equals(world)).findFirst()
                .map(p -> ((List<?>) p.get("locations"))).orElse(new ArrayList<>()).stream()
                .filter(p -> !((Map<?, ?>) p).get("name").equals(locationName)).toList());

        setNewWorlds(oldWorlds, world, newLocations);
        plugin.saveConfig();
        plugin.loadConfig();
        sender.sendMessage(ChatColor.GREEN + locationName + "已移除");
    }

    private static void setNewWorlds(List<Map<?, ?>> oldWorlds, String world, List<?> newLocations) {
        List<Object> newWorlds = new ArrayList<>(oldWorlds.stream()
                .filter(p -> !p.get("name").equals(world)).toList());
        newWorlds.add(oldWorlds.stream()
                .filter(p -> p.get("name").equals(world)).findFirst()
                .map(p -> {
                    HashMap<String, Object> newWorld = new HashMap<>();
                    newWorld.put("name", world);
                    newWorld.put("locations", newLocations);
                    return newWorld;
                }).orElse(null)
        );
        config.set("worlds", newWorlds);
    }
}
