package org.bukkit;

import java.util.logging.Logger;
import java.util.UUID;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.OfflinePlayer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public interface Server {
    PluginManager getPluginManager();

    BukkitScheduler getScheduler();

    Logger getLogger();

    OfflinePlayer getOfflinePlayer(UUID uuid);

    PluginCommand getCommand(String name);
}
