package com.rafex.housedb.handlers;

import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.security.JwtService;
import com.rafex.housedb.services.AuthService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.fasterxml.jackson.databind.JsonNode;

public final class LoginHandler extends Handler.Abstract.NonBlocking {

    private final JwtService jwt;
    private final AuthService authService;
    private final long ttlSeconds;

    public LoginHandler(final JwtService jwt, final AuthService authService) {
        this(jwt, authService, Long.parseLong(System.getenv().getOrDefault("JWT_TTL_SECONDS", "3600")));
    }

    public LoginHandler(final JwtService jwt, final AuthService authService, final long ttlSeconds) {
        this.jwt = Objects.requireNonNull(jwt);
        this.authService = Objects.requireNonNull(authService);
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            HttpUtil.json(response, callback, HttpStatus.METHOD_NOT_ALLOWED_405, Map.of("error", "method_not_allowed"));
            return true;
        }

        final var authz = request.getHeaders().get("authorization");
        if (authz != null && authz.regionMatches(true, 0, "Basic ", 0, "Basic ".length())) {
            final var creds = decodeBasic(authz.substring("Basic ".length()).trim());
            if (creds == null) {
                HttpUtil.unauthorized(response, callback, "bad_basic_auth");
                return true;
            }
            return authenticateAndMint(response, callback, creds.user(), creds.pass());
        }

        final String body;
        try {
            body = Content.Source.asString(request, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            HttpUtil.badRequest(response, callback, "cannot_read_body");
            return true;
        }

        if (body == null || body.isBlank()) {
            HttpUtil.unauthorized(response, callback, "missing_credentials");
            return true;
        }

        final JsonNode json;
        try {
            json = JsonUtil.MAPPER.readTree(body);
        } catch (final Exception e) {
            HttpUtil.badRequest(response, callback, "invalid_json");
            return true;
        }

        final var user = text(json, "username");
        final var pass = text(json, "password");

        if (user == null || pass == null) {
            HttpUtil.unauthorized(response, callback, "missing_credentials");
            return true;
        }

        return authenticateAndMint(response, callback, user, pass);
    }

    private boolean authenticateAndMint(final Response response, final Callback callback, final String username,
            final String password) throws Exception {

        final var result = authService.authenticate(username, password.toCharArray());

        if (!result.ok()) {
            final var code = result.code() != null ? result.code() : "bad_credentials";

            if ("user_disabled".equals(code)) {
                HttpUtil.json(response, callback, HttpStatus.FORBIDDEN_403,
                        Map.of("error", "forbidden", "code", "user_disabled"));
            } else if ("bad_credentials".equals(code)) {
                HttpUtil.unauthorized(response, callback, "bad_credentials");
            } else {
                HttpUtil.json(response, callback, HttpStatus.UNAUTHORIZED_401,
                        Map.of("error", "unauthorized", "code", code));
            }
            return true;
        }

        final var token = jwt.mint(result.userId().toString(), result.roles(), ttlSeconds);

        HttpUtil.ok(response, callback, Map.of("token_type", "Bearer", "access_token", token, "expires_in", ttlSeconds));
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
