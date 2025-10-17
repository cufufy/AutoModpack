package com.cufufy.amp.plugin.bridge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * Lightweight reflective bridge for interacting with Mojang server classes without compile-time dependencies.
 */
final class NmsReflection {
    private static final Logger LOGGER = Bukkit.getLogger();

    private static final Class<?> CLASS_MINECRAFT_SERVER = findClass("net.minecraft.server.MinecraftServer");
    private static final Class<?> CLASS_SERVER_CONNECTION = findClass("net.minecraft.server.network.ServerConnectionListener");
    private static final Class<?> CLASS_SERVER_LOGIN_HANDLER = findClass("net.minecraft.server.network.ServerLoginPacketListenerImpl");
    private static final Class<?> CLASS_CONNECTION = findClass("net.minecraft.network.Connection");
    private static final Class<?> CLASS_CLIENTBOUND_CUSTOM_QUERY = findClass("net.minecraft.network.protocol.login.ClientboundCustomQueryPacket");
    private static final Class<?> CLASS_SERVERBOUND_CUSTOM_QUERY = findClass("net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket");
    private static final Class<?> CLASS_FRIENDLY_BYTE_BUF = findClass("net.minecraft.network.FriendlyByteBuf");
    private static final Class<?> CLASS_RESOURCE_LOCATION = findClass("net.minecraft.resources.ResourceLocation");
    private static final Class<?> CLASS_CUSTOM_QUERY_PAYLOAD = findClass("net.minecraft.network.protocol.login.custom.CustomQueryPayload");
    private static final Class<?> CLASS_COMPONENT = findClass("net.minecraft.network.chat.Component");
    private static final Class<?> CLASS_CLIENTBOUND_LOGIN_DISCONNECT = findClass("net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket");

    private static final MethodHandle CRAFT_SERVER_GET_SERVER = resolveCraftServerHandle();
    private static final MethodHandle SERVER_GET_CONNECTION = lookupVirtual(CLASS_MINECRAFT_SERVER, "getConnection", CLASS_SERVER_CONNECTION);
    private static final MethodHandle CONNECTION_SEND_PACKET = lookupVirtual(CLASS_CONNECTION, "send", void.class, findClass("net.minecraft.network.protocol.Packet"));
    private static final MethodHandle COMPONENT_LITERAL = lookupStatic(CLASS_COMPONENT, "literal", CLASS_COMPONENT, String.class);

    private static final Map<Class<?>, List<Field>> CACHED_FIELD_LISTS = new ConcurrentHashMap<>();

    private NmsReflection() {
    }

