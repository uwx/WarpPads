package raytech.warppads;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Maxine on 14/11/2020.
 *
 * @author Maxine
 * @since 14/11/2020
 */
public class CommandWarpDeny implements CommandExecutor {
    private final SpigotPlugin plugin;
    private final AccessList accessList;

    public CommandWarpDeny(SpigotPlugin plugin, AccessList accessList) {
        this.plugin = plugin;
        this.accessList = accessList;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be ran as a player");
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Must provide the nickname of the player to remove from whitelist");
            return false;
        }

        Player guest = plugin.getServer().getPlayer(args[0]);
        if (guest == null) {
            sender.sendMessage(ChatColor.RED + "Player " + args[0] + " is not online");
            return false;
        }

        if (guest.getUniqueId().equals(((Player)sender).getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "Cannot remove yourself from whitelist");
            return false;
        }

        accessList.remove(((Player)sender).getUniqueId(), guest.getUniqueId());
        plugin.saveAccessList();
        return false;
    }
}
