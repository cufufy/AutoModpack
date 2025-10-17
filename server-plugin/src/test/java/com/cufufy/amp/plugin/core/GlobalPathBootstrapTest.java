package com.cufufy.amp.plugin.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.cufufy.amp.core.GlobalVariables;

class GlobalPathBootstrapTest {
    private Path tempDir;

    private Path originalAutomodpackDir;
    private Path originalHostModpackDir;
    private Path originalHostContentDir;
    private Path originalHostContentFile;
    private Path originalServerConfig;
    private Path originalServerCoreConfig;
    private Path originalPrivateDir;
    private Path originalServerSecrets;
    private Path originalKnownHosts;
    private Path originalServerCert;
    private Path originalServerKey;
    private Path originalClientSecrets;
    private Path originalModsDir;
    private Path originalModpackTempFile;
    private Path originalClientConfig;
    private Path originalModpacksDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("automodpack-bootstrap-test");

        originalAutomodpackDir = GlobalVariables.automodpackDir;
        originalHostModpackDir = GlobalVariables.hostModpackDir;
        originalHostContentDir = GlobalVariables.hostContentModpackDir;
        originalHostContentFile = GlobalVariables.hostModpackContentFile;
        originalServerConfig = GlobalVariables.serverConfigFile;
        originalServerCoreConfig = GlobalVariables.serverCoreConfigFile;
        originalPrivateDir = GlobalVariables.privateDir;
        originalServerSecrets = GlobalVariables.serverSecretsFile;
        originalKnownHosts = GlobalVariables.knownHostsFile;
        originalServerCert = GlobalVariables.serverCertFile;
        originalServerKey = GlobalVariables.serverPrivateKeyFile;
        originalClientSecrets = GlobalVariables.clientSecretsFile;
        originalModsDir = GlobalVariables.MODS_DIR;
        originalModpackTempFile = GlobalVariables.modpackContentTempFile;
        originalClientConfig = GlobalVariables.clientConfigFile;
        originalModpacksDir = GlobalVariables.modpacksDir;
    }

    @AfterEach
    void tearDown() throws IOException {
        GlobalVariables.automodpackDir = originalAutomodpackDir;
        GlobalVariables.hostModpackDir = originalHostModpackDir;
        GlobalVariables.hostContentModpackDir = originalHostContentDir;
        GlobalVariables.hostModpackContentFile = originalHostContentFile;
        GlobalVariables.serverConfigFile = originalServerConfig;
        GlobalVariables.serverCoreConfigFile = originalServerCoreConfig;
        GlobalVariables.privateDir = originalPrivateDir;
        GlobalVariables.serverSecretsFile = originalServerSecrets;
        GlobalVariables.knownHostsFile = originalKnownHosts;
        GlobalVariables.serverCertFile = originalServerCert;
        GlobalVariables.serverPrivateKeyFile = originalServerKey;
        GlobalVariables.clientSecretsFile = originalClientSecrets;
        GlobalVariables.MODS_DIR = originalModsDir;
        GlobalVariables.modpackContentTempFile = originalModpackTempFile;
        GlobalVariables.clientConfigFile = originalClientConfig;
        GlobalVariables.modpacksDir = originalModpacksDir;

        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    @Test
    void redirectUpdatesGlobalVariablesPaths() {
        GlobalPathBootstrap.redirect(tempDir);

        assertEquals(tempDir.toAbsolutePath(), GlobalVariables.automodpackDir);
        assertEquals(tempDir.toAbsolutePath().resolve("mods"), GlobalVariables.MODS_DIR);

        Path expectedHostDir = tempDir.toAbsolutePath().resolve("host-modpack");
        assertEquals(expectedHostDir, GlobalVariables.hostModpackDir);
        assertEquals(expectedHostDir.resolve("main"), GlobalVariables.hostContentModpackDir);
        assertEquals(expectedHostDir.resolve("automodpack-content.json"), GlobalVariables.hostModpackContentFile);

        assertEquals(tempDir.toAbsolutePath().resolve("automodpack-server.json"), GlobalVariables.serverConfigFile);
        assertEquals(tempDir.toAbsolutePath().resolve("automodpack-core.json"), GlobalVariables.serverCoreConfigFile);

        Path expectedPrivate = tempDir.toAbsolutePath().resolve(".private");
        assertEquals(expectedPrivate, GlobalVariables.privateDir);
        assertEquals(expectedPrivate.resolve("automodpack-secrets.json"), GlobalVariables.serverSecretsFile);
        assertEquals(expectedPrivate.resolve("automodpack-known-hosts.json"), GlobalVariables.knownHostsFile);
        assertEquals(expectedPrivate.resolve("cert.crt"), GlobalVariables.serverCertFile);
        assertEquals(expectedPrivate.resolve("key.pem"), GlobalVariables.serverPrivateKeyFile);
        assertEquals(expectedPrivate.resolve("automodpack-client-secrets.json"), GlobalVariables.clientSecretsFile);

        assertEquals(tempDir.toAbsolutePath().resolve("automodpack-content.json.temp"),
                GlobalVariables.modpackContentTempFile);
        assertEquals(tempDir.toAbsolutePath().resolve("automodpack-client.json"), GlobalVariables.clientConfigFile);
        assertEquals(tempDir.toAbsolutePath().resolve("modpacks"), GlobalVariables.modpacksDir);
    }
}
