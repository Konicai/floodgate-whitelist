package me.konicai.floodgatewhitelist.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import lombok.RequiredArgsConstructor;
import me.konicai.floodgatewhitelist.FloodgateWhitelist;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class WhitelistCommand extends SubCommand {

    private static final String ADD = "add";
    private static final String REMOVE = "remove";
    private static final String CLEAR = "clear";
    private static final String READ = "read";

    private final Logger logger;
    private final BukkitAudiences audiences;
    private final FloodgateWhitelist plugin;

    private final Server server = Bukkit.getServer();
    private final FloodgateApi floodgateApi = FloodgateApi.getInstance();

    public void register(CommandManager<CommandSender> manager, Command.Builder<CommandSender> defaultBuilder) {
        manager.command(defaultBuilder
            .literal(ADD)
            .permission(makePermission(REMOVE))
            .argument(StringArgument.of("gamertag"))
            .handler(context -> {
                CommandSender sender = context.getSender();
                String gamertag = prefixGamertag(context.get("gamertag"));
                Player bedrockPlayer = server.getPlayer(gamertag);
                if (bedrockPlayer == null) {
                    plugin.cache(gamertag);
                    sender.sendMessage(gamertag + " will be added to the whitelist when they attempt to join.");
                } else {
                    plugin.whitelistOnlinePlayer(bedrockPlayer.getUniqueId());
                    sender.sendMessage(gamertag + "(" + bedrockPlayer.getUniqueId() + ") was added to the whitelist.");
                }
            })
        );

        manager.command(defaultBuilder
            .literal(REMOVE)
            .permission(makePermission(REMOVE))
            .argument(StringArgument.of("gamertag"))
            .handler(context -> {
                CommandSender sender = context.getSender();
                String gamertag = prefixGamertag(context.get("gamertag"));


            })
        );
        manager.command(defaultBuilder
            .literal(CLEAR)
            .permission(makePermission(CLEAR))
        );

        manager.command(defaultBuilder
            .literal(READ)
            .permission(makePermission(READ))
        );
    }

    @Nonnull
    private String prefixGamertag(@Nonnull String gamertag) {
        if (gamertag.startsWith(floodgateApi.getPlayerPrefix())) {
            return gamertag;
        } else {
            return floodgateApi.getPlayerPrefix() + gamertag;
        }
    }
}
