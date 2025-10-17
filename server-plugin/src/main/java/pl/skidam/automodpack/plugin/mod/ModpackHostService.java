package pl.skidam.automodpack.plugin.mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import pl.skidam.automodpack.plugin.config.PluginSettings;
import pl.skidam.automodpack.plugin.core.PluginGameCall;
import pl.skidam.automodpack.plugin.core.SpigotLoaderManager;
import pl.skidam.automodpack_core.GlobalVariables;
import pl.skidam.automodpack_core.config.ConfigTools;
import pl.skidam.automodpack_core.config.Jsons;
import pl.skidam.automodpack_core.modpack.ModpackExecutor;
import pl.skidam.automodpack_core.protocol.netty.NettyServer;
import pl.skidam.automodpack_core.loader.LoaderManagerService;

public final class ModpackHostService implements AutoCloseable {
    private final PluginSettings settings;
    private final Path pluginDir;
    private final Path modsSourceDir;
    private final Path configMirrorDir;
    private final Logger logger;
    private ModRepositoryWatcher modWatcher;
    private ConfigMirror configMirror;

    public ModpackHostService(PluginSettings settings, Path pluginDir, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.pluginDir = Objects.requireNonNull(pluginDir, "pluginDir");
        this.modsSourceDir = pluginDir.resolve("mods");
        this.configMirrorDir = pluginDir.resolve("configs");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void start() throws IOException {
        Files.createDirectories(pluginDir);
        Files.createDirectories(modsSourceDir);
        Files.createDirectories(configMirrorDir);

        initializeGlobalState();
        reloadServerConfig();
        startHostInfrastructure();
        startWatchers();
    }

    private void initializeGlobalState() {
        GlobalVariables.AM_VERSION = settings.automodpackVersion();
        GlobalVariables.MC_VERSION = settings.minecraftVersion();
        String primaryLoader = settings.acceptedLoaders().isEmpty() ? "fabric" : settings.acceptedLoaders().get(0);
        GlobalVariables.LOADER = primaryLoader;
        GlobalVariables.LOADER_VERSION = "plugin";
        GlobalVariables.LOADER_MANAGER = new SpigotLoaderManager(LoaderManagerService.ModPlatform.FABRIC, "plugin");
        GlobalVariables.GAME_CALL = new PluginGameCall();
    }

    private void reloadServerConfig() {
        Jsons.ServerConfigFieldsV2 serverConfig = ConfigTools.load(GlobalVariables.serverConfigFile, Jsons.ServerConfigFieldsV2.class);
        if (serverConfig == null) {
            serverConfig = new Jsons.ServerConfigFieldsV2();
        }

        serverConfig.modpackName = settings.modpackName();
        serverConfig.modpackHost = true;
        serverConfig.generateModpackOnStart = true;
        serverConfig.requireAutoModpackOnClient = settings.forceMod();
        serverConfig.nagUnModdedClients = settings.nagMissingMod();
        serverConfig.nagMessage = settings.nagMessage();
        serverConfig.nagClickableMessage = settings.nagLinkText();
        serverConfig.nagClickableLink = settings.nagLinkUrl();
        serverConfig.acceptedLoaders = settings.acceptedLoaders().isEmpty()
                ? List.of("fabric")
                : List.copyOf(settings.acceptedLoaders());
        serverConfig.bindAddress = settings.host().bindAddress();
        serverConfig.bindPort = settings.host().bindPort();
        serverConfig.addressToSend = settings.host().addressToSend();
        serverConfig.portToSend = settings.host().portToSend();
        serverConfig.disableInternalTLS = settings.host().disableInternalTls();
        serverConfig.bandwidthLimit = settings.host().bandwidthLimit();
        serverConfig.autoExcludeServerSideMods = true;
        serverConfig.autoExcludeUnnecessaryFiles = true;
        serverConfig.updateIpsOnEveryStart = false;
        serverConfig.selfUpdater = false;

        ConfigTools.save(GlobalVariables.serverConfigFile, serverConfig);
        GlobalVariables.serverConfig = serverConfig;

        Jsons.ServerCoreConfigFields coreConfig = ConfigTools.load(GlobalVariables.serverCoreConfigFile, Jsons.ServerCoreConfigFields.class);
        if (coreConfig == null) {
            coreConfig = new Jsons.ServerCoreConfigFields();
        }
        coreConfig.automodpackVersion = settings.automodpackVersion();
        coreConfig.loader = GlobalVariables.LOADER;
        coreConfig.loaderVersion = GlobalVariables.LOADER_VERSION;
        coreConfig.mcVersion = settings.minecraftVersion();
        ConfigTools.save(GlobalVariables.serverCoreConfigFile, coreConfig);
    }

    private void startHostInfrastructure() throws IOException {
        GlobalVariables.modpackExecutor = new ModpackExecutor();
        GlobalVariables.hostServer = new NettyServer();

        if (!GlobalVariables.modpackExecutor.generateNew()) {
            logger.warning("Initial modpack generation reported no content. Check the mods directory: " + modsSourceDir);
        }

        GlobalVariables.hostServer.start();

        if (!GlobalVariables.hostServer.isRunning()) {
            throw new IOException("Failed to start AutoModpack host server");
        }
    }

    private void startWatchers() throws IOException {
        Path hostModsDir = GlobalVariables.hostModpackDir.resolve("main").resolve("mods");
        Path hostConfigDir = GlobalVariables.hostModpackDir.resolve("main").resolve("config");

        modWatcher = new ModRepositoryWatcher(modsSourceDir, hostModsDir, this::handleModpackUpdate);
        modWatcher.initialize();

        configMirror = new ConfigMirror(hostConfigDir, configMirrorDir);
        configMirror.synchronizeAsync();
    }

    private void handleModpackUpdate(Instant timestamp) {
        synchronized (this) {
            if (GlobalVariables.modpackExecutor.isGenerating()) {
                return;
            }

            boolean generated = GlobalVariables.modpackExecutor.generateNew();
            if (!generated) {
                logger.warning("Modpack regeneration completed without output");
            }
            if (configMirror != null) {
                configMirror.synchronizeAsync();
            }
        }
    }

    @Override
    public void close() {
        if (modWatcher != null) {
            modWatcher.close();
        }
        if (configMirror != null) {
            configMirror.close();
        }
        if (GlobalVariables.hostServer != null && GlobalVariables.hostServer.isRunning()) {
            GlobalVariables.hostServer.stop();
        }
        if (GlobalVariables.modpackExecutor != null) {
            GlobalVariables.modpackExecutor.stop();
        }
    }

    public Path modsDirectory() {
        return modsSourceDir;
    }

    public Path configMirrorDirectory() {
        return configMirrorDir;
    }
}
