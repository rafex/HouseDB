package com.rafex.housedb.repository.impl;

import com.rafex.housedb.repository.RefreshTokenRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

public final class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private static final String SQL_INSERT_REFRESH_TOKEN = """
            SELECT api_create_refresh_token(?, ?, ?, ?, ?)
            """;

    private static final String SQL_SELECT_FOR_UPDATE = """
            SELECT status,
                   user_id,
                   token_family_id
              FROM api_rotate_refresh_token(?, ?, ?, ?)
            """;

    private static final String SQL_REVOKE_FAMILY = """
            SELECT api_revoke_refresh_token_family(?, ?)
            """;

    private final DataSource dataSource;

    public RefreshTokenRepositoryImpl(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void createRefreshToken(final UUID userId, final UUID tokenFamilyId, final String jwtId,
            final Instant expiresAt, final String parentJwtId)
            throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_INSERT_REFRESH_TOKEN)) {
            ps.setObject(1, userId);
            ps.setObject(2, tokenFamilyId);
            ps.setString(3, jwtId);
            ps.setTimestamp(4, Timestamp.from(expiresAt));
            ps.setString(5, parentJwtId);
            ps.executeQuery();
        }
    }

    @Override
    public RotateResult rotateRefreshToken(final String currentJwtId, final String newJwtId, final Instant newExpiresAt,
            final Instant now)
            throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_SELECT_FOR_UPDATE)) {
            ps.setString(1, currentJwtId);
            ps.setString(2, newJwtId);
            ps.setTimestamp(3, Timestamp.from(newExpiresAt));
            ps.setTimestamp(4, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return new RotateResult(RotateStatus.NOT_FOUND, null, null);
                }
                return new RotateResult(
                        RotateStatus.valueOf(rs.getString("status")),
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("token_family_id", UUID.class));
            }
        }
    }

    @Override
    public void revokeFamily(final UUID tokenFamilyId, final Instant now) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_REVOKE_FAMILY)) {
            ps.setObject(1, tokenFamilyId);
            ps.setTimestamp(2, Timestamp.from(now));
            ps.executeQuery();
        }
    }
}
