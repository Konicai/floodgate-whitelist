package me.konicai.floodgatewhitelist.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import org.bukkit.command.CommandSender;

public abstract class SubCommand {

    protected static final String BASE_PERMISSION = "floodgatewhitelist";

    public abstract void register(CommandManager<CommandSender> manager, Command.Builder<CommandSender> defaultBuilder);

    public String makePermission(String subNode) {
        return BASE_PERMISSION + "." + subNode;
    }
}
