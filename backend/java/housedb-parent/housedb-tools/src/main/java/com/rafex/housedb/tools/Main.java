package com.rafex.housedb.tools;

import com.rafex.housedb.db.Db;
import com.rafex.housedb.security.PasswordHasherPBKDF2;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.sql.DataSource;

public final class Main {

    private static final String SQL_CREATE_USER = """
            INSERT INTO users (user_id, username, password_hash, salt, iterations, status, enabled, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 'active', TRUE, NOW(), NOW())
            """;
    private static final String SQL_ENSURE_ROLE = """
            INSERT INTO roles (role_id, name, description, status, created_at, updated_at)
            VALUES (gen_random_uuid(), ?, 'Provisioned by housedb-tools', 'active', NOW(), NOW())
            ON CONFLICT (name) DO NOTHING
            """;
    private static final String SQL_ASSIGN_ROLE = """
            INSERT INTO user_roles (user_fk, role_fk, assigned_at)
            SELECT u.id, r.id, NOW()
            FROM users u
            JOIN roles r ON r.name = ?
            WHERE u.user_id = ?
            ON CONFLICT (user_fk, role_fk) DO NOTHING
            """;

    private Main() {
    }

    public static void main(final String[] args) throws Exception {
        final var a = Args.parse(args);
        if (a.help) {
            printHelp();
            return;
        }

        final var username = required("username", a.username);
        final var userId = a.userId != null && !a.userId.isBlank() ? UUID.fromString(a.userId) : UUID.randomUUID();
        final var password = readPassword(a);
        final var roles = parseRoles(a.roles);

        final int hashBytes = Math.max(16, parseIntEnv("AUTH_HASH_BYTES", 32));
        final int saltBytes = Math.max(16, parseIntEnv("AUTH_SALT_BYTES", 16));
        final int iterations = Math.max(10000, parseIntEnv("AUTH_PBKDF2_ITERATIONS", 120000));

        final var hasher = new PasswordHasherPBKDF2(hashBytes);
        final var salt = new byte[saltBytes];
        new SecureRandom().nextBytes(salt);

        final var hashResult = hasher.hash(password, salt, iterations);
        Arrays.fill(password, '\0');

        final DataSource ds = Db.dataSource();
        try {
            createUserWithRoles(ds, userId, username, hashResult.hash(), hashResult.salt(), hashResult.iterations(),
                    roles);

            System.out.println("OK: user created");
            System.out.println("user_id=" + userId);
            System.out.println("username=" + username);
            System.out.println("roles=" + roles);
        } finally {
            closeDataSource(ds);
        }
    }

    private static void createUserWithRoles(final DataSource ds, final UUID userId, final String username,
            final byte[] hash, final byte[] salt, final int iterations, final List<String> roles) throws SQLException {
        try (Connection c = ds.getConnection()) {
            final boolean prev = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (var ps = c.prepareStatement(SQL_CREATE_USER)) {
                    ps.setObject(1, userId);
                    ps.setString(2, username);
                    ps.setBytes(3, hash);
                    ps.setBytes(4, salt);
                    ps.setInt(5, iterations);
                    ps.executeUpdate();
                }

                for (final var role : roles) {
                    try (var ps = c.prepareStatement(SQL_ENSURE_ROLE)) {
                        ps.setString(1, role);
                        ps.executeUpdate();
                    }
                    try (var ps = c.prepareStatement(SQL_ASSIGN_ROLE)) {
                        ps.setString(1, role);
                        ps.setObject(2, userId);
                        ps.executeUpdate();
                    }
                }

                c.commit();
            } catch (final SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(prev);
            }
        }
    }

    private static char[] readPassword(final Args a) {
        if (a.password != null && !a.password.isBlank()) {
            return a.password.toCharArray();
        }
        if (a.passwordEnv != null && !a.passwordEnv.isBlank()) {
            final var value = System.getenv(a.passwordEnv);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("password env var is empty: " + a.passwordEnv);
            }
            return value.toCharArray();
        }
        final var console = System.console();
        if (console != null) {
            final var p = console.readPassword("Password for %s: ", a.username != null ? a.username : "user");
            if (p == null || p.length == 0) {
                throw new IllegalArgumentException("password is empty");
            }
            return p;
        }
        throw new IllegalArgumentException("missing --password or --password-env (no console available)");
    }

    private static List<String> parseRoles(final String rolesCsv) {
        if (rolesCsv == null || rolesCsv.isBlank()) {
            return List.of("USER");
        }
        final var out = new ArrayList<String>();
        for (final var p : rolesCsv.split(",")) {
            final var role = p.trim().toUpperCase(Locale.ROOT);
            if (!role.isEmpty()) {
                out.add(role);
            }
        }
        if (out.isEmpty()) {
            return List.of("USER");
        }
        return out;
    }

    private static int parseIntEnv(final String key, final int def) {
        final var raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return def;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException e) {
            return def;
        }
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing --" + name);
        }
        return value;
    }

    private static void closeDataSource(final DataSource ds) {
        if (ds instanceof AutoCloseable c) {
            try {
                c.close();
            } catch (final Exception ignored) {
                // Best effort shutdown for CLI process termination.
            }
        }
    }

    private static void printHelp() {
        System.out.println("""
                Usage:
                  ./mvnw -q -pl housedb-tools -am exec:java -Dexec.args="--username <name> [--user-id <uuid>] [--password <pass> | --password-env <ENV_VAR>] [--roles USER,ADMIN]"

                Required env vars:
                  DB_URL        e.g. jdbc:postgresql://localhost:5432/housedb
                Optional env vars:
                  DB_USER
                  DB_PASSWORD
                  AUTH_HASH_BYTES (default 32)
                  AUTH_SALT_BYTES (default 16, min 16)
                  AUTH_PBKDF2_ITERATIONS (default 120000, min 10000)

                Example:
                  export DB_URL='jdbc:postgresql://localhost:5432/housedb'
                  export DB_USER='housedb'
                  export DB_PASSWORD='housedb'
                  export ADMIN_PASS='Admin123!'

                  ./mvnw -q -pl housedb-tools -am exec:java -Dexec.args="--username admin --password-env ADMIN_PASS --roles ADMIN,USER"
                """);
    }

    private static final class Args {
        boolean help;
        String userId;
        String username;
        String password;
        String passwordEnv;
        String roles;

        static Args parse(final String[] args) {
            final var a = new Args();
            final var list = Arrays.asList(args);

            for (int i = 0; i < list.size(); i++) {
                final var key = list.get(i);
                if ("-h".equals(key) || "--help".equals(key)) {
                    a.help = true;
                    return a;
                }
                if (key.startsWith("--")) {
                    final var opt = key.substring(2).toLowerCase(Locale.ROOT);
                    final var val = i + 1 < list.size() && !list.get(i + 1).startsWith("--") ? list.get(++i) : null;
                    switch (opt) {
                        case "user-id" -> a.userId = val;
                        case "username" -> a.username = val;
                        case "password" -> a.password = val;
                        case "password-env" -> a.passwordEnv = val;
                        case "roles" -> a.roles = val;
                        default -> throw new IllegalArgumentException("unknown option: " + key);
                    }
                    continue;
                }
                throw new IllegalArgumentException("unexpected arg: " + key);
            }
            return a;
        }
    }
}
