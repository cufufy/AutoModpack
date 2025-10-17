package org.bukkit.command;

public class PluginCommand extends Command {
    private CommandExecutor executor;
    private TabCompleter completer;

    public PluginCommand(String name) {
        super(name);
    }

    public void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    public void setTabCompleter(TabCompleter completer) {
        this.completer = completer;
    }

    public CommandExecutor getExecutor() {
        return executor;
    }

    public TabCompleter getTabCompleter() {
        return completer;
    }
}
