package me.konicai.floodgatewhitelist.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;

public abstract class SubCommand {

    public static final String BASE_PERMISSION = "floodgatewhitelist";

    public abstract void register(CommandManager<CommandSender> manager, Command.Builder<CommandSender> defaultBuilder);

    public String makePermission(String subNode) {
        return BASE_PERMISSION + "." + subNode;
    }

    @Contract("_, _, null -> null")
    public static <C, V> V argument(CommandContext<C> context, String key, V defaultValue) {
        return context.getOrDefault(key, defaultValue);
    }
}
