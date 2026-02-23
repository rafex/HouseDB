package com.rafex.housedb.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class Db {

    private static final HikariDataSource DS = create();

    private Db() {
    }

    private static HikariDataSource create() {
        final var cfg = new HikariConfig();

        final var dbUrl = System.getenv("DB_URL");
        final var dbUser = System.getenv("DB_USER");
        final var dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException("DB_URL environment variable is not set or is empty");
        }

        cfg.setJdbcUrl(dbUrl);

        if (dbUser != null && !dbUser.isBlank()) {
            cfg.setUsername(dbUser);
        }
        if (dbPassword != null && !dbPassword.isBlank()) {
            cfg.setPassword(dbPassword);
        }

        cfg.setMaximumPoolSize(parseIntEnv("DB_MAX_POOL_SIZE", 6));
        cfg.setMinimumIdle(parseIntEnv("DB_MIN_IDLE", 2));
        cfg.setConnectionTimeout(parseLongEnv("DB_CONNECTION_TIMEOUT_MS", 30000L));
        cfg.setIdleTimeout(parseLongEnv("DB_IDLE_TIMEOUT_MS", 600000L));
        cfg.setMaxLifetime(parseLongEnv("DB_MAX_LIFETIME_MS", 1800000L));
        cfg.setValidationTimeout(parseLongEnv("DB_VALIDATION_TIMEOUT_MS", 5000L));
        cfg.setPoolName("housedb-pool");

        return new HikariDataSource(cfg);
    }

    public static DataSource dataSource() {
        return DS;
    }

    private static int parseIntEnv(final String name, final int def) {
        final var v = System.getenv(name);
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (final NumberFormatException e) {
            return def;
        }
    }

    private static long parseLongEnv(final String name, final long def) {
        final var v = System.getenv(name);
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (final NumberFormatException e) {
            return def;
        }
    }
}
