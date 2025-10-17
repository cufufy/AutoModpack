package com.cufufy.amp.plugin.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettingsLoaderTest {
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("automodpack-settings-test");
    }

    @AfterEach
    void tearDown() throws IOException {
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
    void loadsExistingConfigurationValues() throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
                force-mod: true
                nag-missing-mod: false
                nag-message: "Install the mod"
                nag-link-text: "Download"
                nag-link-url: "https://example.invalid"
                accepted-loaders:
                  - fabric
                  - forge
                modpack-name: "ExamplePack"
                automodpack-version: "4.0.0"
                minecraft-version: "1.20.4"
                server-host:
                  bind-address: "0.0.0.0"
                  bind-port: 25565
                  address-to-send: "mc.example.invalid"
                  port-to-send: 25566
                  disable-internal-tls: true
                  bandwidth-limit: 2048
                """);

        PluginSettings settings = SettingsLoader.load(configFile, null);

        assertTrue(settings.forceMod());
        assertFalse(settings.nagMissingMod());
        assertEquals("Install the mod", settings.nagMessage());
        assertEquals("Download", settings.nagLinkText());
        assertEquals("https://example.invalid", settings.nagLinkUrl());
        assertEquals(List.of("fabric", "forge"), settings.acceptedLoaders());
        assertEquals("ExamplePack", settings.modpackName());
        assertEquals("4.0.0", settings.automodpackVersion());
        assertEquals("1.20.4", settings.minecraftVersion());

        PluginSettings.HostSettings host = settings.host();
        assertEquals("0.0.0.0", host.bindAddress());
        assertEquals(25565, host.bindPort());
        assertEquals("mc.example.invalid", host.addressToSend());
        assertEquals(25566, host.portToSend());
        assertTrue(host.disableInternalTls());
        assertEquals(2048, host.bandwidthLimit());
    }

    @Test
    void createsConfigFromDefaultsWhenMissing() throws IOException {
        Path configFile = tempDir.resolve("config.yml");
        byte[] defaults = """
                force-mod: false
                nag-missing-mod: true
                modpack-name: "ExamplePack"
                accepted-loaders:
                  - fabric
                """.getBytes(StandardCharsets.UTF_8);

        PluginSettings settings = SettingsLoader.load(configFile, new ByteArrayInputStream(defaults));

        assertTrue(Files.exists(configFile));
        assertEquals("ExamplePack", settings.modpackName());
        assertEquals(List.of("fabric"), settings.acceptedLoaders());
    }
}
