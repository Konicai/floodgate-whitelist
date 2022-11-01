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
import me.konicai.floodgatewhitelist.command.WhitelistCommand;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

public class FloodgateWhitelist extends JavaPlugin {

    private static final String ROOT_COMMAND = "flist";
    private static final Object DUMMY = new Object();

    private Logger logger;
    private Server server;
    private BukkitAudiences bukkitAudiences;
    private FloodgateApi floodgateApi;

    private Cache<String, Object> temporaryWhitelist;

    @Override
    public void onEnable() {
        logger = getLogger();
        server = Bukkit.getServer();
        bukkitAudiences = BukkitAudiences.create(this);
        floodgateApi = FloodgateApi.getInstance();

        Duration expire = Duration.ofMinutes(30);
        temporaryWhitelist = CacheBuilder.newBuilder().expireAfterWrite(expire).build();

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

        MinecraftHelp<CommandSender> minecraftHelp = new MinecraftHelp<>(
            "/" + ROOT_COMMAND + " help",
            bukkitAudiences::sender,
            commandManager
        );

        Command.Builder<CommandSender> commandBuilder = commandManager.commandBuilder(ROOT_COMMAND);

        commandManager.command(commandBuilder
            .literal("help")
            .permission("help")
            .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
            .handler(context -> minecraftHelp.queryCommands(context.getOrDefault("query", ""), context.getSender()))
        );

        new WhitelistCommand(logger, bukkitAudiences, this).register(commandManager, commandBuilder);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public void cache(String gamertag) {
        String prefix = floodgateApi.getPlayerPrefix();
        if (gamertag.startsWith(prefix)) {
            temporaryWhitelist.put(gamertag, DUMMY);
        } else {
            temporaryWhitelist.put(prefix + gamertag, DUMMY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.KICK_WHITELIST) {
            return;
        }

        Player player = event.getPlayer();
        String username = player.getName();

        if (temporaryWhitelist.getIfPresent(username) != null) {
            event.setResult(PlayerLoginEvent.Result.ALLOWED);
            server.getScheduler().runTask(this, () -> {
                whitelistOnlinePlayer(player.getUniqueId());
                temporaryWhitelist.invalidate(username);
            });
        }
    }

    public void whitelistOnlinePlayer(UUID uuid) {
        server.dispatchCommand(server.getConsoleSender(), "whitelist add " + uuid);
    }
}
