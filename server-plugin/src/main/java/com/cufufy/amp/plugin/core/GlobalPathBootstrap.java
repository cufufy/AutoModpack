package com.cufufy.amp.plugin.core;

import java.nio.file.Path;

import com.cufufy.amp.core.GlobalVariables;

public final class GlobalPathBootstrap {
    private GlobalPathBootstrap() {
    }

    public static void redirect(Path baseDirectory) {
        Path base = baseDirectory.toAbsolutePath();

        GlobalVariables.automodpackDir = base;
        GlobalVariables.MODS_DIR = base.resolve("mods");

        GlobalVariables.hostModpackDir = base.resolve("host-modpack");
        GlobalVariables.hostContentModpackDir = GlobalVariables.hostModpackDir.resolve("main");
        GlobalVariables.hostModpackContentFile = GlobalVariables.hostModpackDir.resolve("automodpack-content.json");

        GlobalVariables.serverConfigFile = base.resolve("automodpack-server.json");
        GlobalVariables.serverCoreConfigFile = base.resolve("automodpack-core.json");

        GlobalVariables.privateDir = base.resolve(".private");
        GlobalVariables.serverSecretsFile = GlobalVariables.privateDir.resolve("automodpack-secrets.json");
        GlobalVariables.knownHostsFile = GlobalVariables.privateDir.resolve("automodpack-known-hosts.json");
        GlobalVariables.serverCertFile = GlobalVariables.privateDir.resolve("cert.crt");
        GlobalVariables.serverPrivateKeyFile = GlobalVariables.privateDir.resolve("key.pem");
        GlobalVariables.clientSecretsFile = GlobalVariables.privateDir.resolve("automodpack-client-secrets.json");

        GlobalVariables.modpackContentTempFile = base.resolve("automodpack-content.json.temp");
        GlobalVariables.clientConfigFile = base.resolve("automodpack-client.json");
        GlobalVariables.modpacksDir = base.resolve("modpacks");
    }
}
