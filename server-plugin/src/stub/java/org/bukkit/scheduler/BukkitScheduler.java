package org.bukkit.scheduler;

import org.bukkit.plugin.Plugin;

public interface BukkitScheduler {
    BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period);

    BukkitTask runTask(Plugin plugin, Runnable task);

    BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task);
}
