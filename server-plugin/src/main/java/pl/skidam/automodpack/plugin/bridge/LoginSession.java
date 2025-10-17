package pl.skidam.automodpack.plugin.bridge;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import pl.skidam.automodpack.plugin.config.PluginSettings;
import pl.skidam.automodpack.plugin.bridge.NmsReflection.FriendlyResponse;
import pl.skidam.automodpack.plugin.bridge.LoginBridgeManager.Handshake;
import pl.skidam.automodpack_core.GlobalVariables;

final class LoginSession {
    private static final int HANDSHAKE_QUERY_ID = -100;
    private static final int DATA_QUERY_ID = -101;
    private static final Pattern SEMVER_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+");

    private final Object loginHandler;
    private final PluginSettings settings;
    private final Logger logger;
    private final BiConsumer<UUID, Boolean> completionCallback;
    private final Consumer<UUID> missingCallback;

    private boolean handshakeSent;
    private boolean dataSent;
    private boolean closed;

    LoginSession(Object loginHandler, PluginSettings settings, Logger logger,
                 BiConsumer<UUID, Boolean> completionCallback,
                 Consumer<UUID> missingCallback) {
        this.loginHandler = loginHandler;
        this.settings = settings;
        this.logger = logger;
        this.completionCallback = completionCallback;
        this.missingCallback = missingCallback;
    }

    void tick() {
        if (closed) {
            return;
        }

        Enum<?> state = NmsReflection.loginState(loginHandler);
        if (!handshakeSent && state != null) {
            String name = state.name().toUpperCase(Locale.ROOT);
            if (name.contains("NEGOTIATING") || name.contains("VERIFYING") || name.contains("READY")) {
                sendHandshake();
            }
        }
    }

    boolean handlePacket(Object packet) {
        if (closed) {
            return false;
        }
        FriendlyResponse response = NmsReflection.parseQueryResponse(packet);
        if (response == FriendlyResponse.NOT_HANDLED) {
            return false;
        }

        if (response.id() == HANDSHAKE_QUERY_ID) {
            handleHandshakeResponse(response);
            return true;
        }
        if (response.id() == DATA_QUERY_ID) {
            handleDataResponse(response);
            return true;
        }
        return false;
    }

    private void sendHandshake() {
        handshakeSent = true;
        Object buf = NmsReflection.createFriendlyByteBuf();
        String payload = LoginBridgeManager.handshakeJson(settings);
        NmsReflection.writeUtf(buf, payload);
        Object identifier = NmsReflection.createResourceLocation("automodpack", "handshake");
        Object packet = NmsReflection.createCustomQueryPacket(HANDSHAKE_QUERY_ID, identifier, buf);
        if (packet == null) {
            logger.severe("Unable to create AutoModpack handshake packet");
            closed = true;
            return;
        }
        NmsReflection.sendPacket(loginHandler, packet);
    }

    private void handleHandshakeResponse(FriendlyResponse response) {
        UUID profileId = resolveProfileId();
        String playerName = resolveProfileName();

        if (!response.understood()) {
            logger.log(Level.WARNING, () -> playerName + " has not installed AutoModpack.");
            if (settings.forceMod()) {
                disconnect(requiredModMessage());
                closed = true;
            }
            missingCallback.accept(profileId);
            completionCallback.accept(profileId, false);
            return;
        }

        String json = NmsReflection.readUtf(response.buffer());
        if (json == null) {
            logger.log(Level.WARNING, () -> "Invalid AutoModpack handshake from " + playerName);
            disconnect("Invalid AutoModpack handshake from client.");
            closed = true;
            completionCallback.accept(profileId, false);
            return;
        }

        Handshake clientHandshake = parseHandshake(json);
        if (clientHandshake == null) {
            logger.log(Level.WARNING, () -> "Malformed AutoModpack handshake payload from " + playerName);
            disconnect("Malformed AutoModpack handshake payload.");
            closed = true;
            completionCallback.accept(profileId, false);
            return;
        }

        String validationError = validateHandshake(clientHandshake);
        if (validationError != null) {
            logger.log(Level.WARNING, () -> "AutoModpack handshake rejected for " + playerName + ": " + validationError);
            disconnect(validationError);
            closed = true;
            completionCallback.accept(profileId, false);
            return;
        }

        logger.log(Level.INFO, () -> playerName + " has installed AutoModpack.");
        sendData(profileId, playerName);
    }

