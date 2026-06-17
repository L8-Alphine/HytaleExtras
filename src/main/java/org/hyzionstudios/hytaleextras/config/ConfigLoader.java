package org.hyzionstudios.hytaleextras.config;

import org.hyzionstudios.hytaleextras.HyextrasPlugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Loads {@link HytaleExtrasConfig} from {@code hytaleextras.properties} in the plugin data directory.
 * Writes a default file on first run if none exists.
 */
public final class ConfigLoader {

    private static final String FILENAME = "hytaleextras.properties";

    private ConfigLoader() {}

    public static HytaleExtrasConfig load(Path dataDir) {
        HytaleExtrasConfig cfg = new HytaleExtrasConfig();
        Path file = dataDir.resolve(FILENAME);
        if (!Files.exists(file)) {
            writeDefaults(cfg, file);
            return cfg;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            props.load(reader);
        } catch (IOException e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[HytaleExtras] Failed to read config — using defaults");
            return cfg;
        }
        cfg.advancedPacketActions = Boolean.parseBoolean(
                props.getProperty("advancedPacketActions", String.valueOf(cfg.advancedPacketActions)));
        cfg.debugMode = Boolean.parseBoolean(
                props.getProperty("debugMode", String.valueOf(cfg.debugMode)));
        return cfg;
    }

    private static void writeDefaults(HytaleExtrasConfig cfg, Path file) {
        try {
            Files.createDirectories(file.getParent());
            Properties props = new Properties();
            props.setProperty("advancedPacketActions", String.valueOf(cfg.advancedPacketActions));
            props.setProperty("debugMode", String.valueOf(cfg.debugMode));
            try (Writer writer = Files.newBufferedWriter(file)) {
                props.store(writer, "HytaleExtras Configuration");
            }
        } catch (IOException e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[HytaleExtras] Failed to write default config file");
        }
    }
}
