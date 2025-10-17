package com.cufufy.amp.plugin.mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

import com.cufufy.amp.plugin.config.PluginSettings;
import com.cufufy.amp.plugin.core.PluginGameCall;
import com.cufufy.amp.plugin.core.SpigotLoaderManager;
import com.cufufy.amp.core.GlobalVariables;
import com.cufufy.amp.core.config.ConfigTools;
import com.cufufy.amp.core.config.Jsons;
import com.cufufy.amp.core.modpack.ModpackExecutor;
import com.cufufy.amp.core.protocol.netty.NettyServer;
import com.cufufy.amp.core.loader.LoaderManagerService;

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

        boolean started = false;
        try {
            startHostInfrastructure();
            startWatchers();
            started = true;
        } finally {
            if (!started) {
                close();
            }
        }
    }

    private void initializeGlobalState() {
        GlobalVariables.AM_VERSION = settings.automodpackVersion();
        GlobalVariables.MC_VERSION = settings.minecraftVersion();
        String primaryLoader = settings.acceptedLoaders().isEmpty() ? "fabric" : settings.acceptedLoaders().get(0);
        String normalizedLoader = primaryLoader == null ? "fabric" : primaryLoader.toLowerCase(Locale.ROOT);
        GlobalVariables.LOADER = normalizedLoader;
        GlobalVariables.LOADER_VERSION = "plugin";
        GlobalVariables.LOADER_MANAGER = new SpigotLoaderManager(resolvePlatform(normalizedLoader), "plugin");
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
            if (!GlobalVariables.hostServer.shouldHost()) {
                logger.warning("AutoModpack host server was not started because there is no modpack content to host yet.");
            } else {
                throw new IOException("Failed to start AutoModpack host server");
            }
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

    public synchronized boolean regenerateModpack() {
        if (GlobalVariables.modpackExecutor == null) {
            return false;
        }

        return GlobalVariables.modpackExecutor.generateNew();
    }

    public synchronized boolean isHostRunning() {
        return GlobalVariables.hostServer != null && GlobalVariables.hostServer.isRunning();
    }

    public synchronized boolean startHost() {
        if (GlobalVariables.hostServer == null || GlobalVariables.hostServer.isRunning()) {
            return false;
        }

        GlobalVariables.hostServer.start();
        return GlobalVariables.hostServer.isRunning();
    }

    public synchronized boolean stopHost() {
        if (GlobalVariables.hostServer == null || !GlobalVariables.hostServer.isRunning()) {
            return false;
        }

        return GlobalVariables.hostServer.stop();
    }

    public synchronized boolean restartHost() {
        if (GlobalVariables.hostServer == null) {
            return false;
        }

        boolean wasRunning = GlobalVariables.hostServer.isRunning();
        if (wasRunning && !GlobalVariables.hostServer.stop()) {
            return false;
        }

        GlobalVariables.hostServer.start();
        return GlobalVariables.hostServer.isRunning();
    }

    public synchronized List<String> activeConnectionSecrets() {
        if (GlobalVariables.hostServer == null) {
            return List.of();
        }

        return Collections.unmodifiableList(new ArrayList<>(GlobalVariables.hostServer.getConnections().values()));
    }

    public synchronized String certificateFingerprint() {
        if (GlobalVariables.hostServer == null) {
            return null;
        }

        return GlobalVariables.hostServer.getCertificateFingerprint();
    }

    public synchronized boolean reloadServerConfigFromDisk() {
        Jsons.ServerConfigFieldsV2 reloaded = ConfigTools.load(GlobalVariables.serverConfigFile, Jsons.ServerConfigFieldsV2.class);
        if (reloaded == null) {
            return false;
        }

        GlobalVariables.serverConfig = reloaded;
        return true;
    }

    private LoaderManagerService.ModPlatform resolvePlatform(String loaderId) {
        if (loaderId == null) {
            return LoaderManagerService.ModPlatform.FABRIC;
        }
        return switch (loaderId.toLowerCase(Locale.ROOT)) {
            case "quilt" -> LoaderManagerService.ModPlatform.QUILT;
            case "forge" -> LoaderManagerService.ModPlatform.FORGE;
            case "neoforge" -> LoaderManagerService.ModPlatform.NEOFORGE;
            default -> LoaderManagerService.ModPlatform.FABRIC;
        };
    }
}
