package com.rafex.housedb;

import com.rafex.housedb.bootstrap.HouseDbBootstrap;
import com.rafex.housedb.server.HouseDBServer;

import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private App() {
    }

    public static void main(final String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        configureLogging(args);
        LOG.info("Starting HouseDB backend...");

        try (var runtime = HouseDbBootstrap.start()) {
            HouseDBServer.start(runtime.container());
        }
    }

    private static void configureLogging(final String[] args) {
        var levelStr = System.getenv().getOrDefault("LOG_LEVEL", "INFO");

        for (final String arg : args) {
            if (arg != null && arg.startsWith("--log=")) {
                levelStr = arg.substring("--log=".length());
                break;
            }
        }

        final var level = parseAllowedLevel(levelStr);
        final var root = Logger.getLogger("");
        root.setLevel(level);
        for (final Handler handler : root.getHandlers()) {
            handler.setLevel(level);
        }

        LOG.info("Log level set to " + level.getName());
    }

    private static Level parseAllowedLevel(final String value) {
        if (value == null || value.isBlank()) {
            return Level.INFO;
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "DEBUG" -> Level.FINE;
            case "INFO" -> Level.INFO;
            case "WARN" -> Level.WARNING;
            case "ERROR" -> Level.SEVERE;
            default -> Level.INFO;
        };
    }
}
