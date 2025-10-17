package org.bukkit.plugin;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

import org.bukkit.Server;

public interface Plugin {
    File getDataFolder();

    Logger getLogger();

    Server getServer();

    PluginDescriptionFile getDescription();

    InputStream getResource(String path);
}
