package org.hyzionstudios.hyextras.util;

import org.hyzionstudios.hyextras.HyExtrasPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Offloads player-data disk writes from the event thread and coalesces rapid repeated saves for the
 * same key (player) into a single trailing write. Each caller snapshots its in-memory state and hands
 * a self-contained write task here, so there is no race against the live state being mutated/cleared.
 *
 * <p>{@link #schedule} debounces (used for non-critical saves). {@link #flush} cancels any pending
 * debounced write and runs the latest task immediately on the calling thread, guaranteeing durability
 * before disconnect cleanup clears memory.
 */
public final class AsyncSaver {

    private final ScheduledExecutorService executor;
    private final long debounceMs;
    private final Map<String, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    private final Map<String, Runnable> latest = new ConcurrentHashMap<>();

    public AsyncSaver(String threadName, long debounceMs) {
        this.debounceMs = Math.max(0L, debounceMs);
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Schedules a debounced background write, replacing any pending write for the same key. */
    public void schedule(String key, Runnable writeTask) {
        if (key == null || writeTask == null) {
            return;
        }
        latest.put(key, writeTask);
        ScheduledFuture<?> previous = pending.remove(key);
        if (previous != null) {
            previous.cancel(false);
        }
        try {
            pending.put(key, executor.schedule(() -> runLatest(key), debounceMs, TimeUnit.MILLISECONDS));
        } catch (java.util.concurrent.RejectedExecutionException rejected) {
            // Executor is shutting down — write synchronously so nothing is lost.
            runTask(writeTask);
            latest.remove(key);
        }
    }

    private void runLatest(String key) {
        pending.remove(key);
        runTask(latest.remove(key));
    }

    /** Cancels any pending debounced write for the key and runs the given task now on the calling thread. */
    public void flush(String key, Runnable writeTask) {
        if (key != null) {
            ScheduledFuture<?> previous = pending.remove(key);
            if (previous != null) {
                previous.cancel(false);
            }
            latest.remove(key);
        }
        runTask(writeTask);
    }

    private static void runTask(Runnable task) {
        if (task == null) {
            return;
        }
        try {
            task.run();
        } catch (Exception e) {
            HyExtrasPlugin plugin = HyExtrasPlugin.get();
            if (plugin != null && plugin.getLogger() != null) {
                plugin.getLogger().at(Level.WARNING).withCause(e)
                        .log("[hextras persistence] background save failed");
            }
        }
    }

    /** Runs any remaining pending writes synchronously and stops the executor. */
    public void shutdown() {
        executor.shutdown();
        for (Map.Entry<String, Runnable> entry : latest.entrySet()) {
            runTask(entry.getValue());
        }
        latest.clear();
        pending.clear();
    }

    /**
     * Atomically writes {@code props} to {@code file} (temp file + atomic rename), creating parent dirs.
     * Falls back to a plain replace when the filesystem rejects an atomic move.
     */
    public static void writeProperties(Path file, Properties props) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (var writer = Files.newBufferedWriter(tmp)) {
            props.store(writer, null);
        }
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicFailed) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
