package com.cufufy.amp.plugin.bridge;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import com.cufufy.amp.plugin.config.PluginSettings;
import com.cufufy.amp.core.auth.Secrets;
import com.cufufy.amp.core.auth.SecretsStore;

/**
 * Drives the login handshake bridge between AutoModpack clients and vanilla Paper/Spigot servers.
 */
public final class LoginBridgeManager implements Listener, AutoCloseable {
    private static final int HANDSHAKE_QUERY_ID = -100;
    private static final int DATA_QUERY_ID = -101;

    private final Plugin plugin;
    private final PluginSettings settings;
    private final Logger logger;
    private final Map<Object, LoginSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> knownClients = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public LoginBridgeManager(Plugin plugin, PluginSettings settings, Logger logger) {
        this.plugin = plugin;
        this.settings = settings;
        this.logger = logger;
    }

    public void start() {
        if (!NmsReflection.isAvailable()) {
            logger.severe("AutoModpack login bridge is unavailable - incompatible server version");
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        Server bukkitServer = plugin.getServer();
        Object minecraftServer = NmsReflection.minecraftServer(bukkitServer);
        Object serverConnection = NmsReflection.serverConnection(minecraftServer);
        Collection<Object> handlers = NmsReflection.pendingLoginHandlers(serverConnection);

        Map<Object, LoginSession> active = new HashMap<>();
        for (Object handler : handlers) {
            LoginSession session = sessions.computeIfAbsent(handler, key -> createSession(key));
            if (session != null) {
                session.tick();
                active.put(handler, session);
            }
        }
        sessions.keySet().retainAll(active.keySet());
    }

    private LoginSession createSession(Object loginHandler) {
        Channel channel = NmsReflection.connectionChannel(loginHandler);
        if (channel == null) {
            logger.warning("Unable to attach AutoModpack login handler - missing Netty channel");
            return null;
        }

        LoginSession session = new LoginSession(
                loginHandler,
                settings,
                logger,
                this::handleClientResult,
                this::handleClientMissing
        );

        ChannelDuplexHandler interceptor = NmsReflection.createInboundInterceptor(packet -> session.handlePacket(packet));
        NmsReflection.installInterceptor(channel, interceptor);
        return session;
    }

    private void handleClientResult(UUID profileId, boolean success) {
        if (profileId != null) {
            knownClients.put(profileId, success);
        }
    }

    private void handleClientMissing(UUID profileId) {
        if (profileId != null) {
            knownClients.put(profileId, false);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Boolean hasMod = knownClients.get(player.getUniqueId());
        if (Boolean.FALSE.equals(hasMod) && settings.nagMissingMod()) {
            player.sendMessage(settings.nagMessage());
            if (!settings.nagLinkText().isBlank() && !settings.nagLinkUrl().isBlank()) {
                player.sendMessage(settings.nagLinkText() + ": " + settings.nagLinkUrl());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        knownClients.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void close() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        sessions.clear();
        knownClients.clear();
    }

    static String handshakeJson(PluginSettings settings) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"loaders\":[");
        Collection<String> loaders = settings.acceptedLoaders().isEmpty()
                ? List.of("fabric")
                : settings.acceptedLoaders();
        Iterator<String> it = loaders.iterator();
        while (it.hasNext()) {
            sb.append('"').append(escape(it.next())).append('"');
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(']');
        sb.append(',').append("\"amVersion\":\"").append(escape(settings.automodpackVersion())).append('"');
        sb.append(',').append("\"mcVersion\":\"").append(escape(settings.minecraftVersion())).append('"');
        sb.append('}');
        return sb.toString();
    }

    static String dataJson(PluginSettings settings, UUID playerId) {
        Secrets.Secret secret = Secrets.generateSecret();
        SecretsStore.saveHostSecret(playerId.toString(), secret);

        int portToSend = settings.host().portToSend();
        String addressToSend = settings.host().addressToSend();
        boolean requiresMagic = settings.host().bindPort() == -1;

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        appendStringField(sb, "address", addressToSend);
        sb.append(',');
        appendNumberField(sb, "port", portToSend);
        sb.append(',');
        appendStringField(sb, "modpackName", settings.modpackName());
        sb.append(',');
        sb.append("\"secret\":{");
        appendStringField(sb, "secret", secret.secret());
        sb.append(',');
        appendNumberField(sb, "timestamp", secret.timestamp());
        sb.append('}');
        sb.append(',');
        appendBooleanField(sb, "modRequired", settings.forceMod());
        sb.append(',');
        appendBooleanField(sb, "requiresMagic", requiresMagic);
        sb.append('}');
        return sb.toString();
    }

    private static void appendStringField(StringBuilder sb, String name, String value) {
        sb.append('"').append(name).append('"').append(':').append('"').append(escape(value)).append('"');
    }

    private static void appendNumberField(StringBuilder sb, String name, Number value) {
        sb.append('"').append(name).append('"').append(':').append(value);
    }

    private static void appendBooleanField(StringBuilder sb, String name, boolean value) {
        sb.append('"').append(name).append('"').append(':').append(value);
    }

    private static String escape(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    record Handshake(Collection<String> loaders, String amVersion, String mcVersion) {
    }
}
