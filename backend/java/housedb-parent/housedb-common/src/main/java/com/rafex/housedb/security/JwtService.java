package com.rafex.housedb.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.ether.jwt.DefaultTokenIssuer;
import dev.rafex.ether.jwt.DefaultTokenVerifier;
import dev.rafex.ether.jwt.JwtConfig;
import dev.rafex.ether.jwt.KeyProvider;
import dev.rafex.ether.jwt.TokenClaims;
import dev.rafex.ether.jwt.TokenIssuer;
import dev.rafex.ether.jwt.TokenSpec;
import dev.rafex.ether.jwt.TokenType;
import dev.rafex.ether.jwt.TokenVerifier;

public final class JwtService {

    public record AuthContext(String sub, long exp, String iss, String aud, List<String> roles, String tokenType,
            String clientId) {
    }

    public record VerifyResult(boolean ok, AuthContext ctx, String code) {
        public static VerifyResult ok(final AuthContext ctx) {
            return new VerifyResult(true, ctx, null);
        }

        public static VerifyResult bad(final String code) {
            return new VerifyResult(false, null, code);
        }
    }

    private final String iss;
    private final String aud;
    private final TokenIssuer tokenIssuer;
    private final TokenVerifier tokenVerifier;

    public JwtService(final ObjectMapper mapper, final String iss, final String aud, final String secret) {
        Objects.requireNonNull(mapper, "mapper");
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

    public VerifyResult verify(final String token, final long nowEpochSeconds) {
        final var result = tokenVerifier.verify(token, Instant.ofEpochSecond(nowEpochSeconds));
        if (!result.ok()) {
            return VerifyResult.bad(result.code());
        }
        final var claims = result.claims().orElse(null);
        if (claims == null) {
            return VerifyResult.bad("verify_exception");
        }
        return VerifyResult.ok(mapAuthContext(claims));
    }

    private AuthContext mapAuthContext(final TokenClaims claims) {
        final var tokenType = claims.tokenType() == null ? "user" : claims.tokenType().claimValue();
        final var audValue = claims.audience().isEmpty() ? null : claims.audience().get(0);
        final var expiresAt = claims.expiresAt() == null ? 0L : claims.expiresAt().getEpochSecond();
        return new AuthContext(
                claims.subject(),
                expiresAt,
                claims.issuer(),
                audValue,
                claims.roles(),
                tokenType,
                claims.clientId());
    }
}
