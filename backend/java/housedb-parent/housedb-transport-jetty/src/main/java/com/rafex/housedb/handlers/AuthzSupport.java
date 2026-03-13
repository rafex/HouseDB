package com.rafex.housedb.handlers;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.jetty12.JettyAuthHandler;
import dev.rafex.ether.jwt.TokenClaims;
import dev.rafex.ether.jwt.TokenType;

public final class AuthzSupport {

    private AuthzSupport() {
    }

    public static UUID requireAuthorizedUser(final Request request, final UUID requestedUserId) {
        Objects.requireNonNull(request, "request");
        if (requestedUserId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        final var auth = requireAuthContext(request);
        if (isAppToken(auth) || isAdmin(auth)) {
            return requestedUserId;
        }

        final UUID tokenUserId;
        try {
            tokenUserId = UUID.fromString(auth.subject());
        } catch (final IllegalArgumentException e) {
            throw new SecurityException("token subject is not a valid user id");
        }

        if (!tokenUserId.equals(requestedUserId)) {
            throw new SecurityException("forbidden: token user does not match requested userId");
        }

        return requestedUserId;
    }

    public static UUID requireAuthorizedUser(final HttpExchange exchange, final UUID requestedUserId) {
        return requireAuthorizedUser(ExchangeAdapters.request(exchange), requestedUserId);
    }

    public static UUID resolveRequestedOrTokenUser(final Request request, final UUID requestedUserId) {
        Objects.requireNonNull(request, "request");
        if (requestedUserId != null) {
            return requireAuthorizedUser(request, requestedUserId);
        }

        final var auth = requireAuthContext(request);
        if (isAppToken(auth)) {
            throw new IllegalArgumentException("userId is required for app token");
        }

        try {
            return UUID.fromString(auth.subject());
        } catch (final IllegalArgumentException e) {
            throw new SecurityException("token subject is not a valid user id");
        }
    }

    public static UUID resolveRequestedOrTokenUser(final HttpExchange exchange, final UUID requestedUserId) {
        return resolveRequestedOrTokenUser(ExchangeAdapters.request(exchange), requestedUserId);
    }

    public static UUID requireTokenUser(final Request request) {
        Objects.requireNonNull(request, "request");
        final var auth = requireAuthContext(request);
        if (isAppToken(auth)) {
            throw new SecurityException("forbidden: user token required");
        }
        try {
            return UUID.fromString(auth.subject());
        } catch (final IllegalArgumentException e) {
            throw new SecurityException("token subject is not a valid user id");
        }
    }

    public static UUID requireTokenUser(final HttpExchange exchange) {
        return requireTokenUser(ExchangeAdapters.request(exchange));
    }

    public static void requireAppOrAdmin(final Request request) {
        Objects.requireNonNull(request, "request");
        final var auth = requireAuthContext(request);
        if (isAppToken(auth) || isAdmin(auth)) {
            return;
        }
        throw new SecurityException("forbidden: admin or app token required");
    }

    public static void requireAppOrAdmin(final HttpExchange exchange) {
        requireAppOrAdmin(ExchangeAdapters.request(exchange));
    }

    private static TokenClaims requireAuthContext(final Request request) {
        final var auth = request.getAttribute(JettyAuthHandler.REQ_ATTR_AUTH);
        if (auth instanceof TokenClaims ctx) {
            return ctx;
        }
        throw new SecurityException("missing authentication context");
    }

    private static boolean isAppToken(final TokenClaims auth) {
        return auth.tokenType() == TokenType.APP;
    }

    private static boolean isAdmin(final TokenClaims auth) {
        return auth.roles().stream()
                .filter(Objects::nonNull)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .anyMatch("ADMIN"::equals);
    }
}
