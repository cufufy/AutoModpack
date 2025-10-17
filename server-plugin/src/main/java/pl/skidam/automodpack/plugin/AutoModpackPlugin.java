package pl.skidam.automodpack.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import pl.skidam.automodpack.plugin.command.AutoModpackCommand;
import pl.skidam.automodpack.plugin.command.AutoModpackPaperCommand;
import pl.skidam.automodpack.plugin.bridge.LoginBridgeManager;
import pl.skidam.automodpack.plugin.config.PluginSettings;
import pl.skidam.automodpack.plugin.config.SettingsLoader;
import pl.skidam.automodpack.plugin.core.GlobalPathBootstrap;
import pl.skidam.automodpack.plugin.mod.ModpackHostService;

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
            hostService.start();

            loginBridge = new LoginBridgeManager(this, settings, getLogger());
            loginBridge.start();

            AutoModpackCommand command = new AutoModpackCommand(this, hostService);
            registerCommands(command);

            getLogger().info("AutoModpack plugin initialized. Mods directory: " + hostService.modsDirectory());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize AutoModpack plugin", e);
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
