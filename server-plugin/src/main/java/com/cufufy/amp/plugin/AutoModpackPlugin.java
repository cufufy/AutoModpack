package com.cufufy.amp.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import com.cufufy.amp.plugin.command.AutoModpackCommand;
import com.cufufy.amp.plugin.command.AutoModpackPaperCommand;
import com.cufufy.amp.plugin.bridge.LoginBridgeManager;
import com.cufufy.amp.plugin.config.PluginSettings;
import com.cufufy.amp.plugin.config.SettingsLoader;
import com.cufufy.amp.plugin.core.GlobalPathBootstrap;
import com.cufufy.amp.plugin.mod.ModpackHostService;

public final class AutoModpackPlugin extends JavaPlugin {
    private ModpackHostService hostService;
    private LoginBridgeManager loginBridge;

    @Override
    public void onEnable() {
        try {
            Path pluginDirectory = getDataFolder().toPath();
            GlobalPathBootstrap.redirect(pluginDirectory);

            PluginSettings settings = loadSettings(pluginDirectory);
            hostService = new ModpackHostService(settings, pluginDirectory, getLogger());

            ensureAutoModpackModPresent(hostService.modsDirectory());
            hostService.start();

            loginBridge = new LoginBridgeManager(this, settings, getLogger());
            loginBridge.start();

            AutoModpackCommand command = new AutoModpackCommand(this, hostService);
            registerCommands(command);

            getLogger().info("AutoModpack plugin initialized. Mods directory: " + hostService.modsDirectory());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize AutoModpack plugin", e);
            if (loginBridge != null) {
                loginBridge.close();
            }
            if (hostService != null) {
                hostService.close();
            }
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (hostService != null) {
            hostService.close();
        }
        if (loginBridge != null) {
            loginBridge.close();
        }
        getLogger().info("AutoModpack plugin stopped");
    }

    private PluginSettings loadSettings(Path pluginDirectory) throws IOException {
        try (InputStream resource = getResource("config.yml")) {
            Path configFile = pluginDirectory.resolve("config.yml");
            return SettingsLoader.load(configFile, resource);
        }
    }

    private void ensureAutoModpackModPresent(Path modsDirectory) throws IOException {
        Files.createDirectories(modsDirectory);

        try (Stream<Path> entries = Files.list(modsDirectory)) {
            boolean hasMod = entries
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
                    .anyMatch(name -> name.endsWith(".jar") && name.contains("automodpack"));

            if (!hasMod) {
                throw new IOException("AutoModpack mod jar not found in " + modsDirectory);
            }
        }
    }

    private void registerCommands(AutoModpackCommand command) {
        Command paperCommand = new AutoModpackPaperCommand(this, command);
        try {
            Method registerCommand = JavaPlugin.class.getMethod("registerCommand", String.class, Command.class);
            registerCommand.invoke(this, paperCommand.getName(), paperCommand);
            return;
        } catch (NoSuchMethodException ignored) {
            // Legacy Bukkit API - fall back to plugin.yml registration.
        } catch (IllegalAccessException | InvocationTargetException e) {
            getLogger().log(Level.SEVERE, "Unable to register /automodpack command via Paper API", e);
            return;
        }

        PluginCommand pluginCommand = getCommand(paperCommand.getName());
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        } else {
            getLogger().warning("Unable to register /automodpack command - missing plugin.yml definition");
        }
    }

}
