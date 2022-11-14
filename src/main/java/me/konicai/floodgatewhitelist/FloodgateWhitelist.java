package me.konicai.floodgatewhitelist;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import me.konicai.floodgatewhitelist.command.SubCommand;
import me.konicai.floodgatewhitelist.command.WhitelistCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Logger;

public class FloodgateWhitelist extends JavaPlugin implements Listener {

    private static final int METRICS_ID = 16868;
    private static final String ROOT_COMMAND = "flist";
    private static final Object DUMMY = new Object();

    private Logger logger;
    private Server server;
    private BukkitAudiences bukkitAudiences;
    private FloodgateApi floodgateApi;

    @Getter
    private final Cache<String, Object> temporaryWhitelist = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .build();

    @Override
    public void onEnable() {
        logger = getLogger();
        server = Bukkit.getServer();
        bukkitAudiences = BukkitAudiences.create(this);
        floodgateApi = FloodgateApi.getInstance();
        new Metrics(this, METRICS_ID);

        // Yes, this is not Paper-exclusive plugin. Cloud handles this gracefully.
        PaperCommandManager<CommandSender> commandManager;
        try {
            commandManager = new PaperCommandManager<>(
                this,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity()
            );
        } catch (Exception e) {
            logger.severe("Failed to create CommandManager, stopping");
            e.printStackTrace();
            return;
        }

        try {
            // Brigadier is ideal if possible. Allows for much more readable command options, especially on BE.
            commandManager.registerBrigadier();
        } catch (BukkitCommandManager.BrigadierFailureException e) {
            logger.warning("Failed to initialize Brigadier support: " + e.getMessage());
        }

        // Makes the info messages for invalid syntax, sender, etc exceptions nicer
        new MinecraftExceptionHandler<CommandSender>()
            .withArgumentParsingHandler()
            .withInvalidSenderHandler()
            .withInvalidSyntaxHandler()
            .withNoPermissionHandler()
            .withCommandExecutionHandler()
            .apply(commandManager, bukkitAudiences::sender);

        MinecraftHelp<CommandSender> help = new MinecraftHelp<>(
            "/" + ROOT_COMMAND + " help",
            bukkitAudiences::sender,
            commandManager
        );

        Command.Builder<CommandSender> commandBuilder = commandManager.commandBuilder(ROOT_COMMAND);

        commandManager.command(commandBuilder
            .literal("help")
            .permission(SubCommand.BASE_PERMISSION + ".help")
            .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
            .handler(context ->
                help.queryCommands(SubCommand.argument(context, "query", ""), context.getSender()))
        );

        new WhitelistCommand(this).register(commandManager, commandBuilder);
    }

    public void cache(String gamertag) {
        temporaryWhitelist.put(removePrefix(gamertag), DUMMY);
    }

    public boolean removeCache(String gamertag) {
        gamertag = removePrefix(gamertag);
        boolean cached = temporaryWhitelist.getIfPresent(gamertag) != null;
        if (cached) {
            temporaryWhitelist.invalidate(gamertag);
        }
        return cached;

    }

    public void clearCache() {
        temporaryWhitelist.invalidateAll();
    }

    public String[] cacheEntries() {
        return temporaryWhitelist.asMap().keySet().toArray(new String[0]);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.KICK_WHITELIST) {
            return;
        }

        Player player = event.getPlayer();
        if (!floodgateApi.isFloodgatePlayer(player.getUniqueId())) {
            return;
        }

        String username = player.getName();
        String gamertag = removePrefix(username);

        if (temporaryWhitelist.getIfPresent(gamertag) != null) {
            player.setWhitelisted(true);
            temporaryWhitelist.invalidate(gamertag);
            event.setResult(PlayerLoginEvent.Result.ALLOWED);
            logger.info(username + " (" + player.getUniqueId() + ") has been added to the whitelist.");
        }
    }

    @NonNull
    public String prefixGamertag(@NonNull String gamertag) {
        if (gamertag.startsWith(floodgateApi.getPlayerPrefix())) {
            return gamertag;
        } else {
            return floodgateApi.getPlayerPrefix() + gamertag;
        }
    }

    @NotNull
    public String removePrefix(@NotNull String gamertag) {
        String prefix = floodgateApi.getPlayerPrefix();
        if (gamertag.startsWith(prefix)) {
            return gamertag.replaceFirst(prefix, gamertag);
        } else {
            return gamertag;
        }
    }
}
