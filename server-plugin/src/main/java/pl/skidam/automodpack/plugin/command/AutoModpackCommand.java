package pl.skidam.automodpack.plugin.command;

import static pl.skidam.automodpack_core.GlobalVariables.AM_VERSION;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.entity.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import pl.skidam.automodpack.plugin.mod.ModpackHostService;
import pl.skidam.automodpack_core.auth.SecretsStore;

/**
 * Bukkit command bridge mirroring the dedicated AutoModpack server commands.
 */
public final class AutoModpackCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "automodpack.admin";

    private final JavaPlugin plugin;
    private final ModpackHostService hostService;

    public AutoModpackCommand(JavaPlugin plugin, ModpackHostService hostService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.hostService = Objects.requireNonNull(hostService, "hostService");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            send(sender, ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendAbout(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "generate" -> {
                runAsync(() -> {
                    send(sender, ChatColor.YELLOW + "Regenerating AutoModpack metadata...");
                    boolean generated = hostService.regenerateModpack();
                    if (generated) {
                        send(sender, ChatColor.GREEN + "Modpack metadata regenerated!");
                    } else {
                        send(sender, ChatColor.RED + "Modpack regeneration completed without output. Check your mods folder.");
                    }
                });
            }
            case "host" -> handleHostCommand(sender, args);
            case "config" -> handleConfigCommand(sender, args);
            default -> send(sender, ChatColor.RED + "Unknown sub-command. Try /" + label + " for usage.");
        }
        return true;
    }

    private void handleHostCommand(CommandSender sender, String[] args) {
        if (args.length == 1) {
            boolean running = hostService.isHostRunning();
            ChatColor statusColor = running ? ChatColor.GREEN : ChatColor.RED;
            send(sender, statusColor + "Modpack hosting is " + (running ? "running" : "stopped") + ChatColor.RESET + ".");
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "start" -> runAsync(() -> {
                if (hostService.isHostRunning()) {
                    send(sender, ChatColor.RED + "Modpack hosting is already running.");
                    return;
                }

                send(sender, ChatColor.YELLOW + "Starting modpack host...");
                if (hostService.startHost()) {
                    send(sender, ChatColor.GREEN + "Modpack hosting started!");
                } else {
                    send(sender, ChatColor.RED + "Failed to start modpack hosting. Ensure there is content to host.");
                }
            });
            case "stop" -> runAsync(() -> {
                if (!hostService.isHostRunning()) {
                    send(sender, ChatColor.RED + "Modpack hosting is not running.");
                    return;
                }

                send(sender, ChatColor.YELLOW + "Stopping modpack host...");
                if (hostService.stopHost()) {
                    send(sender, ChatColor.GREEN + "Modpack hosting stopped.");
                } else {
                    send(sender, ChatColor.RED + "Failed to stop modpack hosting.");
                }
            });
            case "restart" -> runAsync(() -> {
                send(sender, ChatColor.YELLOW + "Restarting modpack host...");
                if (hostService.restartHost()) {
                    send(sender, ChatColor.GREEN + "Modpack hosting restarted.");
                } else {
                    send(sender, ChatColor.RED + "Failed to restart modpack hosting.");
                }
            });
            case "connections" -> runAsync(() -> {
                List<String> secrets = hostService.activeConnectionSecrets();
                Map<String, Long> counts = secrets.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(secret -> secret, Collectors.counting()));

                int totalConnections = secrets.size();
                int uniqueConnections = counts.size();

                send(sender, ChatColor.YELLOW + "Active connections: " + totalConnections
                        + " Unique connections: " + uniqueConnections);

                counts.forEach((secret, count) -> {
                    var entry = SecretsStore.getHostSecret(secret);
                    if (entry == null) {
                        send(sender, ChatColor.GREEN + "Secret " + secret + " has " + count + " connection(s).");
                        return;
                    }

                    String playerId = entry.getKey();
                    String displayName = playerId;
                    try {
                        UUID uuid = UUID.fromString(playerId);
                        OfflinePlayer player = plugin.getServer().getOfflinePlayer(uuid);
                        if (player.getName() != null) {
                            displayName = player.getName();
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Leave displayName as playerId when UUID parsing fails.
                    }

                    send(sender, ChatColor.GREEN + "Player " + displayName + " (" + playerId
                            + ") is downloading with " + count + " connection(s).");
                });
            });
            case "fingerprint" -> {
                String fingerprint = hostService.certificateFingerprint();
                if (fingerprint == null || fingerprint.isBlank()) {
                    send(sender, ChatColor.RED + "Certificate fingerprint is not available. Start the host first.");
                } else {
                    send(sender, ChatColor.YELLOW + "Certificate fingerprint: " + ChatColor.WHITE + fingerprint);
                }
            }
            default -> send(sender, ChatColor.RED + "Unknown host command. Try start, stop, restart, connections, or fingerprint.");
        }
    }

    private void handleConfigCommand(CommandSender sender, String[] args) {
        if (args.length < 2 || !"reload".equalsIgnoreCase(args[1])) {
            send(sender, ChatColor.RED + "Unknown config command. Did you mean /automodpack config reload?");
            return;
        }

        runAsync(() -> {
            if (hostService.reloadServerConfigFromDisk()) {
                send(sender, ChatColor.GREEN + "AutoModpack server config reloaded from disk.");
            } else {
                send(sender, ChatColor.RED + "Failed to reload automodpack-server.json. Check the server logs.");
            }
        });
    }

    private void sendAbout(CommandSender sender) {
        send(sender, ChatColor.GREEN + "AutoModpack " + ChatColor.WHITE + AM_VERSION);
        send(sender, ChatColor.YELLOW + "/automodpack generate");
        send(sender, ChatColor.YELLOW + "/automodpack host start|stop|restart|connections|fingerprint");
        send(sender, ChatColor.YELLOW + "/automodpack config reload");
    }

    private void runAsync(Runnable task) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
    }

    private void send(CommandSender sender, String message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }

        if (args.length == 1) {
            return partialMatches(args[0], List.of("generate", "host", "config"));
        }

        if (args.length == 2 && "host".equalsIgnoreCase(args[0])) {
            return partialMatches(args[1], List.of("start", "stop", "restart", "connections", "fingerprint"));
        }

        if (args.length == 2 && "config".equalsIgnoreCase(args[0])) {
            return partialMatches(args[1], List.of("reload"));
        }

        return List.of();
    }

    private List<String> partialMatches(String token, List<String> options) {
        List<String> results = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, results);
        return results;
    }
}
