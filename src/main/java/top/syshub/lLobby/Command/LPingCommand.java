package top.syshub.lLobby.Command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import top.syshub.lLobby.Manager.ServerInfoManager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

public class LPingCommand implements TabExecutor {

    @Override
    @ParametersAreNonnullByDefault
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of();
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ServerInfoManager.ping(args[0]);
        return true;
    }
}
