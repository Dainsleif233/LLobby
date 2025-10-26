package top.syshub.lLobby;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Command {

    public static class LLobbyCommand implements TabExecutor {

        @Override
        @ParametersAreNonnullByDefault
        public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
            if (args.length == 1) return LLobby.worlds.keySet().stream().toList();
            if (args.length == 2) return LLobby.worlds.getOrDefault(args[0], Collections.emptyList()).stream().toList();
            return List.of();
        }

        @Override
        @ParametersAreNonnullByDefault
        public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
            if (!(sender instanceof Player)) return false;
            if (args.length == 0) return executeRandomTeleport((Player) sender, null);
            if (args.length == 1) return executeRandomTeleport((Player) sender, args[0]);
            if (args.length == 2) return executeTeleport((Player) sender, args[0], args[1]);
            return false;
        }

        private static boolean executeRandomTeleport(Player sender, String world) {
            if (LLobby.worlds.isEmpty()) return false;
            Random random = ThreadLocalRandom.current();
            if (world == null) {
                List<String> worlds = new ArrayList<>(LLobby.worlds.keySet());
                world = worlds.get(random.nextInt(worlds.size()));
            }
            List<String> locations = LLobby.worlds.get(world);
            if (locations == null || locations.isEmpty()) return false;
            String location = locations.get(random.nextInt(locations.size()));
            return executeTeleport(sender, world, location);
        }

        private static boolean executeTeleport(Player sender, String world, String locationName) {

            if (!LLobby.worlds.containsKey(world) || !LLobby.worlds.get(world).contains(locationName)) return false;

            Map<?, ?> worldObj = LLobby.config.getMapList("worlds").stream()
                    .filter(p -> p.get("name").equals(world)).findFirst().orElse(Map.of());
            List<Double> location = ((List<?>) worldObj.get("locations")).stream()
                    .map(p -> (Map<?, ?>) p)
                    .filter(p -> p.get("name").equals(locationName)).findFirst()
                    .map(p -> ((List<?>) p.get("position")).stream()
                            .map(n -> ((Number) n).doubleValue())
                            .collect(Collectors.toList())).orElse(Collections.emptyList());

            sender.teleport(new Location(LLobby.plugin.getServer().getWorld(world), location.get(0), location.get(1), location.get(2), location.get(3).floatValue(), location.get(4).floatValue()));
            return true;
        }
    }

    public static class LLobbyAdminCommand implements TabExecutor {

        @Override
        @ParametersAreNonnullByDefault
        public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
            if (args.length == 1) return List.of("reload", "add", "remove");
            if (args.length == 2 && args[0].equals("remove")) return LLobby.worlds.keySet().stream().toList();
            if (args.length == 3 && args[0].equals("remove")) return LLobby.worlds.get(args[1]);
            return List.of();
        }

        @Override
        @ParametersAreNonnullByDefault
        public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
            if (!(sender instanceof Player)) return false;
            try {
                if (args.length == 1 && args[0].equals("reload")) {
                    LLobby.plugin.load();
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
                if(args[0].equals("help")) {
                    sender.sendMessage(Tab.fakePlayers.toString());
                    return true;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return false;
        }

        private static void executeAdd(Player sender, String locationName) throws IOException {
            Location senderLocation = sender.getLocation();
            String world = Objects.requireNonNull(senderLocation.getWorld()).getName();
            if (LLobby.worlds.containsKey(world) && LLobby.worlds.get(world).contains(locationName)) {
                sender.sendMessage(ChatColor.RED + locationName + "已存在");
                return;
            }
            List<? extends Number> location = List.of(senderLocation.getX(), senderLocation.getY(), senderLocation.getZ(), senderLocation.getYaw(), senderLocation.getPitch());
            List<Map<?, ?>> oldWorlds = LLobby.config.getMapList("worlds");
            if (oldWorlds.stream().filter(p -> world.equals(p.get("name"))).findFirst().isEmpty()) {
                oldWorlds.add(Map.of("name", world, "locations", List.of(Map.of("name", locationName, "position", location))));
                LLobby.config.set("worlds", oldWorlds);
            }else {
                List<?> oldLocations = oldWorlds.stream()
                        .filter(p -> p.get("name").equals(world)).findFirst()
                        .map(p -> ((List<?>) p.get("locations"))).orElse(new ArrayList<>());
                List<Object> newLocations = new ArrayList<>(oldLocations);
                newLocations.add(Map.of("name", locationName, "position", location));
                setNewWorlds(oldWorlds, world, newLocations);
            }
            LLobby.plugin.saveConfig();
            LLobby.plugin.load();
            sender.sendMessage(ChatColor.GREEN + locationName + "已添加");
        }

        private static void executeRemove(Player sender, String world, String locationName) throws IOException {
            if (!LLobby.worlds.containsKey(world) || !LLobby.worlds.get(world).contains(locationName)) {
                sender.sendMessage(ChatColor.RED + locationName + "不存在");
                return;
            }
            List<Map<?, ?>> oldWorlds = LLobby.config.getMapList("worlds");
            List<?> newLocations = new ArrayList<>(oldWorlds.stream()
                    .filter(p -> p.get("name").equals(world)).findFirst()
                    .map(p -> ((List<?>) p.get("locations"))).orElse(new ArrayList<>()).stream()
                    .filter(p -> !((Map<?, ?>) p).get("name").equals(locationName)).toList());

            setNewWorlds(oldWorlds, world, newLocations);
            LLobby.plugin.saveConfig();
            LLobby.plugin.load();
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
            LLobby.config.set("worlds", newWorlds);
        }
    }
}
