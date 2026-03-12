package com.rafex.housedb.handlers;

import com.rafex.housedb.security.JwtService;
import com.rafex.housedb.services.AppClientAuthService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import com.fasterxml.jackson.databind.JsonNode;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.json.JsonUtils;

public final class TokenHandler {

    private final JwtService jwt;
    private final AppClientAuthService authService;
    private final long ttlSeconds;

    public TokenHandler(final JwtService jwt, final AppClientAuthService authService) {
        this(jwt, authService, Long.parseLong(System.getenv().getOrDefault("JWT_APP_TTL_SECONDS", "1800")));
    }

    public TokenHandler(final JwtService jwt, final AppClientAuthService authService, final long ttlSeconds) {
        this.jwt = Objects.requireNonNull(jwt);
        this.authService = Objects.requireNonNull(authService);
        this.ttlSeconds = ttlSeconds;
    }

    public boolean handle(final HttpExchange x) throws Exception {
        final Request request = ExchangeAdapters.request(x);
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            x.json(HttpStatus.METHOD_NOT_ALLOWED_405, Map.of("error", "method_not_allowed"));
            return true;
        }

        final var authz = request.getHeaders().get("authorization");
        if (authz != null && authz.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
            final var creds = decodeBasic(authz.substring("Basic ".length()).trim());
            if (creds == null) {
                x.json(HttpStatus.UNAUTHORIZED_401,
                        Map.of("error", "unauthorized", "code", "invalid_client", "timestamp", Instant.now().toString()));
                return true;
            }
            return authenticateAndMint(x, creds.clientId(), creds.clientSecret(), "client_credentials");
        }

        final String body;
        try {
            body = Content.Source.asString(request, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            x.json(HttpStatus.BAD_REQUEST_400,
                    Map.of("error", "bad_request", "message", "cannot_read_body", "timestamp", Instant.now().toString()));
            return true;
        }

        if (body == null || body.isBlank()) {
            x.json(HttpStatus.BAD_REQUEST_400,
                    Map.of("error", "bad_request", "message", "missing_body", "timestamp", Instant.now().toString()));
            return true;
        }

        final var contentType = request.getHeaders().get("content-type");
        if (contentType != null && contentType.toLowerCase().contains("application/json")) {
            final JsonNode json;
            try {
                json = JsonUtils.parseTree(body);
            } catch (final Exception e) {
                x.json(HttpStatus.BAD_REQUEST_400,
                        Map.of("error", "bad_request", "message", "invalid_json", "timestamp", Instant.now().toString()));
                return true;
            }

            return authenticateAndMint(x, text(json, "client_id"), text(json, "client_secret"),
                    text(json, "grant_type"));
        }

        final var form = new MultiMap<String>();
        UrlEncoded.decodeTo(body, form, StandardCharsets.UTF_8);
        return authenticateAndMint(x, value(form, "client_id"), value(form, "client_secret"),
                value(form, "grant_type"));
    }

    private boolean authenticateAndMint(final HttpExchange x, final String clientId,
            final String clientSecret, final String grantType) throws Exception {
        if (grantType == null || !"client_credentials".equals(grantType)) {
            x.json(HttpStatus.BAD_REQUEST_400,
                    Map.of("error", "bad_request", "message", "unsupported_grant_type", "timestamp", Instant.now().toString()));
            return true;
        }

        if (clientId == null || clientSecret == null) {
            x.json(HttpStatus.UNAUTHORIZED_401,
                    Map.of("error", "unauthorized", "code", "invalid_client", "timestamp", Instant.now().toString()));
            return true;
        }

        final var result = authService.authenticate(clientId, clientSecret.toCharArray());
        if (!result.ok()) {
            final var code = result.code() != null ? result.code() : "invalid_client";
            if ("client_disabled".equals(code)) {
                x.json(HttpStatus.FORBIDDEN_403,
                        Map.of("error", "forbidden", "code", "client_disabled", "timestamp", Instant.now().toString()));
            } else if ("invalid_client".equals(code)) {
                x.json(HttpStatus.UNAUTHORIZED_401,
                        Map.of("error", "unauthorized", "code", "invalid_client", "timestamp", Instant.now().toString()));
            } else {
                x.json(HttpStatus.UNAUTHORIZED_401,
                        Map.of("error", "unauthorized", "code", code));
            }
            return true;
        }

        final var token = jwt.mintApp("app:" + result.clientId(), result.clientId(), result.roles(), ttlSeconds);
        x.json(200, Map.of("token_type", "Bearer", "access_token", token, "expires_in", ttlSeconds, "grant_type",
                        "client_credentials"));
        return true;
    }

    private static String text(final JsonNode node, final String field) {
        if (node == null) {
            return null;
        }
        final var v = node.get(field);
        return v != null && v.isTextual() ? v.asText() : null;
    }

    private static String value(final MultiMap<String> form, final String field) {
        final var v = form.getValue(field);
        return v == null || v.isBlank() ? null : v;
    }

    private record BasicCreds(String clientId, String clientSecret) {
    }

    private static BasicCreds decodeBasic(final String base64Part) {
        try {
            final var decoded = new String(Base64.getDecoder().decode(base64Part), StandardCharsets.UTF_8);
            final var idx = decoded.indexOf(':');
            if (idx <= 0) {
                return null;
            }
            return new BasicCreds(decoded.substring(0, idx), decoded.substring(idx + 1));
        } catch (final Exception e) {
            return null;
        }
    }
}