    private void handleDataResponse(FriendlyResponse response) {
        if (!response.understood() || response.buffer() == null) {
            return;
        }

        String marker = NmsReflection.readUtf(response.buffer());
        if (marker == null) {
            return;
        }

        UUID profileId = resolveProfileId();
        String playerName = resolveProfileName();

        if ("true".equalsIgnoreCase(marker)) {
            logger.log(Level.WARNING, () -> playerName + " has not installed modpack. Certificate fingerprint: "
                    + (GlobalVariables.hostServer != null ? GlobalVariables.hostServer.getCertificateFingerprint() : "unknown"));
            disconnect("[AutoModpack] Install/Update modpack to join");
            closed = true;
            completionCallback.accept(profileId, false);
        } else if ("false".equalsIgnoreCase(marker)) {
            logger.log(Level.INFO, () -> playerName + " has installed whole modpack");
            completionCallback.accept(profileId, true);
        } else {
            logger.severe("[AutoModpack] Host server error for " + playerName
                    + ". AutoModpack host server is down or server is not configured correctly");
            disconnect("[AutoModpack] Host server error. Please contact server administrator to check the server logs!");
            closed = true;
            completionCallback.accept(profileId, false);
            logHostMisconfiguration();
        }
    }

    private void sendData(UUID profileId, String playerName) {
        if (dataSent) {
            return;
        }
        dataSent = true;

        if (GlobalVariables.hostServer == null || !GlobalVariables.hostServer.isRunning()) {
            logger.log(Level.WARNING, () -> "AutoModpack host server is not running - unable to deliver modpack to " + playerName);
            completionCallback.accept(profileId, false);
            return;
        }
        if (GlobalVariables.modpackExecutor == null) {
            completionCallback.accept(profileId, false);
            return;
        }
        if (GlobalVariables.modpackExecutor.isGenerating()) {
            disconnect("AutoModpack is still generating the modpack. Please try again shortly.");
            completionCallback.accept(profileId, false);
            closed = true;
            return;
        }

        Object buf = NmsReflection.createFriendlyByteBuf();
        String payload = LoginBridgeManager.dataJson(settings, profileId);
        NmsReflection.writeUtf(buf, payload);
        Object identifier = NmsReflection.createResourceLocation("automodpack", "data");
        Object packet = NmsReflection.createCustomQueryPacket(DATA_QUERY_ID, identifier, buf);
        if (packet == null) {
            logger.severe("Failed to build AutoModpack data packet");
            completionCallback.accept(profileId, false);
            return;
        }
        NmsReflection.sendPacket(loginHandler, packet);
        logger.log(Level.INFO, () -> "Dispatched AutoModpack host data to " + playerName);
    }

    private String validateHandshake(Handshake handshake) {
        if (handshake == null) {
            return "Malformed AutoModpack handshake payload.";
        }

        if (!isLoaderAccepted(handshake.loaders())) {
            return requiredModMessage();
        }

        if (!Objects.equals(handshake.amVersion(), settings.automodpackVersion())) {
            if (isClientVersionHigher(handshake.amVersion(), settings.automodpackVersion())) {
                return "You are using a more recent version of AutoModpack than the server. Please contact the server administrator to update the AutoModpack mod.";
            }
            return "AutoModpack version mismatch! Install " + settings.automodpackVersion()
                    + " version of AutoModpack mod for " + loaderDisplayName() + " to play on this server!";
        }

        return null;
    }

    private Handshake parseHandshake(String json) {
        if (json == null) {
            return null;
        }
        List<String> loaders = parseStringArray(json, "loaders");
        String amVersion = extractString(json, "amVersion");
        String mcVersion = extractString(json, "mcVersion");
        if (amVersion == null || mcVersion == null) {
            return null;
        }
        return new Handshake(loaders, amVersion, mcVersion);
    }

    private List<String> parseStringArray(String json, String key) {
        int keyIndex = json.indexOf('"' + key + '"');
        if (keyIndex < 0) {
            return List.of();
        }
        int arrayStart = json.indexOf('[', keyIndex);
        int arrayEnd = json.indexOf(']', arrayStart);
        if (arrayStart < 0 || arrayEnd < 0 || arrayEnd <= arrayStart) {
            return List.of();
        }
        String content = json.substring(arrayStart + 1, arrayEnd).trim();
        if (content.isEmpty()) {
            return List.of();
        }
        String[] parts = content.split(",");
        List<String> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            String value = unquote(part.trim());
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private String extractString(String json, String key) {
        int keyIndex = json.indexOf('"' + key + '"');
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex);
        int startQuote = json.indexOf('"', colon + 1);
        int endQuote = json.indexOf('"', startQuote + 1);
        if (colon < 0 || startQuote < 0 || endQuote < 0) {
            return null;
        }
        return unescape(json.substring(startQuote + 1, endQuote));
    }

