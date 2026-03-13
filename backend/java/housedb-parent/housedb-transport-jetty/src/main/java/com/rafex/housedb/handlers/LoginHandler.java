package com.rafex.housedb.handlers;

import com.rafex.housedb.handlers.support.EtherJettyErrors;
import com.rafex.housedb.security.JwtService;
import com.rafex.housedb.services.AuthService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;

import com.fasterxml.jackson.databind.JsonNode;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.json.JsonCodec;

public final class LoginHandler {

    private final JsonCodec jsonCodec;
    private final JwtService jwt;
    private final AuthService authService;
    private final long ttlSeconds;

    public LoginHandler(final JsonCodec jsonCodec, final JwtService jwt, final AuthService authService) {
        this(jsonCodec, jwt, authService, Long.parseLong(System.getenv().getOrDefault("JWT_TTL_SECONDS", "3600")));
    }

    public LoginHandler(final JsonCodec jsonCodec, final JwtService jwt, final AuthService authService,
            final long ttlSeconds) {
        this.jsonCodec = Objects.requireNonNull(jsonCodec);
        this.jwt = Objects.requireNonNull(jwt);
        this.authService = Objects.requireNonNull(authService);
        this.ttlSeconds = ttlSeconds;
    }

    public boolean handle(final HttpExchange x) throws Exception {
        final Request request = ExchangeAdapters.request(x);

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            x.methodNotAllowed();
            return true;
        }

        final var authz = request.getHeaders().get("authorization");
        if (authz != null && authz.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
            final var creds = decodeBasic(authz.substring("Basic ".length()).trim());
            if (creds == null) {
                EtherJettyErrors.unauthorized(x, "bad_basic_auth");
                return true;
            }
            return authenticateAndMint(x, creds.user(), creds.pass());
        }

        final String body;
        try {
            body = Content.Source.asString(request, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            EtherJettyErrors.badRequest(x, "cannot_read_body");
            return true;
        }

        if (body == null || body.isBlank()) {
            EtherJettyErrors.unauthorized(x, "missing_credentials");
            return true;
        }

        final JsonNode json;
        try {
            json = jsonCodec.readTree(body);
        } catch (final Exception e) {
            EtherJettyErrors.badRequest(x, "invalid_json");
            return true;
        }

        final var user = text(json, "username");
        final var pass = text(json, "password");

        if (user == null || pass == null) {
            EtherJettyErrors.unauthorized(x, "missing_credentials");
            return true;
        }

        return authenticateAndMint(x, user, pass);
    }

    private boolean authenticateAndMint(final HttpExchange x, final String username,
            final String password) throws Exception {

        final var result = authService.authenticate(username, password.toCharArray());

        if (!result.ok()) {
            final var code = result.code() != null ? result.code() : "bad_credentials";

            if ("user_disabled".equals(code)) {
                EtherJettyErrors.forbidden(x, "user_disabled");
            } else if ("bad_credentials".equals(code)) {
                EtherJettyErrors.unauthorized(x, "bad_credentials");
            } else {
                EtherJettyErrors.unauthorized(x, code);
            }
            return true;
        }

        final var token = jwt.mint(result.userId().toString(), result.roles(), ttlSeconds);

        x.json(200, Map.of("token_type", "Bearer", "access_token", token, "expires_in", ttlSeconds));
        return true;
    }

    private static String text(final JsonNode node, final String field) {
        final var v = node.get(field);
        return v != null && v.isTextual() ? v.asText() : null;
    }

    private record BasicCreds(String user, String pass) {
    }

    private static BasicCreds decodeBasic(final String base64Part) {
        try {
            final var decoded = new String(Base64.getDecoder().decode(base64Part), StandardCharsets.UTF_8);
            final var idx = decoded.indexOf(':');
            if (idx <= 0) {
                return null;
            }
            final var user = decoded.substring(0, idx);
            final var pass = decoded.substring(idx + 1);
            return new BasicCreds(user, pass);
        } catch (final Exception e) {
            return null;
        }
    }
}
