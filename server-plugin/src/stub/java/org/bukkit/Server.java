package org.bukkit;

import java.util.logging.Logger;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public interface Server {
    PluginManager getPluginManager();

    BukkitScheduler getScheduler();

    Logger getLogger();
}
