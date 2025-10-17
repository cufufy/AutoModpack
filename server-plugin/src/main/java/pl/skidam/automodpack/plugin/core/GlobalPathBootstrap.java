package pl.skidam.automodpack.plugin.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import pl.skidam.automodpack_core.GlobalVariables;

public final class GlobalPathBootstrap {
    private static final Map<String, Function<Path, Object>> PATH_FIELDS = new HashMap<>();

    static {
        PATH_FIELDS.put("automodpackDir", base -> base);
        PATH_FIELDS.put("hostModpackDir", base -> base.resolve("modpack"));
        PATH_FIELDS.put("hostContentModpackDir", base -> base.resolve("modpack").resolve("main"));
        PATH_FIELDS.put("serverConfigFile", base -> base.resolve("automodpack-server.json"));
        PATH_FIELDS.put("serverCoreConfigFile", base -> base.resolve("automodpack-core.json"));
        PATH_FIELDS.put("privateDir", base -> base.resolve(".private"));
        PATH_FIELDS.put("serverSecretsFile", base -> base.resolve(".private").resolve("automodpack-secrets.json"));
        PATH_FIELDS.put("knownHostsFile", base -> base.resolve(".private").resolve("automodpack-known-hosts.json"));
        PATH_FIELDS.put("serverCertFile", base -> base.resolve(".private").resolve("cert.crt"));
        PATH_FIELDS.put("serverPrivateKeyFile", base -> base.resolve(".private").resolve("key.pem"));
        PATH_FIELDS.put("hostModpackContentFile", base -> base.resolve("modpack").resolve("automodpack-content.json"));
    }

    private GlobalPathBootstrap() {
    }

    public static void redirect(Path baseDirectory) {
        PATH_FIELDS.forEach((fieldName, resolver) -> setStaticField(fieldName, resolver.apply(baseDirectory)));
    }

    private static void setStaticField(String fieldName, Object value) {
        try {
            VarHandle handle = MethodHandles.privateLookupIn(GlobalVariables.class, MethodHandles.lookup())
                    .findStaticVarHandle(GlobalVariables.class, fieldName, value.getClass());
            handle.set(value);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // fallback to standard reflection for legacy fields
            try {
                var field = GlobalVariables.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(null, value);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to override GlobalVariables." + fieldName, e);
            }
        }
    }
}
