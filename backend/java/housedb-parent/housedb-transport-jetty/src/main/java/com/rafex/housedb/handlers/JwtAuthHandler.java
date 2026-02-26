package com.rafex.housedb.handlers;

import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.security.JwtService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class JwtAuthHandler extends Handler.Wrapper {

    public static final String REQ_ATTR_AUTH = "auth";

    private record Rule(String method, PathSpec pathSpec) {
    }

    private final JwtService jwt;
    private final List<Rule> publicRules = new ArrayList<>();
    private final List<PathSpec> protectedPrefixes = new ArrayList<>();

    public JwtAuthHandler(final Handler delegate, final JwtService jwt) {
        super(delegate);
        this.jwt = Objects.requireNonNull(jwt);
    }

    public JwtAuthHandler publicPath(final String method, final String pathSpec) {
        publicRules.add(new Rule(method.toUpperCase(), PathSpec.from(pathSpec)));
        return this;
    }

    public JwtAuthHandler protectedPrefix(final String pathSpec) {
        protectedPrefixes.add(PathSpec.from(pathSpec));
        return this;
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {
        final var method = request.getMethod().toUpperCase();
        final var path = request.getHttpURI() != null ? request.getHttpURI().getPath() : null;

        if (path == null) {
            HttpUtil.json(response, callback, HttpStatus.BAD_REQUEST_400,
                    Map.of("error", "bad_request", "message", "missing_path"));
            return true;
        }

        if (isPublic(method, path) || !isProtected(path)) {
            return super.handle(request, response, callback);
        }

        final var authz = request.getHeaders().get("authorization");
        if (authz == null || !authz.startsWith("Bearer ")) {
            HttpUtil.json(response, callback, HttpStatus.UNAUTHORIZED_401,
                    Map.of("error", "unauthorized", "code", "missing_bearer_token"));
            return true;
        }

        final var token = authz.substring("Bearer ".length()).trim();
        final var result = jwt.verify(token, Instant.now().getEpochSecond());
        if (!result.ok()) {
            HttpUtil.json(response, callback, HttpStatus.UNAUTHORIZED_401,
                    Map.of("error", "unauthorized", "code", result.code()));
            return true;
        }

        request.setAttribute(REQ_ATTR_AUTH, result.ctx());
        return super.handle(request, response, callback);
    }

    private boolean isPublic(final String method, final String path) {
        for (final var rule : publicRules) {
            if (!rule.method().equals(method)) {
                continue;
            }
            if (rule.pathSpec().matches(path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProtected(final String path) {
        for (final var prefix : protectedPrefixes) {
            if (prefix.matches(path)) {
                return true;
            }
        }
        return false;
    }
}
