package com.rafex.housedb.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

    private String mintInternal(final String sub, final Collection<String> roles, final long ttlSeconds,
            final TokenType tokenType, final String clientId) {
        final String[] safeRoles = roles == null ? new String[0] : roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toArray(String[]::new);

        final var builder = TokenSpec.builder()
                .subject(sub)
                .issuer(iss)
                .audience(aud)
                .tokenType(tokenType)
                .roles(safeRoles)
                .ttl(Duration.ofSeconds(ttlSeconds));
        if (clientId != null && !clientId.isBlank()) {
            builder.clientId(clientId);
        }
        return tokenIssuer.issue(builder.build());
    }

    public VerificationResult verify(final String token, final long nowEpochSeconds) {
        return tokenVerifier.verify(token, Instant.ofEpochSecond(nowEpochSeconds));
    }
}