    static Channel connectionChannel(Object loginHandler) {
        Object connection = connection(loginHandler);
        if (connection == null) {
            return null;
        }
        try {
            Field channelField = connection.getClass().getDeclaredField("channel");
            channelField.setAccessible(true);
            return (Channel) channelField.get(connection);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
    }

    static boolean isAvailable() {
        return CLASS_MINECRAFT_SERVER != null
                && CLASS_SERVER_CONNECTION != null
                && CLASS_SERVER_LOGIN_HANDLER != null
                && CLASS_CONNECTION != null
                && CLASS_CLIENTBOUND_CUSTOM_QUERY != null
                && CLASS_SERVERBOUND_CUSTOM_QUERY != null
                && CLASS_FRIENDLY_BYTE_BUF != null
                && CLASS_RESOURCE_LOCATION != null
                && CLASS_COMPONENT != null
                && CLASS_CLIENTBOUND_LOGIN_DISCONNECT != null;
    }

    static Object minecraftServer(Server bukkitServer) {
        if (CRAFT_SERVER_GET_SERVER == null) {
            return null;
        }
        try {
            return CRAFT_SERVER_GET_SERVER.invoke(bukkitServer);
        } catch (Throwable e) {
            LOGGER.severe("Failed to resolve MinecraftServer instance: " + e.getMessage());
            return null;
        }
    }

    static Object serverConnection(Object minecraftServer) {
        if (minecraftServer == null || SERVER_GET_CONNECTION == null) {
            return null;
        }
        try {
            return SERVER_GET_CONNECTION.invoke(minecraftServer);
        } catch (Throwable e) {
            LOGGER.severe("Failed to access ServerConnectionListener: " + e.getMessage());
            return null;
        }
    }

    static Collection<Object> pendingLoginHandlers(Object serverConnection) {
        if (serverConnection == null) {
            return List.of();
        }
        List<Field> fields = CACHED_FIELD_LISTS.computeIfAbsent(serverConnection.getClass(), NmsReflection::discoverIterableFields);
        if (fields.isEmpty()) {
            return List.of();
        }
        List<Object> result = new ArrayList<>();
        for (Field field : fields) {
            try {
                Object value = field.get(serverConnection);
                if (value == null) {
                    continue;
                }
                if (value instanceof Iterable<?> iterable) {
                    for (Object element : iterable) {
                        if (isLoginHandler(element)) {
                            result.add(element);
                        }
                    }
                } else if (value.getClass().isArray()) {
                    int length = Array.getLength(value);
                    for (int i = 0; i < length; i++) {
                        Object element = Array.get(value, i);
                        if (isLoginHandler(element)) {
                            result.add(element);
                        }
                    }
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return result;
    }

    static Object connection(Object loginHandler) {
        if (loginHandler == null) {
            return null;
        }
        Field field = findField(loginHandler.getClass(), CLASS_CONNECTION);
        if (field == null) {
            return null;
        }
        try {
            return field.get(loginHandler);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    static Object minecraftServer(Object loginHandler) {
        if (loginHandler == null) {
            return null;
        }
        Field field = findField(loginHandler.getClass(), CLASS_MINECRAFT_SERVER);
        if (field == null) {
            return null;
        }
        try {
            return field.get(loginHandler);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    static Object gameProfile(Object loginHandler) {
        if (loginHandler == null) {
            return null;
        }
        try {
            Class<?> profileClass = findClass("com.mojang.authlib.GameProfile");
            Field field = findField(loginHandler.getClass(), profileClass);
            if (field == null) {
                return null;
            }
            return field.get(loginHandler);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    static Enum<?> loginState(Object loginHandler) {
        if (loginHandler == null) {
            return null;
        }
        for (Field field : loginHandler.getClass().getDeclaredFields()) {
            if (field.getType().isEnum()) {
                if (field.getName().equalsIgnoreCase("state") || field.getType().getName().contains("State")) {
                    field.setAccessible(true);
                    try {
                        return (Enum<?>) field.get(loginHandler);
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
        }
        return null;
    }

    static void sendPacket(Object loginHandler, Object packet) {
        Object connection = connection(loginHandler);
        if (connection == null || CONNECTION_SEND_PACKET == null || packet == null) {
            return;
        }
        try {
            CONNECTION_SEND_PACKET.invoke(connection, packet);
        } catch (Throwable e) {
            LOGGER.severe("Failed to send login packet: " + e.getMessage());
        }
    }

    static Object createDisconnectPacket(String message) {
        if (CLASS_CLIENTBOUND_LOGIN_DISCONNECT == null || CLASS_COMPONENT == null) {
            return null;
        }
        try {
            Object component = COMPONENT_LITERAL.invoke(message);
            try {
                return CLASS_CLIENTBOUND_LOGIN_DISCONNECT.getConstructor(CLASS_COMPONENT).newInstance(component);
            } catch (ReflectiveOperationException ex) {
                return CLASS_CLIENTBOUND_LOGIN_DISCONNECT.getConstructor(findClass("net.minecraft.network.protocol.login.LoginDisconnectReason"), CLASS_COMPONENT)
                        .newInstance(null, component);
            }
        } catch (Throwable e) {
            LOGGER.severe("Failed to create disconnect packet: " + e.getMessage());
            return null;
        }
    }

    static Object createResourceLocation(String namespace, String path) {
        if (CLASS_RESOURCE_LOCATION == null) {
            return null;
        }
        try {
            try {
                return CLASS_RESOURCE_LOCATION.getConstructor(String.class, String.class).newInstance(namespace, path);
            } catch (ReflectiveOperationException ex) {
                Method method = CLASS_RESOURCE_LOCATION.getMethod("tryParse", String.class);
                return method.invoke(null, namespace + ":" + path);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.severe("Failed to create ResourceLocation: " + e.getMessage());
            return null;
        }
    }

    static Object createFriendlyByteBuf() {
        if (CLASS_FRIENDLY_BYTE_BUF == null) {
            return null;
        }
        try {
            ByteBuf buf = Unpooled.buffer();
            return CLASS_FRIENDLY_BYTE_BUF.getConstructor(ByteBuf.class).newInstance(buf);
        } catch (ReflectiveOperationException e) {
            LOGGER.severe("Failed to create FriendlyByteBuf: " + e.getMessage());
            return null;
        }
    }

    static void writeUtf(Object friendlyByteBuf, String value) {
        if (friendlyByteBuf == null || value == null) {
            return;
        }
        try {
            Method method;
            try {
                method = friendlyByteBuf.getClass().getMethod("writeUtf", String.class);
                method.invoke(friendlyByteBuf, value);
            } catch (NoSuchMethodException ex) {
                method = friendlyByteBuf.getClass().getMethod("writeUtf", String.class, int.class);
                method.invoke(friendlyByteBuf, value, Short.MAX_VALUE);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.severe("Failed to write UTF to FriendlyByteBuf: " + e.getMessage());
        }
    }

    static String readUtf(Object friendlyByteBuf) {
        if (friendlyByteBuf == null) {
            return null;
        }
        try {
            Method method;
            try {
                method = friendlyByteBuf.getClass().getMethod("readUtf", int.class);
                return (String) method.invoke(friendlyByteBuf, Short.MAX_VALUE);
            } catch (NoSuchMethodException ex) {
                method = friendlyByteBuf.getClass().getMethod("readUtf");
                return (String) method.invoke(friendlyByteBuf);
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.severe("Failed to read UTF from FriendlyByteBuf: " + e.getMessage());
            return null;
        }
    }

    static Object createCustomQueryPacket(int id, Object resourceLocation, Object buffer) {
        if (CLASS_CLIENTBOUND_CUSTOM_QUERY == null) {
            return null;
        }
        try {
            if (resourceLocation != null && buffer != null) {
                for (var constructor : CLASS_CLIENTBOUND_CUSTOM_QUERY.getConstructors()) {
                    Class<?>[] params = constructor.getParameterTypes();
                    if (params.length == 3 && params[0] == int.class && params[1].isAssignableFrom(resourceLocation.getClass())) {
                        return constructor.newInstance(id, resourceLocation, buffer);
                    }
                }
            }
            if (CLASS_CUSTOM_QUERY_PAYLOAD != null && buffer != null) {
                Object payload = createLoginPayload(resourceLocation, buffer);
                if (payload != null) {
                    for (var constructor : CLASS_CLIENTBOUND_CUSTOM_QUERY.getConstructors()) {
                        Class<?>[] params = constructor.getParameterTypes();
                        if (params.length == 2 && params[0] == int.class && params[1].isInstance(payload)) {
                            return constructor.newInstance(id, payload);
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.severe("Failed to create ClientboundCustomQueryPacket: " + e.getMessage());
        }
        return null;
    }

    static FriendlyResponse parseQueryResponse(Object packet) {
        if (packet == null || !CLASS_SERVERBOUND_CUSTOM_QUERY.isInstance(packet)) {
            return FriendlyResponse.NOT_HANDLED;
        }
        try {
            int id;
            Method transactionGetter;
            try {
                transactionGetter = packet.getClass().getMethod("transactionId");
            } catch (NoSuchMethodException ex) {
                transactionGetter = packet.getClass().getMethod("getTransactionId");
            }
            id = (int) transactionGetter.invoke(packet);

            Method payloadGetter = null;
            try {
                payloadGetter = packet.getClass().getMethod("payload");
            } catch (NoSuchMethodException ignored) {
            }

            if (payloadGetter != null) {
                Object payload = payloadGetter.invoke(packet);
                if (payload == null) {
                    return new FriendlyResponse(id, false, null, null);
                }
                Method dataMethod = payload.getClass().getMethod("data");
                Object data = dataMethod.invoke(payload);
                Method idMethod = payload.getClass().getMethod("id");
                Object identifier = idMethod.invoke(payload);
                return new FriendlyResponse(id, true, data, identifier);
            }

            Method dataGetter;
            try {
                dataGetter = packet.getClass().getMethod("getData");
            } catch (NoSuchMethodException ex) {
                dataGetter = packet.getClass().getMethod("getData", new Class[0]);
            }
            Object dataObj = dataGetter.invoke(packet);
            boolean understood = dataObj != null;
            return new FriendlyResponse(id, understood, dataObj, null);
        } catch (ReflectiveOperationException e) {
            LOGGER.severe("Failed to parse login query response: " + e.getMessage());
            return FriendlyResponse.NOT_HANDLED;
        }
    }

    static void mirrorBuffer(Object targetBuf, Object sourceBuf) {
        if (targetBuf == null || sourceBuf == null) {
            return;
        }
        try {
            Method copyMethod = sourceBuf.getClass().getMethod("copy");
            Object copy = copyMethod.invoke(sourceBuf);
            Class<?> byteBufClass = Class.forName("io.netty.buffer.ByteBuf");
            Method writeBytes = targetBuf.getClass().getMethod("writeBytes", byteBufClass);
            writeBytes.invoke(targetBuf, copy);
        } catch (ReflectiveOperationException e) {
            LOGGER.severe("Failed to mirror friendly buffer: " + e.getMessage());
        }
    }

    static ChannelDuplexHandler createInboundInterceptor(Function<Object, Boolean> handler) {
        return new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (handler.apply(msg)) {
                    return;
                }
                super.channelRead(ctx, msg);
            }
        };
    }

    static void installInterceptor(Channel channel, ChannelDuplexHandler handler) {
        if (channel == null || handler == null) {
            return;
        }
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get("automodpack-login") != null) {
            return;
        }
        pipeline.addBefore("packet_handler", "automodpack-login", handler);
    }

    private static Object createLoginPayload(Object resourceLocation, Object buffer) {
        if (CLASS_CUSTOM_QUERY_PAYLOAD == null || resourceLocation == null || buffer == null) {
            return null;
        }
        return java.lang.reflect.Proxy.newProxyInstance(
                CLASS_CUSTOM_QUERY_PAYLOAD.getClassLoader(),
                new Class<?>[] { CLASS_CUSTOM_QUERY_PAYLOAD },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("write".equals(name) && args != null && args.length == 1) {
                        mirrorBuffer(args[0], buffer);
                        return null;
                    }
                    if ("id".equals(name)) {
                        return resourceLocation;
                    }
                    if ("type".equals(name)) {
                        return resolvePayloadType(resourceLocation);
                    }
                    return method.getDefaultValue();
                });
    }

    private static Object resolvePayloadType(Object resourceLocation) {
        try {
            Class<?> typeClass = findClass("net.minecraft.network.protocol.login.custom.CustomQueryPayload$Type");
            if (typeClass == null) {
                return null;
            }
            Method create = typeClass.getMethod("create", CLASS_RESOURCE_LOCATION);
            return create.invoke(null, resourceLocation);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static List<Field> discoverIterableFields(Class<?> type) {
        List<Field> result = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            if (Iterable.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
                result.add(field);
            }
        }
        return result;
    }

    private static Field findField(Class<?> owner, Class<?> desiredType) {
        for (Field field : owner.getDeclaredFields()) {
            if (desiredType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static boolean isLoginHandler(Object candidate) {
        return candidate != null && CLASS_SERVER_LOGIN_HANDLER.isInstance(candidate);
    }

    private static MethodHandle resolveCraftServerHandle() {
        try {
            Class<?> craftServerClass = Class.forName("org.bukkit.craftbukkit.CraftServer");
            Method method = craftServerClass.getMethod("getServer");
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return lookup.unreflect(method);
        } catch (ReflectiveOperationException e) {
            LOGGER.severe("Failed to resolve CraftServer#getServer handle: " + e.getMessage());
            return null;
        }
    }

    private static MethodHandle lookupStatic(Class<?> owner, String name, Class<?> returnType, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            Method method = owner.getDeclaredMethod(name, params);
            method.setAccessible(true);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return lookup.unreflect(method);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static MethodHandle lookupVirtual(Class<?> owner, String name, Class<?> returnType, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            Method method = owner.getMethod(name, params);
            method.setAccessible(true);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return lookup.unreflect(method);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    record FriendlyResponse(int id, boolean understood, Object buffer, Object channelIdentifier) {
        static final FriendlyResponse NOT_HANDLED = new FriendlyResponse(Integer.MIN_VALUE, false, null, null);
    }
}
