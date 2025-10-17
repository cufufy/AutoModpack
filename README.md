<h1 align="center">
    ✨ AutoModpack ✨
</h1>
<p align="center">
    Tired of modpack update headaches? AutoModpack takes care of it!
    <br/>
    <br/>
    <a href="https://www.curseforge.com/minecraft/mc-mods/automodpack"><img src="http://cf.way2muchnoise.eu/639211.svg" alt="CurseForge Downloads"></a>
    <a href="https://github.com/Skidamek/AutoModpack/releases"><img src="https://img.shields.io/github/downloads/skidamek/automodpack/total?style=round&logo=github" alt="GitHub Total Downloads"></a>
    <a href="https://modrinth.com/mod/automodpack"><img src="https://img.shields.io/modrinth/dt/k68glP2e?logo=modrinth&label=&style=flat&color=242629" alt="Modrinth Downloads"></a>
</p>

---

## 🤯 Ditch the Update Hassle, Dive into Gameplay!

**AutoModpack** is the ultimate Minecraft modification designed to **revolutionize private server modpack management**. Say goodbye to the days of struggling to get everyone on the same version! This mod **automatically synchronizes** players with the server's modpack, making playing with friends incredibly smooth and hassle-free.

<p align="center">
    <a href="https://youtu.be/lPPzaNPn8g8" target="_blank">
        <img src="https://img.youtube.com/vi/lPPzaNPn8g8/0.jpg" alt="AutoModpack Showcase Video (Outdated)" width="400">
    </a>
    <br>
    <i>(Heads up! This showcase video is a little outdated, but it gives you a good idea of what AutoModpack does!)</i>
</p>

<br>

> **Disclaimer:** While AutoModpack is a powerful tool for managing modpacks, the content it downloads (mods, resource packs, etc.) is created by various talented developers. Please remember to respect their work and licenses. Don't use AutoModpack to mass-distribute content without explicit permission, especially for commercial purposes. Always check the licenses of the mods you include in your pack.

## 🔥 Why AutoModpack is Your New Best Friend

This isn't just another mod; it's a game-changer for private servers. Here's why:

*   🔌 **Plug-'n'-Play:** Install the mod, and you're done! Live in perfect sync with the server's modpack forever.
*   🔄 **Seamless Player Updates:** Players get the latest modpack updates automatically, without manual downloads or disruptions.
*   🚀 **Effortless Admin Management:** Easily manage mods, configs, resource packs, shaders, and more. Your modpack, your control.
*   ⚡️ **Direct & Respectful Downloads:** The mod pulls directly from Modrinth and CurseForge APIs, so mod authors get credit for every download.
*   🔒 **Secure & Speed:** Encrypted, authorized, compressed, quick modpack downloads.

## 🛠️ How the Magic Happens

AutoModpack works by generating a modpack (**metadata file**) on the server, which contains all the files of your modpack. The server then hosts this file and the modpack files.

When a client connects to the server:

