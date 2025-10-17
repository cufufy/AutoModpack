package com.cufufy.amp.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.cufufy.amp.core.config.Jsons;
import com.cufufy.amp.core.loader.*;
import com.cufufy.amp.core.modpack.ModpackExecutor;
import com.cufufy.amp.core.protocol.netty.NettyServer;

import java.nio.file.Path;

public class GlobalVariables {
    public static final Logger LOGGER = LogManager.getLogger("AutoModpack");
    public static final String MOD_ID = "automodpack";
    public static Boolean DEBUG = false;
    public static Boolean preload;
    public static String MC_VERSION;
    public static String AM_VERSION;
    public static String LOADER_VERSION;
    public static String LOADER;
    public static LoaderManagerService LOADER_MANAGER = new NullLoaderManager();
    public static ModpackLoaderService MODPACK_LOADER = new NullModpackLoader();
    public static GameCallService GAME_CALL = new NullGameCall();
    public static Path THIZ_JAR;
    public static Path MODS_DIR;
    public static ModpackExecutor modpackExecutor;
    public static NettyServer hostServer;
    public static Jsons.ServerConfigFieldsV2 serverConfig;
    public static Jsons.ClientConfigFieldsV2 clientConfig;
    public static Jsons.KnownHostsFields knownHosts;
    public static Path automodpackDir = Path.of("automodpack");
    public static Path hostModpackDir = automodpackDir.resolve("host-modpack");
    // TODO More server modpacks
    // Main - required
    // Addons - optional addon packs
    // Switches - optional or required packs, chosen by the player, only one can be installed at a time
    public static Path hostContentModpackDir = hostModpackDir.resolve("main");
    public static Path hostModpackContentFile = hostModpackDir.resolve("automodpack-content.json");
    public static Path serverConfigFile = automodpackDir.resolve("automodpack-server.json");
    public static Path serverCoreConfigFile = automodpackDir.resolve("automodpack-core.json");
    public static Path privateDir = automodpackDir.resolve(".private");
    public static Path serverSecretsFile = privateDir.resolve("automodpack-secrets.json");
    public static Path knownHostsFile = privateDir.resolve("automodpack-known-hosts.json");
    public static Path serverCertFile = privateDir.resolve("cert.crt");
    public static Path serverPrivateKeyFile = privateDir.resolve("key.pem");


    // Client
    public static Path modpackContentTempFile = automodpackDir.resolve("automodpack-content.json.temp");
    public static Path clientConfigFile = automodpackDir.resolve("automodpack-client.json");
    public static Path clientSecretsFile = privateDir.resolve("automodpack-client-secrets.json");
    public static Path modpacksDir = automodpackDir.resolve("modpacks");

    public static final String clientConfigFileOverrideResource = "overrides-automodpack-client.json";
    public static String clientConfigOverride; // read from inside a jar file on preload, used instead of clientConfigFile if exists

    public static Path selectedModpackDir;
}
