package org.bukkit;

import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public final class Bukkit {
    private static Server server;

    private Bukkit() {
    }

    public static void setServer(Server server) {
        Bukkit.server = server;
    }

    public static Server getServer() {
        return server;
    }

    public static PluginManager getPluginManager() {
        return Objects.requireNonNull(server, "server").getPluginManager();
    }

    public static BukkitScheduler getScheduler() {
        return Objects.requireNonNull(server, "server").getScheduler();
    }

    public static Logger getLogger() {
        return Objects.requireNonNull(server, "server").getLogger();
    }
}
