package com.cufufy.amp.plugin.mod;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.cufufy.amp.plugin.util.PathUtils;

public final class ModRepositoryWatcher implements AutoCloseable {
    private final Path sourceModsDir;
    private final Path targetModsDir;
    private final Consumer<Instant> onSync;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AutoModpack-ModWatcher");
        t.setDaemon(true);
        return t;
    });

    public ModRepositoryWatcher(Path sourceModsDir, Path targetModsDir, Consumer<Instant> onSync) {
        this.sourceModsDir = Objects.requireNonNull(sourceModsDir, "sourceModsDir");
        this.targetModsDir = Objects.requireNonNull(targetModsDir, "targetModsDir");
        this.onSync = Objects.requireNonNull(onSync, "onSync");
    }

    public void initialize() throws IOException {
        Files.createDirectories(sourceModsDir);
        Files.createDirectories(targetModsDir);
        synchronize();
        startWatcher();
    }

    private void startWatcher() throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        sourceModsDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

        executor.submit(() -> runWatcher(watchService));
    }

    private void runWatcher(WatchService watchService) {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            boolean relevantChange = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                relevantChange = true;
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }

            if (relevantChange) {
                try {
                    synchronize();
                } catch (IOException e) {
                    // continue watching despite the failure
                    e.printStackTrace();
                }
            }
        }

        try {
            watchService.close();
        } catch (IOException ignored) {
        }
    }

    private synchronized void synchronize() throws IOException {
        PathUtils.deleteContents(targetModsDir);
        PathUtils.copyTree(sourceModsDir, targetModsDir);
        onSync.accept(Instant.now());
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
