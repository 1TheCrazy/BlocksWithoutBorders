package com.github.thecrazy.noborders;

import org.apache.logging.log4j.LogManager;
import java.io.*;
import java.util.Properties;

public final class WindowModeConfig {
    private WindowModeConfig() { }

    public static WindowMode load(File gameDirectory, WindowMode fallback) {
        File file = new File(gameDirectory, "config/noborders.properties");
        if (!file.isFile()) return fallback;
        try (InputStream input = new FileInputStream(file)) {
            Properties properties = new Properties();
            properties.load(input);
            return WindowMode.valueOf(properties.getProperty("mode", fallback.name()));
        } catch (IOException | IllegalArgumentException exception) {
            LogManager.getLogger("NoBorders").warn("Could not read window mode", exception);
            return fallback;
        }
    }

    public static void save(File gameDirectory, WindowMode mode) {
        File file = new File(gameDirectory, "config/noborders.properties");
        try {
            if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) {
                throw new IOException("Could not create " + file.getParent());
            }
            Properties properties = new Properties();
            properties.setProperty("mode", mode.name());
            try (OutputStream output = new FileOutputStream(file)) {
                properties.store(output, "No Borders display mode: WINDOWED, FULLSCREEN, BORDERLESS");
            }
        } catch (IOException exception) {
            LogManager.getLogger("NoBorders").warn("Could not save window mode", exception);
        }
    }
}
