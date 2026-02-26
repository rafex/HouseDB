package com.rafex.housedb.repository.impl;

import com.rafex.housedb.repository.AppClientRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

public final class AppClientRepositoryImpl implements AppClientRepository {

    private final DataSource ds;

    public AppClientRepositoryImpl(final DataSource ds) {
        this.ds = ds;
    }

    @Override
    public void createClient(final UUID appClientId, final String clientId, final String name, final byte[] secretHash,
            final byte[] salt, final int iterations, final List<String> roles) throws SQLException {
        final var sql = """
                INSERT INTO app_clients (
                    app_client_id, client_id, name, secret_hash, salt, iterations, roles, status, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'active', NOW(), NOW())
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setObject(1, appClientId);
            ps.setString(2, clientId);
            ps.setString(3, name);
            ps.setBytes(4, secretHash);
            ps.setBytes(5, salt);
            ps.setInt(6, iterations);
            ps.setArray(7, c.createArrayOf("text", normalizeRoles(roles).toArray(new String[0])));
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<AppClientRow> findByClientId(final String clientId) throws SQLException {
        final var sql = """
                SELECT app_client_id, client_id, name, secret_hash, salt, iterations,
                       roles, status, last_used_at, created_at, updated_at
                FROM app_clients
                WHERE client_id = ?
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, clientId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AppClientRow(rs.getObject("app_client_id", UUID.class), rs.getString("client_id"),
                        rs.getString("name"), rs.getBytes("secret_hash"), rs.getBytes("salt"), rs.getInt("iterations"),
                        toStringList(rs.getArray("roles")), rs.getString("status"), asInstant(rs, "last_used_at"),
                        asInstant(rs, "created_at"), asInstant(rs, "updated_at")));
            }
        }
    }

    @Override
    public void touchLastUsed(final UUID appClientId) throws SQLException {
        final var sql = """
                UPDATE app_clients
                SET last_used_at = NOW(),
                    updated_at = NOW()
                WHERE app_client_id = ?
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setObject(1, appClientId);
            ps.executeUpdate();
        }
    }

    private static List<String> normalizeRoles(final List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream().filter(r -> r != null && !r.isBlank()).map(String::trim).distinct().toList();
    }

    private static List<String> toStringList(final java.sql.Array arr) throws SQLException {
        if (arr == null) {
            return List.of();
        }
        final var raw = arr.getArray();
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof final Object[] values) {
            return java.util.Arrays.stream(values).filter(java.util.Objects::nonNull).map(String::valueOf)
                    .filter(s -> !s.isBlank()).map(String::trim).toList();
        }
        return List.of();
    }

    private static Instant asInstant(final ResultSet rs, final String column) throws SQLException {
        final Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }
}
