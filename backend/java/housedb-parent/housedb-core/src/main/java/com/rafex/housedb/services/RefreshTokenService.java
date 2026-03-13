package com.rafex.housedb.services;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

public interface RefreshTokenService {

    void issueRefreshToken(UUID userId, UUID tokenFamilyId, String jwtId, Instant expiresAt, String parentJwtId)
            throws SQLException;

    RotateResult rotateRefreshToken(String currentJwtId, String newJwtId, Instant newExpiresAt, Instant now)
            throws SQLException;

    void revokeFamily(UUID tokenFamilyId, Instant now) throws SQLException;

    enum RotateStatus {
        SUCCESS,
        NOT_FOUND,
        EXPIRED,
        USED,
        REVOKED
    }

    record RotateResult(RotateStatus status, UUID userId, UUID tokenFamilyId) {
    }
}
