package org.bukkit.plugin.java;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

public abstract class JavaPlugin implements Plugin {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final PluginDescriptionFile description = new PluginDescriptionFile("0.0.0");

    @Override
    public File getDataFolder() {
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Server getServer() {
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public PluginDescriptionFile getDescription() {
        return description;
    }

    @Override
    public InputStream getResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(Objects.requireNonNull(path));
    }

    public void saveDefaultConfig() {
        // no-op for stubs
    }

    public void saveResource(String resourcePath, boolean replace) {
        // no-op for stubs
    }

    public void onEnable() {
    }

    public void onDisable() {
    }
}
