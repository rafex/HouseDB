package com.rafex.housedb.repository.impl;

import com.rafex.housedb.repository.UserRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

public final class UserRepositoryImpl implements UserRepository {

    private final DataSource ds;

    public UserRepositoryImpl(final DataSource ds) {
        this.ds = ds;
    }

    @Override
    public void createUser(final UUID userId, final String username, final byte[] passwordHash, final byte[] salt,
            final int iterations) throws SQLException {
        final var sql = """
                INSERT INTO users (user_id, username, password_hash, salt, iterations, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'active', NOW(), NOW())
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ps.setString(2, username);
            ps.setBytes(3, passwordHash);
            ps.setBytes(4, salt);
            ps.setInt(5, iterations);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<UserRow> findByUsername(final String username) throws SQLException {
        final var sql = """
                SELECT user_id, username, password_hash, salt, iterations, status, created_at, updated_at
                FROM users
                WHERE username = ?
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, username);

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                return Optional.of(new UserRow(rs.getObject("user_id", UUID.class), rs.getString("username"),
                        rs.getBytes("password_hash"), rs.getBytes("salt"), rs.getInt("iterations"),
                        rs.getString("status"), asInstant(rs, "created_at"), asInstant(rs, "updated_at")));
            }
        }
    }

    @Override
    public List<String> findRoleNamesByUserId(final UUID userId) throws SQLException {
        final var sql = """
                SELECT r.name
                FROM user_roles ur
                JOIN users u ON u.id = ur.user_fk
                JOIN roles r ON r.id = ur.role_fk
                WHERE u.user_id = ?
                  AND r.status = 'active'
                ORDER BY r.name
                """;

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setObject(1, userId);

            try (var rs = ps.executeQuery()) {
                final var out = new ArrayList<String>();
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
                return out;
            }
        }
    }

    @Override
    public Optional<UserWithRoles> findByUsernameWithRoles(final String username) throws SQLException {
        final var userOpt = findByUsername(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        final var user = userOpt.get();
        final var roles = findRoleNamesByUserId(user.userId());
        return Optional.of(new UserWithRoles(user, roles));
    }

    @Override
    public int countUsers() throws SQLException {
        try (var c = ds.getConnection();
                var ps = c.prepareStatement("SELECT count(*) FROM users");
                var rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static Instant asInstant(final ResultSet rs, final String column) throws SQLException {
        final Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toInstant() : null;
    }
}