    private String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return unescape(value);
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private UUID resolveProfileId() {
        Object profile = NmsReflection.gameProfile(loginHandler);
        if (profile == null) {
            return new UUID(0L, 0L);
        }

        UUID uuid = extractUuid(profile, "getId");
        if (uuid == null) {
            uuid = extractUuid(profile, "id");
        }
        if (uuid != null) {
            return uuid;
        }

        String name = resolveProfileName(profile);
        if (!"unknown".equals(name)) {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        }

        return new UUID(0L, 0L);
    }

    private String resolveProfileName() {
        Object profile = NmsReflection.gameProfile(loginHandler);
        return resolveProfileName(profile);
    }

    private String resolveProfileName(Object profile) {
        if (profile == null) {
            return "unknown";
        }
        String name = invokeStringMethod(profile, "getName");
        if (name == null || name.isBlank()) {
            name = invokeStringMethod(profile, "name");
        }
        return (name == null || name.isBlank()) ? "unknown" : name;
    }

    private UUID extractUuid(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof UUID uuid) {
                return uuid;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private String invokeStringMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof String stringResult) {
                return stringResult;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private boolean isLoaderAccepted(java.util.Collection<String> clientLoaders) {
        if (settings.acceptedLoaders().isEmpty()) {
            return true;
        }
        if (clientLoaders == null || clientLoaders.isEmpty()) {
            return false;
        }
        for (String expected : settings.acceptedLoaders()) {
            for (String candidate : clientLoaders) {
                if (expected.equalsIgnoreCase(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String loaderDisplayName() {
        if (settings.acceptedLoaders().isEmpty()) {
            return "fabric";
        }
        if (settings.acceptedLoaders().size() == 1) {
            return settings.acceptedLoaders().get(0);
        }
        return String.join("/", settings.acceptedLoaders());
    }

    private String requiredModMessage() {
        return "AutoModpack mod for " + loaderDisplayName() + " modloader is required to play on this server!";
    }

    private boolean isClientVersionHigher(String clientVersion, String serverVersion) {
        if (clientVersion == null || serverVersion == null) {
            return false;
        }
        if (!SEMVER_PATTERN.matcher(clientVersion).matches() || !SEMVER_PATTERN.matcher(serverVersion).matches()) {
            return false;
        }
        if (clientVersion.equals(serverVersion)) {
            return false;
        }
        String[] clientParts = clientVersion.split("\\.");
        String[] serverParts = serverVersion.split("\\.");
        int length = Math.min(clientParts.length, serverParts.length);
        try {
            for (int i = 0; i < length; i++) {
                int clientValue = Integer.parseInt(clientParts[i]);
                int serverValue = Integer.parseInt(serverParts[i]);
                if (clientValue > serverValue) {
                    return true;
                }
                if (clientValue < serverValue) {
                    return false;
                }
            }
        } catch (NumberFormatException ignored) {
            return false;
        }
        return clientParts.length > serverParts.length;
    }

    private void logHostMisconfiguration() {
        logger.severe("Host server error. AutoModpack host server is down or server is not configured correctly");

        var config = GlobalVariables.serverConfig;
        if (config != null) {
            if (config.bindPort == -1) {
                logger.warning("You are hosting AutoModpack host server on the Minecraft port.");
            } else {
                logger.warning("Please check if AutoModpack host server (TCP) port '" + config.bindPort
                        + "' is forwarded / opened correctly");
            }
            logger.warning("Make sure that 'address-to-send' is correctly set in the config file!");
            logger.warning("It can be either an IP address or a domain pointing to your modpack host server.");
            logger.warning("If nothing works, try changing the 'bind-port' in the config file, then forward / open it and restart server");
            logger.warning("Note that some hosting providers may proxy this port internally and give you a different address and port to use. In this case, separate the given address with ':', and set the first part as 'address-to-send' and the second part as 'port-to-send' in the config file.");

            if (config.bindPort != config.portToSend && config.bindPort != -1 && config.portToSend != -1) {
                logger.severe("bind-port '" + config.bindPort + "' is different than port-to-send '" + config.portToSend
                        + "'. If you are not using reverse proxy, match them! If you do use reverse proxy, make sure it is setup correctly.");
            }
        }

        if (GlobalVariables.hostServer != null) {
            String fingerprint = GlobalVariables.hostServer.getCertificateFingerprint();
            if (fingerprint != null && !fingerprint.isBlank()) {
                logger.warning("Server certificate fingerprint: " + fingerprint);
            }
        }
    }

    private void disconnect(String message) {
        Object disconnectPacket = NmsReflection.createDisconnectPacket(message);
        if (disconnectPacket != null) {
            NmsReflection.sendPacket(loginHandler, disconnectPacket);
        }
    }
}
