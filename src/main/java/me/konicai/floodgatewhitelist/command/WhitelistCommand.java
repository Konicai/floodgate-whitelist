package me.konicai.floodgatewhitelist.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import me.konicai.floodgatewhitelist.FloodgateWhitelist;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;
import java.util.logging.Logger;

public class WhitelistCommand extends SubCommand {

    private static final String ADD = "add";
    private static final String REMOVE = "remove";
    private static final String CLEAR = "clear";
    private static final String READ = "read";

    private final FloodgateWhitelist whitelist;
    private final Logger logger;
    private final FloodgateApi floodgateApi = FloodgateApi.getInstance();

    private final Server server = Bukkit.getServer();

    public WhitelistCommand(FloodgateWhitelist whitelist) {
        this.whitelist = whitelist;
        this.logger = whitelist.getLogger();
    }

    public void register(CommandManager<CommandSender> manager, Command.Builder<CommandSender> defaultBuilder) {
        manager.command(defaultBuilder
            .literal(ADD)
            .permission(makePermission(REMOVE))
            .argument(StringArgument.greedy("gamertag"))
            .handler(context -> {
                CommandSender sender = context.getSender();
                String gamertag = context.get("gamertag");
                gamertag = gamertag.replace(' ', '_');
                Player player = getPlayer(gamertag);
                if (player != null) {
                    // #getPlayer may return null for offline players... so it may not?
                    UUID uuid = player.getUniqueId();
                    if (floodgateApi.isFloodgateId(uuid)) {
                        player.setWhitelisted(true);
                        sender.sendMessage(gamertag + " (" + uuid + ") was immediately added to the whitelist.");
                        if (!isConsole(sender)) {
                            logger.info(gamertag + " has been successfully whitelisted by " + sender.getName());
                        }
                        return;
                    }

                    // A java edition player has the same name. continue on and cache the gamertag.
                }

                whitelist.cache(gamertag);
                sender.sendMessage(gamertag + " will be added to the whitelist when they attempt to join.");
                if (!isConsole(sender)) {
                    logger.info(sender.getName() + " added " + gamertag + " to the temporary whitelist.");
                }
            })
        );

        manager.command(defaultBuilder
            .literal(REMOVE)
            .permission(makePermission(REMOVE))
            .argument(StringArgument.greedy("gamertag"))
            .handler(context -> {
                CommandSender sender = context.getSender();
                String gamertag = context.get("gamertag");
                gamertag = gamertag.replace(' ', '_');

                if (whitelist.removeCache(gamertag)) {
                    sender.sendMessage(gamertag + " was removed from the cache.");
                    if (!isConsole(sender)) {
                        logger.info(gamertag + "was removed from the cache by " + sender.getName());
                    }
                } else {
                    sender.sendMessage(gamertag + " is not in the cache.");
                }
            })
        );

        manager.command(defaultBuilder
            .literal(CLEAR)
            .permission(makePermission(CLEAR))
            .handler(context -> {
                CommandSender sender = context.getSender();
                whitelist.clearCache();
                sender.sendMessage("The whitelist cache has been cleared.");
            })
        );

        manager.command(defaultBuilder
            .literal(READ)
            .permission(makePermission(READ))
            .handler(context -> {
                CommandSender sender = context.getSender();
                String[] entries = whitelist.cacheEntries();
                if (entries.length == 0) {
                    sender.sendMessage("The whitelist cache is empty.");
                } else {
                    sender.sendMessage("The following gamertags are in the whitelist cache:");
                    sender.sendMessage(String.join(", ", whitelist.cacheEntries()));
                }
            })
        );
    }

    private Player getPlayer(@NonNull String gamertag) {
        return server.getPlayer(whitelist.prefixGamertag(gamertag));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isConsole(CommandSender sender) {
        return sender instanceof ConsoleCommandSender;
    }
}
