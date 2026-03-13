package com.rafex.housedb.services.impl;

import com.rafex.housedb.repository.RefreshTokenRepository;
import com.rafex.housedb.services.RefreshTokenService;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repository;

    public RefreshTokenServiceImpl(final RefreshTokenRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public void issueRefreshToken(final UUID userId, final UUID tokenFamilyId, final String jwtId,
            final Instant expiresAt, final String parentJwtId)
            throws SQLException {
        repository.createRefreshToken(userId, tokenFamilyId, jwtId, expiresAt, parentJwtId);
    }

    @Override
    public RotateResult rotateRefreshToken(final String currentJwtId, final String newJwtId, final Instant newExpiresAt,
            final Instant now)
            throws SQLException {
        final var result = repository.rotateRefreshToken(currentJwtId, newJwtId, newExpiresAt, now);
        return new RotateResult(RotateStatus.valueOf(result.status().name()), result.userId(), result.tokenFamilyId());
    }

    @Override
    public void revokeFamily(final UUID tokenFamilyId, final Instant now) throws SQLException {
        repository.revokeFamily(tokenFamilyId, now);
    }
}
