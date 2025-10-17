# AutoModpack Server Plugin

This repository hosts the standalone AutoModpack addon for Paper and Spigot based servers. The plugin mirrors the behaviour of the AutoModpack mod on the server side – it watches a curated set of mods/configs, serves the generated modpack to connecting players, and bridges the AutoModpack handshake so vanilla servers can admit modded clients.

> **Important:** The AutoModpack client/server mod is **not** bundled with this plugin. Server owners must distribute the AutoModpack mod with their modpack and ensure players install it. The plugin refuses to start if no AutoModpack mod jar is present in its managed `mods/` directory.

## Getting Started

1. Download the latest `AutoModpackPlugin` release and drop the JAR into your server's `plugins/` folder.
2. Start the server once. The plugin creates `plugins/AutoModpackPlugin/` with managed `mods/` and `configs/` directories plus a `config.yml`.
3. Copy your modpack content into `plugins/AutoModpackPlugin/mods/` and any mirrored configs into `plugins/AutoModpackPlugin/configs/`.
4. Add the **AutoModpack mod jar** to the same `mods/` directory. The plugin verifies its presence on startup and will shut down with an error if it's missing.
5. Restart the server. The plugin builds the AutoModpack metadata, starts the embedded host, and begins serving files to clients.

Use `/automodpack` in game or from the console to manage the host (start/stop/regenerate, inspect active connections, view the TLS fingerprint, etc.).

## Configuration

`plugins/AutoModpackPlugin/config.yml` controls how the plugin hosts content and how aggressively it enforces AutoModpack usage. Key options include:

- `modpack-name` – Friendly name displayed to players.
- `force-mod` and `nag-missing-mod` – Require AutoModpack or gently remind players who join without it.
- `server-host.*` – Bind address, port, and advertised endpoint for the embedded host.

Configuration changes can be reloaded at runtime with `/automodpack config reload`.

## Building From Source

This repository is a trimmed Gradle project that only contains the Bukkit plugin. To build it locally:

```bash
./gradlew clean :server-plugin:build
```

The shaded plugin JAR is written to `server-plugin/build/libs/AutoModpackPlugin.jar` and an exploded distribution appears under `server-plugin/build/distributions/AutoModpackPlugin/`.

## License

The AutoModpack plugin continues to be distributed under the MIT License. Please refer to the upstream AutoModpack project or the packaged release for the full license text.
