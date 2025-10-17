package org.bukkit.plugin;

public class PluginDescriptionFile {
    private final String version;

    public PluginDescriptionFile(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