1.  Connection: AutoModpack establishes a secure connection and prompts you to [verify the server's certificate fingerprint](https://moddedmc.wiki/en/project/automodpack/docs/technicals/certificate).
2.  Direct links: Fetches the APIs for direct downloads of your modpack's files from Modrinth and CurseForge, where possible (mods, resource packs, shaders).
3.  Modpack download: All files are downloaded to the client's automodpack folder.
4.  Game restart: AutoModpack loads the modpack, and the client is perfectly synced and ready to play!

On subsequent game launches, AutoModpack checks for updates. If changes are detected, it updates the modpack in the background—no additional restarts are required!

## ⚠️ Security and Trust - Read This!

> With great power comes great responsibility.

Be aware that this mod allows remote servers to download *arbitrary executable* files directly into your game folder. It's crucial to **only use it on servers you absolutely trust**. A malicious server (administrator) *can* include malicious/harmful files.

While AutoModpack itself tries to be as secure as possible, due to the nature of the internet, the creators and contributors of AutoModpack are not responsible for any harm, damage, loss, or issues that may result from files downloaded from a server you connect to using the mod. **By using AutoModpack, you acknowledge and accept this risk.**

**If you have valuable security insights or concerns, please reach out!** You can contact privately on [Discord](https://discordapp.com/users/464522287618457631) or publicly on [Discord server](https://discord.gg/hS6aMyeA9P) or just open an issue on [GitHub](https://github.com/Skidamek/AutoModpack/issues).
## 🚀 Getting Started is a Breeze!

Installing AutoModpack is as simple as installing any other mod.

1.  Download the AutoModpack from the releases page on [GitHub](https://github.com/Skidamek/AutoModpack/releases), [CurseForge](https://www.curseforge.com/minecraft/mc-mods/automodpack), or [Modrinth](https://modrinth.com/mod/automodpack).
2.  Place the downloaded file into the `/mods/` folder of both your server and client Minecraft installations.
3.  Start your server and let AutoModpack generate the initial modpack metadata.
4.  Connect to your server with the mod installed on your client.

That's typically all you need to do! AutoModpack will automatically create the modpack from your server's mods.

**Want to customize your modpack further?** Add configs, client-side-only mods, and more? **Check out the [documentation](https://moddedmc.wiki/en/project/automodpack/docs)!** There's also a start guide covering more stuff. If you encounter any issues or have questions, feel free to join [Discord server](https://discord.gg/hS6aMyeA9P) or open an issue on [GitHub](https://github.com/Skidamek/AutoModpack/issues).

Prefer an all-in-one solution? You can also use our [modified Fabric installer](https://github.com/Skidamek/AutoModpack-Installer/releases/tag/Latest) which downloads AutoModpack alongside the Fabric loader.

## 🌐 Self-Host the AutoModpack Downloads

You don't need an external file host—AutoModpack can serve your modpack directly to players. To run the built-in host yourself:

1. **Install AutoModpack on the server** (mod or Paper/Spigot plugin) and start it once so the initial metadata is generated.
2. **Curate the files you want to ship:**
   * Modded servers mirror the `/mods` folder and any additional directories configured in `automodpack-server.json`.
   * The Paper/Spigot plugin creates managed folders at `plugins/AutoModpackPlugin/mods/` and `plugins/AutoModpackPlugin/configs/`; drop your jars/configs there and the plugin keeps them mirrored in the hosted modpack directory.【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/mod/ModpackHostService.java†L95-L144】
3. **Expose the host to players.** By default the host binds to the Minecraft port; alternatively set `server-host.bind-address`/`bind-port` (or the plugin's `server-host.*` entries) to run on a separate interface/port and forward it through your firewall or reverse proxy.【F:server-plugin/src/main/resources/config.yml†L12-L18】
4. **Share the address AutoModpack advertises.** Clients receive the value from `server-host.address-to-send`/`port-to-send`, so make sure it's reachable from outside your network.【F:server-plugin/src/main/resources/config.yml†L13-L17】
5. **Manage the host in-game or from console:** `/automodpack host start|stop|restart` toggles the host, `/automodpack host connections` shows active downloads, and `/automodpack host fingerprint` prints the TLS fingerprint your players should verify on first connect.【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/command/AutoModpackCommand.java†L53-L145】

Whenever you edit the hosted files, AutoModpack regenerates the metadata automatically. You can trigger a manual rebuild with `/automodpack generate`, and `/automodpack config reload` refreshes `automodpack-server.json` without a restart.【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/command/AutoModpackCommand.java†L38-L115】【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/mod/ModpackHostService.java†L146-L188】

## 🧩 Paper & Spigot Server Plugin

AutoModpack ships with a dedicated Bukkit-compatible plugin (`AutoModpackPlugin`) that lets you run the host-side logic without installing a traditional mod loader on your server. The plugin mirrors the behaviour of the mod: it generates modpack metadata, serves it to clients, and keeps track of mod/config changes in real time.

### Installation & First Launch

1. Download the `AutoModpackPlugin` JAR from the releases page (it is bundled alongside the standard mod download).
2. Drop the JAR into your server's `plugins/` directory and start the server.
3. On first run the plugin will:
   * create its data directory (`plugins/AutoModpackPlugin/`) along with managed `mods/` and `configs/` folders used for syncing content to players;【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/mod/ModpackHostService.java†L24-L44】【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/mod/ModRepositoryWatcher.java†L20-L41】
   * generate the default `config.yml` next to the plugin JAR so you can tweak behaviour without editing resources in-place.【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/AutoModpackPlugin.java†L19-L52】
4. Copy the mods and data packs you want to distribute into `plugins/AutoModpackPlugin/mods/`. Optional client configs can be mirrored via `plugins/AutoModpackPlugin/configs/`; the plugin keeps this folder in sync with the generated host modpack directory.【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/mod/ConfigMirror.java†L24-L45】
5. Restart or reload the server to let AutoModpack regenerate the modpack metadata.

The plugin keeps watching the `mods/` directory for file changes, so future updates are picked up automatically without restarting the server.【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/mod/ModRepositoryWatcher.java†L32-L70】

### Building the Plugin

Use Maven to rebuild the plugin after making changes:

```bash
mvn -pl server-plugin -am package
```

### Hosting Configuration

All runtime options live in `plugins/AutoModpackPlugin/config.yml`.

* **Modpack name & nagging** – `modpack-name`, `force-mod`, `nag-missing-mod`, and related message/link fields control how the plugin introduces AutoModpack to new players.【F:server-plugin/src/main/resources/config.yml†L1-L18】
* **Loader compatibility** – Add extra loaders under `accepted-loaders` to let clients from compatible mod loaders connect (use with caution, most mods remain loader-specific).【F:server-plugin/src/main/resources/config.yml†L7-L8】
* **Version pinning** – `automodpack-version` and `minecraft-version` define the expected client versions that will be enforced during the login handshake.【F:server-plugin/src/main/resources/config.yml†L9-L11】
* **Built-in host** – Tweak `server-host.*` to expose the Netty host directly or behind a reverse proxy. Use `bind-address`/`bind-port` to pick where the host listens, `address-to-send`/`port-to-send` for the external address presented to clients, and `disable-internal-tls` or `bandwidth-limit` if you run through a proxy.【F:server-plugin/src/main/resources/config.yml†L12-L18】

AutoModpack always attempts an initial modpack generation when the plugin loads. If there are no files to host yet the plugin stays up, continues watching the `mods/` folder, and logs that hosting will start once content becomes available.【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/mod/ModpackHostService.java†L60-L87】

### Operating the Host

Typical workflow for server administrators:

1. **Generate or refresh metadata** – `/automodpack generate`
2. **Start or stop the host** – `/automodpack host start`, `/automodpack host stop`, `/automodpack host restart`
3. **Inspect host status** – `/automodpack host`, `/automodpack host connections`, `/automodpack host fingerprint`
4. **Reload configuration** – `/automodpack config reload`

These commands mirror the Fabric/Forge mod, so your players already familiar with AutoModpack will feel at home.【F:docs/commands/commands.mdx†L1-L10】

On Bukkit servers the plugin registers `/automodpack` (alias `/amp`) with the `automodpack.admin` permission, which defaults to OPs. Grant that node to trusted staff to let them manage the host without giving them full operator powers.【F:server-plugin/src/main/resources/plugin.yml†L12-L19】【F:server-plugin/src/main/java/pl/skidam/automodpack/plugin/command/AutoModpackCommand.java†L21-L33】

### Commands & Permissions

| Command | Description | Required permission |
| --- | --- | --- |
| `/automodpack` | Prints the installed AutoModpack version and available sub-commands. | Minecraft permission level 3 (server operator).【F:docs/commands/commands.mdx†L1-L3】【F:src/main/java/pl/skidam/automodpack/modpack/Commands.java†L23-L60】 |
| `/automodpack generate` | Rebuilds the modpack metadata from the contents of your `mods/` and synced files. | Permission level 3.【F:docs/commands/commands.mdx†L3-L4】【F:src/main/java/pl/skidam/automodpack/modpack/Commands.java†L26-L33】 |
| `/automodpack host [start|stop|restart]` | Manages the embedded Netty host that distributes the modpack. | Permission level 3.【F:docs/commands/commands.mdx†L4-L7】【F:src/main/java/pl/skidam/automodpack/modpack/Commands.java†L33-L58】 |
| `/automodpack host connections` | Lists active modpack download sessions for auditing. | Permission level 3.【F:docs/commands/commands.mdx†L7-L8】【F:src/main/java/pl/skidam/automodpack/modpack/Commands.java†L45-L89】 |
| `/automodpack host fingerprint` | Prints the TLS fingerprint clients must trust when connecting. | Permission level 3.【F:docs/commands/commands.mdx†L8-L9】【F:src/main/java/pl/skidam/automodpack/modpack/Commands.java†L40-L58】 |
| `/automodpack config reload` | Reloads `automodpack-server.json` after manual edits. | Permission level 3.【F:docs/commands/commands.mdx†L9-L10】【F:src/main/java/pl/skidam/automodpack/modpack/Commands.java†L58-L84】 |

`/amp` is registered as a shorthand alias for `/automodpack` and shares the same permission level requirements.【F:docs/commands/commands.mdx†L1-L2】【F:src/main/java/pl/skidam/automodpack/modpack/Commands.java†L66-L72】 On Bukkit-based servers, permission level 3 corresponds to operators; use your permission plugin to grant the equivalent capability if you want trusted staff to run the commands without full operator status.

## 🙏 Huge Thanks to Our Supporters!

AutoModpack wouldn't be where it is without the amazing community!

*   **All the fantastic [contributors](https://github.com/Skidamek/AutoModpack/graphs/contributors)** who have helped improve the mod!
*   **[duckymirror](https://github.com/duckymirror), Juan, cloud, [Merith](https://github.com/Merith-TK), [SettingDust](https://github.com/SettingDust), Suerion, and griffin4cats** for their invaluable help with testing, code, and ideas!
*   **HyperDraw** for creating the awesome mod icon!
*   **All the generous supporters on [Ko-fi](https://ko-fi.com/skidam)** - your support means the world!

## 💖 Contribute and Make AutoModpack Even Better!

We love contributions! Whether it's code, bug reports, documentation improvements, or just spreading the word, your help is welcome.

**Ready to contribute? See our [CONTRIBUTING.md](CONTRIBUTING.md) for details!**
