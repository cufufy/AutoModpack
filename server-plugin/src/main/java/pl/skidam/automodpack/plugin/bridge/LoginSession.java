package pl.skidam.automodpack.plugin.bridge;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import pl.skidam.automodpack.plugin.config.PluginSettings;
import pl.skidam.automodpack.plugin.bridge.NmsReflection.FriendlyResponse;
import pl.skidam.automodpack.plugin.bridge.LoginBridgeManager.Handshake;
import pl.skidam.automodpack_core.GlobalVariables;

final class LoginSession {
    private static final int HANDSHAKE_QUERY_ID = -100;
    private static final int DATA_QUERY_ID = -101;

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
        if (!response.understood()) {
            logger.warning("Player without AutoModpack attempted to join");
            if (settings.forceMod()) {
                disconnect("AutoModpack mod is required to join this server.");
            }
            missingCallback.accept(profileId);
            closed = settings.forceMod();
            return;
        }

        String json = NmsReflection.readUtf(response.buffer());
        if (json == null) {
            disconnect("Invalid AutoModpack handshake from client.");
            closed = true;
            return;
        }

        Handshake clientHandshake = parseHandshake(json);
        if (clientHandshake == null) {
            disconnect("Malformed AutoModpack handshake payload.");
            closed = true;
            return;
        }

        if (!validateHandshake(clientHandshake)) {
            disconnect("AutoModpack version mismatch. Expected " + settings.automodpackVersion());
            closed = true;
            return;
        }

        sendData(profileId);
    }

    private void handleDataResponse(FriendlyResponse response) {
        if (!response.understood()) {
            return;
        }
        if (response.buffer() == null) {
            return;
        }
        String marker = NmsReflection.readUtf(response.buffer());
        if (marker == null) {
            return;
        }
        UUID profileId = resolveProfileId();
        if ("true".equalsIgnoreCase(marker)) {
            disconnect("AutoModpack host requires you to install the modpack.");
            closed = true;
            completionCallback.accept(profileId, false);
        } else if ("false".equalsIgnoreCase(marker)) {
            completionCallback.accept(profileId, true);
        }
    }

    private void sendData(UUID profileId) {
        if (dataSent) {
            return;
        }
        dataSent = true;

        if (GlobalVariables.hostServer == null || !GlobalVariables.hostServer.isRunning()) {
            logger.warning("AutoModpack host server is not running - unable to deliver modpack to clients");
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
    }

    private boolean validateHandshake(Handshake handshake) {
        if (handshake == null) {
            return false;
        }
        if (!Objects.equals(handshake.amVersion(), settings.automodpackVersion())) {
            return false;
        }
        if (!Objects.equals(handshake.mcVersion(), settings.minecraftVersion())) {
            return false;
        }
        if (settings.acceptedLoaders().isEmpty()) {
            return true;
        }
        for (String loader : settings.acceptedLoaders()) {
            if (handshake.loaders() != null && handshake.loaders().stream().anyMatch(loader::equalsIgnoreCase)) {
                return true;
            }
        }
        return false;
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
        try {
            Method method = profile.getClass().getMethod("getId");
            Object result = method.invoke(profile);
            if (result instanceof UUID uuid) {
                return uuid;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return new UUID(0L, 0L);
    }

    private void disconnect(String message) {
        Object disconnectPacket = NmsReflection.createDisconnectPacket(message);
        if (disconnectPacket != null) {
            NmsReflection.sendPacket(loginHandler, disconnectPacket);
        }
    }
}
