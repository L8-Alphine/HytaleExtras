package org.hyzionstudios.hyextras.config;

import org.hyzionstudios.hyextras.HyExtrasPlugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Loads {@link HyExtrasConfig} from {@code hyextras.properties} in the plugin data directory.
 * Writes a default file on first run if none exists.
 */
public final class ConfigLoader {

    private static final String FILENAME = "hyextras.properties";

    private ConfigLoader() {}

    public static HyExtrasConfig load(Path dataDir) {
        HyExtrasConfig cfg = new HyExtrasConfig();
        Path file = dataDir.resolve(FILENAME);
        if (!Files.exists(file)) {
            writeDefaults(cfg, file);
            return cfg;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            props.load(reader);
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[HyExtras] Failed to read config — using defaults");
            return cfg;
        }
        cfg.advancedPacketActions = Boolean.parseBoolean(
                props.getProperty("advancedPacketActions", String.valueOf(cfg.advancedPacketActions)));
        cfg.debugMode = Boolean.parseBoolean(
                props.getProperty("debugMode", String.valueOf(cfg.debugMode)));
        return cfg;
    }

    private static void writeDefaults(HyExtrasConfig cfg, Path file) {
        try {
            Files.createDirectories(file.getParent());
            Properties props = new Properties();
            props.setProperty("advancedPacketActions", String.valueOf(cfg.advancedPacketActions));
            props.setProperty("debugMode", String.valueOf(cfg.debugMode));
            try (Writer writer = Files.newBufferedWriter(file)) {
                props.store(writer, "HyExtras Configuration");
            }
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[HyExtras] Failed to write default config file");
        }
    }
}
