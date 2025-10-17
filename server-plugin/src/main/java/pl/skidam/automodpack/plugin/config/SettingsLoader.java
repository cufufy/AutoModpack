package pl.skidam.automodpack.plugin.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SettingsLoader {
    private SettingsLoader() {
    }

    public static PluginSettings load(Path configFile, InputStream defaultConfigStream) throws IOException {
        if (Files.notExists(configFile)) {
            if (defaultConfigStream == null) {
                throw new IOException("Missing default configuration");
            }
            Files.createDirectories(configFile.getParent());
            Files.copy(defaultConfigStream, configFile);
        }

        List<String> lines = Files.readAllLines(configFile, StandardCharsets.UTF_8);
        PluginSettings settings = new PluginSettings();
        PluginSettings.HostSettings host = settings.host();
        List<String> acceptedLoaders = new ArrayList<>();

        boolean inAcceptedLoaders = false;
        boolean inHostSection = false;

        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            if (!line.startsWith(" ")) {
                inHostSection = false;
                inAcceptedLoaders = false;
            }

            if (trimmed.equals("accepted-loaders:")) {
                inAcceptedLoaders = true;
                acceptedLoaders.clear();
                continue;
            }

            if (inAcceptedLoaders) {
                if (trimmed.startsWith("-")) {
                    acceptedLoaders.add(stripQuotes(trimmed.substring(1).trim()));
                    continue;
                } else {
                    inAcceptedLoaders = false;
                }
            }

            if (trimmed.equals("server-host:")) {
                inHostSection = true;
                continue;
            }

            if (inHostSection && line.startsWith("  ")) {
                parseHostEntry(host, trimmed);
                continue;
            }

            parseTopLevel(settings, trimmed);
        }

        settings.setAcceptedLoaders(acceptedLoaders);
        return settings;
    }

    private static void parseTopLevel(PluginSettings settings, String entry) {
        int colon = entry.indexOf(':');
        if (colon <= 0) {
            return;
        }

        String key = entry.substring(0, colon).trim();
        String value = stripQuotes(entry.substring(colon + 1).trim());

        switch (key) {
            case "force-mod" -> settings.setForceMod(asBoolean(value, false));
            case "nag-missing-mod" -> settings.setNagMissingMod(asBoolean(value, true));
            case "nag-message" -> settings.setNagMessage(value);
            case "nag-link-text" -> settings.setNagLinkText(value);
            case "nag-link-url" -> settings.setNagLinkUrl(value);
            case "modpack-name" -> settings.setModpackName(value);
            case "automodpack-version" -> settings.setAutomodpackVersion(value);
            case "minecraft-version" -> settings.setMinecraftVersion(value);
            default -> {
            }
        }
    }

    private static void parseHostEntry(PluginSettings.HostSettings host, String entry) {
        int colon = entry.indexOf(':');
        if (colon <= 0) {
            return;
        }
        String key = entry.substring(0, colon).trim();
        String value = stripQuotes(entry.substring(colon + 1).trim());

        switch (key) {
            case "bind-address" -> host.setBindAddress(value);
            case "bind-port" -> host.setBindPort(asInt(value, -1));
            case "address-to-send" -> host.setAddressToSend(value);
            case "port-to-send" -> host.setPortToSend(asInt(value, -1));
            case "disable-internal-tls" -> host.setDisableInternalTls(asBoolean(value, false));
            case "bandwidth-limit" -> host.setBandwidthLimit(asInt(value, 0));
            default -> {
            }
        }
    }

    private static boolean asBoolean(String value, boolean fallback) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        return fallback;
    }

    private static int asInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
