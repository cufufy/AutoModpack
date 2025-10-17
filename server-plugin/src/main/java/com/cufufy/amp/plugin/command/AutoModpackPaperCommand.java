package com.cufufy.amp.plugin.command;

import java.util.List;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AutoModpackPaperCommand extends Command implements PluginIdentifiableCommand {
    private static final String DESCRIPTION = "Controls AutoModpack hosting and configuration.";
    private static final String USAGE = "/automodpack [generate|host|config]";
    private static final List<String> ALIASES = List.of("amp");
    private static final String PERMISSION = "automodpack.admin";

    private final JavaPlugin plugin;
    private final AutoModpackCommand delegate;

    public AutoModpackPaperCommand(JavaPlugin plugin, AutoModpackCommand delegate) {
        super("automodpack");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getDescription() {
        return DESCRIPTION;
    }

    public String getUsage() {
        return USAGE;
    }

    public List<String> getAliases() {
        return ALIASES;
    }

    public String getPermission() {
        return PERMISSION;
    }

    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return delegate.onCommand(sender, this, commandLabel, args);
    }

    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> completions = delegate.onTabComplete(sender, this, alias, args);
        return completions != null ? completions : List.of();
    }
}
