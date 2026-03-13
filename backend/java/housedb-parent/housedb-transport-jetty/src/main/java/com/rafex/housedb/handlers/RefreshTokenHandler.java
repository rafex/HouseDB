package com.rafex.housedb.handlers;

import com.rafex.housedb.dtos.RefreshTokenRequest;
import com.rafex.housedb.handlers.support.EtherJettyErrors;
import com.rafex.housedb.security.JwtService;
import com.rafex.housedb.services.RefreshTokenService;
import com.rafex.housedb.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.json.JsonCodec;

public final class RefreshTokenHandler {

    private final JsonCodec jsonCodec;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public RefreshTokenHandler(final JsonCodec jsonCodec, final JwtService jwt,
            final RefreshTokenService refreshTokenService, final UserRepository userRepository) {
        this(jsonCodec, jwt, refreshTokenService, userRepository,
                Long.parseLong(System.getenv().getOrDefault("JWT_ACCESS_TTL_SECONDS",
                        System.getenv().getOrDefault("JWT_TTL_SECONDS", "900"))),
                Long.parseLong(System.getenv().getOrDefault("JWT_REFRESH_TTL_SECONDS", "604800")));
    }

    public RefreshTokenHandler(final JsonCodec jsonCodec, final JwtService jwt,
            final RefreshTokenService refreshTokenService, final UserRepository userRepository,
            final long accessTtlSeconds, final long refreshTtlSeconds) {
        this.jsonCodec = Objects.requireNonNull(jsonCodec);
        this.jwt = Objects.requireNonNull(jwt);
        this.refreshTokenService = Objects.requireNonNull(refreshTokenService);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public boolean handle(final HttpExchange x) throws Exception {
        final Request request = ExchangeAdapters.request(x);
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            x.methodNotAllowed();
            return true;
        }

        final var refreshToken = resolveRefreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            EtherJettyErrors.badRequest(x, "missing_refresh_token");
            return true;
        }

        final var verification = jwt.verify(refreshToken, Instant.now().getEpochSecond());
        if (!verification.ok() || verification.claims().isEmpty()) {
            EtherJettyErrors.unauthorized(x, "invalid_refresh_token");
            return true;
        }

        final var claims = verification.claims().orElseThrow();
        if (!"refresh".equals(String.valueOf(claims.extras().get("token_use")))) {
            EtherJettyErrors.unauthorized(x, "invalid_refresh_token");
            return true;
        }
        if (claims.subject() == null || claims.jwtId() == null || claims.jwtId().isBlank()) {
            EtherJettyErrors.unauthorized(x, "invalid_refresh_token");
            return true;
        }

        final UUID userId;
        try {
            userId = UUID.fromString(claims.subject());
        } catch (final IllegalArgumentException e) {
            EtherJettyErrors.unauthorized(x, "invalid_refresh_token");
            return true;
        }

        final var userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty() || userOpt.get().status() == null || !"active".equalsIgnoreCase(userOpt.get().status())) {
            EtherJettyErrors.forbidden(x, "user_disabled");
            return true;
        }

        final UUID tokenFamilyId;
        try {
            tokenFamilyId = UUID.fromString(String.valueOf(claims.extras().get("token_family_id")));
        } catch (final IllegalArgumentException e) {
            EtherJettyErrors.unauthorized(x, "invalid_refresh_token");
            return true;
        }

        final var now = Instant.now();
        final var newRefresh = jwt.mintRefresh(userId.toString(), tokenFamilyId, refreshTtlSeconds);
        final var rotation = refreshTokenService.rotateRefreshToken(claims.jwtId(), newRefresh.jwtId(),
                newRefresh.expiresAt(), now);

        if (rotation.status() != RefreshTokenService.RotateStatus.SUCCESS) {
            if (rotation.tokenFamilyId() != null
                    && (rotation.status() == RefreshTokenService.RotateStatus.USED
                            || rotation.status() == RefreshTokenService.RotateStatus.REVOKED)) {
                refreshTokenService.revokeFamily(rotation.tokenFamilyId(), now);
            }
            EtherJettyErrors.unauthorized(x, "invalid_refresh_token");
            return true;
        }

        final var roles = userRepository.findRoleNamesByUserId(userId);
        final var accessToken = jwt.mintAccess(userId.toString(), roles, accessTtlSeconds);
        x.json(200, Map.of(
                "token_type", "Bearer",
                "access_token", accessToken.token(),
                "expires_in", accessTtlSeconds,
                "refresh_token", newRefresh.token(),
                "refresh_expires_in", refreshTtlSeconds));
        return true;
    }

    private String resolveRefreshToken(final Request request) throws Exception {
        final var authz = request.getHeaders().get("authorization");
        if (authz != null && authz.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            final var bearer = authz.substring("Bearer ".length()).trim();
            if (!bearer.isBlank()) {
                return bearer;
            }
        }

        final String body;
        try {
            body = Content.Source.asString(request, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            throw new IllegalArgumentException("cannot_read_body");
        }

        if (body == null || body.isBlank()) {
            return null;
        }

        try {
            final var dto = jsonCodec.readValue(body, RefreshTokenRequest.class);
            return dto == null ? null : dto.refreshToken();
        } catch (final Exception e) {
            throw new IllegalArgumentException("invalid_json");
        }
    }
}
