package com.rafex.housedb.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import dev.rafex.ether.jwt.DefaultTokenIssuer;
import dev.rafex.ether.jwt.DefaultTokenVerifier;
import dev.rafex.ether.jwt.JwtConfig;
import dev.rafex.ether.jwt.KeyProvider;
import dev.rafex.ether.jwt.TokenIssuer;
import dev.rafex.ether.jwt.TokenSpec;
import dev.rafex.ether.jwt.TokenType;
import dev.rafex.ether.jwt.TokenVerifier;
import dev.rafex.ether.jwt.VerificationResult;

public final class JwtService {

    private final String iss;
    private final String aud;
    private final TokenIssuer tokenIssuer;
    private final TokenVerifier tokenVerifier;

    public JwtService(final String iss, final String aud, final String secret) {
        this.iss = Objects.requireNonNull(iss, "iss");
        this.aud = Objects.requireNonNull(aud, "aud");
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET demasiado corto (usa >= 32 chars).");
        }

        final var config = JwtConfig.builder(KeyProvider.hmac(secret))
                .expectedIssuer(iss)
                .expectedAudience(aud)
                .build();
        this.tokenIssuer = new DefaultTokenIssuer(config);
        this.tokenVerifier = new DefaultTokenVerifier(config);
    }

    public String mint(final String sub, final long ttlSeconds) {
        return mint(sub, List.of(), ttlSeconds);
    }

    public String mint(final String sub, final Collection<String> roles, final long ttlSeconds) {
        return mintInternal(sub, roles, ttlSeconds, TokenType.USER, null);
    }

    public String mintApp(final String sub, final String clientId, final Collection<String> roles, final long ttlSeconds) {
        return mintInternal(sub, roles, ttlSeconds, TokenType.APP, clientId);
    }

    public IssuedToken mintAccess(final String sub, final Collection<String> roles, final long ttlSeconds) {
        return mintDetailed(sub, roles, ttlSeconds, TokenType.USER, null, "access");
    }

    public IssuedToken mintAppAccess(final String sub, final String clientId, final Collection<String> roles,
            final long ttlSeconds) {
        return mintDetailed(sub, roles, ttlSeconds, TokenType.APP, clientId, "access");
    }

    public IssuedToken mintRefresh(final String sub, final UUID tokenFamilyId, final long ttlSeconds) {
        return mintDetailed(sub, List.of(), ttlSeconds, TokenType.USER, null, "refresh", "token_family_id",
                tokenFamilyId == null ? null : tokenFamilyId.toString());
    }

    private String mintInternal(final String sub, final Collection<String> roles, final long ttlSeconds,
            final TokenType tokenType, final String clientId) {
        return mintDetailed(sub, roles, ttlSeconds, tokenType, clientId, null).token();
    }

    private IssuedToken mintDetailed(final String sub, final Collection<String> roles, final long ttlSeconds,
            final TokenType tokenType, final String clientId, final String tokenUse, final Object... extraClaimPairs) {
        final String[] safeRoles = roles == null ? new String[0] : roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toArray(String[]::new);

        final Instant issuedAt = Instant.now();
        final Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
        final String jwtId = UUID.randomUUID().toString();

        final var builder = TokenSpec.builder()
                .subject(sub)
                .issuer(iss)
                .audience(aud)
                .tokenType(tokenType)
                .roles(safeRoles)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .jwtId(jwtId);
        if (clientId != null && !clientId.isBlank()) {
            builder.clientId(clientId);
        }
        if (tokenUse != null && !tokenUse.isBlank()) {
            builder.claim("token_use", tokenUse);
        }
        if (extraClaimPairs != null) {
            for (int i = 0; i + 1 < extraClaimPairs.length; i += 2) {
                final Object rawKey = extraClaimPairs[i];
                if (rawKey instanceof String key && key != null && !key.isBlank()) {
                    builder.claim(key, extraClaimPairs[i + 1]);
                }
            }
        }
        return new IssuedToken(tokenIssuer.issue(builder.build()), jwtId, expiresAt);
    }

    public VerificationResult verify(final String token, final long nowEpochSeconds) {
        return tokenVerifier.verify(token, Instant.ofEpochSecond(nowEpochSeconds));
    }

    public record IssuedToken(String token, String jwtId, Instant expiresAt) {
    }
}
