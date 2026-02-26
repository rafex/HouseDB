package com.rafex.housedb.handlers;

import com.rafex.housedb.security.JwtService;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jetty.server.Request;

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
            tokenUserId = UUID.fromString(auth.sub());
        } catch (final IllegalArgumentException e) {
            throw new SecurityException("token subject is not a valid user id");
        }

        if (!tokenUserId.equals(requestedUserId)) {
            throw new SecurityException("forbidden: token user does not match requested userId");
        }

        return requestedUserId;
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
            return UUID.fromString(auth.sub());
        } catch (final IllegalArgumentException e) {
            throw new SecurityException("token subject is not a valid user id");
        }
    }

    public static UUID requireTokenUser(final Request request) {
        Objects.requireNonNull(request, "request");
        final var auth = requireAuthContext(request);
        if (isAppToken(auth)) {
            throw new SecurityException("forbidden: user token required");
        }
        try {
            return UUID.fromString(auth.sub());
        } catch (final IllegalArgumentException e) {
            throw new SecurityException("token subject is not a valid user id");
        }
    }

    public static void requireAppOrAdmin(final Request request) {
        Objects.requireNonNull(request, "request");
        final var auth = requireAuthContext(request);
        if (isAppToken(auth) || isAdmin(auth)) {
            return;
        }
        throw new SecurityException("forbidden: admin or app token required");
    }

    private static JwtService.AuthContext requireAuthContext(final Request request) {
        final var auth = request.getAttribute(JwtAuthHandler.REQ_ATTR_AUTH);
        if (auth instanceof JwtService.AuthContext ctx) {
            return ctx;
        }
        throw new SecurityException("missing authentication context");
    }

    private static boolean isAppToken(final JwtService.AuthContext auth) {
        return "app".equalsIgnoreCase(auth.tokenType());
    }

    private static boolean isAdmin(final JwtService.AuthContext auth) {
        return auth.roles().stream()
                .filter(Objects::nonNull)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .anyMatch("ADMIN"::equals);
    }
}
