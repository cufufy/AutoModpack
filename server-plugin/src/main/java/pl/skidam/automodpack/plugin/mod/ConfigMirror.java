package pl.skidam.automodpack.plugin.mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import pl.skidam.automodpack.plugin.util.PathUtils;

public final class ConfigMirror implements AutoCloseable {
    private final Path hostConfigDir;
    private final Path mirrorDir;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "AutoModpack-ConfigMirror");
        thread.setDaemon(true);
        return thread;
    });

    public ConfigMirror(Path hostConfigDir, Path mirrorDir) {
        this.hostConfigDir = Objects.requireNonNull(hostConfigDir, "hostConfigDir");
        this.mirrorDir = Objects.requireNonNull(mirrorDir, "mirrorDir");
    }

    public void synchronizeAsync() {
        executor.submit(() -> {
            try {
                synchronize();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public synchronized void synchronize() throws IOException {
        Files.createDirectories(hostConfigDir);
        Files.createDirectories(mirrorDir);
        PathUtils.deleteContents(mirrorDir);
        PathUtils.copyTree(hostConfigDir, mirrorDir);
    }

    public Path mirrorDir() {
        return mirrorDir;
    }

    public Instant lastSyncTime() throws IOException {
        if (Files.notExists(mirrorDir)) {
            return Instant.EPOCH;
        }
        return Files.getLastModifiedTime(mirrorDir).toInstant();
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }
}
