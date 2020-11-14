package raytech.warppads;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Maxine on 13/11/2020.
 *
 * @author Maxine
 * @since 13/11/2020
 */
public class CommandWarpPads implements CommandExecutor {
    public SpigotPlugin plugin;

    private enum CommandHandlers {
        HELP {
            @Override
            public String[] getUsage() {
                return new String[0];
            }

            @Override
            public String getHelp() {
                return "This is the help command!";
            }

            @Override
            public boolean onCommand(SpigotPlugin plugin, CommandSender sender, Command command, String[] args) {
                StringBuilder sb = new StringBuilder();

                for (Map.Entry<String, CommandHandlers> e : entries.entrySet()) {
                    String commandName = e.getKey();
                    CommandHandlers handler = e.getValue();
                    sb.append(ChatColor.GREEN).append(commandName).append(' ').append(String.join(" ", handler.getUsage())).append(ChatColor.WHITE).append(": ").append(handler.getHelp()).append('\n');
                }

                sender.sendMessage(sb.toString());

                return true;
            }
        },
        RELOAD {
            @Override
            public String[] getUsage() {
                return new String[0];
            }

            @Override
            public String getHelp() {
                return "Reloads the configuration used by Warp Pads.";
            }

            @Override
            public boolean onCommand(SpigotPlugin plugin, CommandSender sender, Command command, String[] args) {
                long startTime = System.currentTimeMillis();

                Config.loadConfig(plugin);
                plugin.runSchedulers();

                sender.sendMessage("Reloaded config in " + (System.currentTimeMillis() - startTime) + "ms");
                return true;
            }
        },
        CLEAR {
            @Override
            public String[] getUsage() {
                return new String[] {"<world>"};
            }

            @Override
            public String getHelp() {
                return "Clears all Warp Pads in a given world.";
            }

            @Override
            public boolean onCommand(SpigotPlugin plugin, CommandSender sender, Command command, String[] args) {
                long startTime = System.currentTimeMillis();

                World world = plugin.getServer().getWorld(args[0]);
                if (world == null) {
                    String[] worlds = plugin.getServer().getWorlds().stream().map(w -> ChatColor.WHITE + w.getName() + ChatColor.RED).toArray(String[]::new);
                    sender.sendMessage(ChatColor.RED + "World does not exist: " + ChatColor.WHITE + args[0] + ChatColor.RED + ", should be one of [" + String.join(", ", worlds) + "]");
                    return false;
                }

                WarpData warpData = plugin.warps.getWarps(world);
                if (warpData == null) {
                    sender.sendMessage(ChatColor.RED + "WarpData for " + ChatColor.WHITE + world.getName() + ChatColor.RED + " does not exist, internal state may be corrupt");
                    return false;
                }

                warpData.warps.clear();
                warpData.playersStandingOnWarps.clear();
                plugin.saveWarpsToFile(world);

                sender.sendMessage(ChatColor.GREEN + "Cleared all warps in " + (System.currentTimeMillis() - startTime) + "ms. Saving is not instant!");
                return true;
            }
        },
        GIVE {
            @Override
            public String[] getUsage() {
                return new String[0];
            }

            @Override
            public String getHelp() {
                return "Gives the current player a stack of 64 Tier 1 Warp Pads.";
            }

            @Override
            public boolean onCommand(SpigotPlugin plugin, CommandSender sender, Command command, String[] args) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Must be a player to execute this command");
                    return false;
                }

                ((Player)sender).getInventory().addItem(plugin.warpPadT1.getStackOf(64));
                return false;
            }
        }
        ;

        public static Map<String, CommandHandlers> entries = Arrays.stream(values()).collect(Collectors.toMap(
                e -> e.name().toLowerCase(Locale.ROOT), e -> e
        ));

        public abstract String[] getUsage();

        public abstract String getHelp();

        public abstract boolean onCommand(SpigotPlugin plugin, CommandSender sender, Command command, String[] args);

        public final boolean runCommand(SpigotPlugin plugin, CommandSender sender, Command command, String[] args) {
            if (args.length < getUsage().length) {
                sender.sendMessage(ChatColor.RED + "Not enough arguments supplied to the command, expected " + String.join(" ", getUsage()));
                return false;
            }
            return onCommand(plugin, sender, command, args);
        }
    }

    public CommandWarpPads(SpigotPlugin plugin) {
        this.plugin = plugin;
    }

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length <= 0) {
            sender.sendMessage(ChatColor.RED + "Not enough arguments supplied to the command. Expected at least 1.");
            return false;
        }

        CommandHandlers entry = CommandHandlers.entries.get(args[0]);
        if (entry == null) {
            sender.sendMessage(ChatColor.RED + "Not a subcommand: '" + args[0] + "'");
            return false;
        }

        try {
            return entry.runCommand(plugin, sender, command, Arrays.copyOfRange(args, 1, args.length));
        } catch (Exception ex) {
            String stackTrace = ExceptionUtils.getFullStackTrace(ex);
            plugin.getLogger().severe(stackTrace);
            sender.sendMessage(ChatColor.RED + "Failed to execute command " + entry.name() + ":\n" + stackTrace);
            return false;
        }
    }
}