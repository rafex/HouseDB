package com.rafex.housedb.handlers;

import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.tools.BuildVersion;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

public final class HelloHandler extends Handler.Abstract.NonBlocking {

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) {
        final var path = request.getHttpURI() != null ? request.getHttpURI().getPath() : null;
        if (path == null) {
            HttpUtil.badRequest(response, callback, "missing_path");
            return true;
        }

        if ("/hello".equals(path)) {
            if (!HttpMethod.GET.is(request.getMethod())) {
                response.setStatus(405);
                callback.succeeded();
                return true;
            }
            HttpUtil.ok(response, callback, responseBody(null));
            return true;
        }

        if ("/hello/name".equals(path)) {
            if (!HttpMethod.GET.is(request.getMethod()) && !HttpMethod.POST.is(request.getMethod())) {
                response.setStatus(405);
                callback.succeeded();
                return true;
            }
            final var name = HttpMethod.GET.is(request.getMethod()) ? nameFromQuery(request) : nameFromPost(request);
            HttpUtil.ok(response, callback, responseBody(name));
            return true;
        }

        return false;
    }

    private static String nameFromPost(final Request request) {
        final var queryName = nameFromQuery(request);
        String body = null;
        try {
            body = Content.Source.asString(request, StandardCharsets.UTF_8);
        } catch (final Exception ignored) {
            return queryName;
        }

        if (body == null || body.isBlank()) {
            return queryName;
        }

        try {
            final var json = JsonUtil.MAPPER.readTree(body);
            final var node = json != null ? json.get("name") : null;
            final var bodyName = node != null && node.isTextual() ? normalize(node.asText()) : null;
            return bodyName != null ? bodyName : queryName;
        } catch (final Exception ignored) {
            final var form = parseQuery(body);
            final var formName = normalize(form.getValue("name"));
            return formName != null ? formName : queryName;
        }
    }

    private static String nameFromQuery(final Request request) {
        final var rawQuery = request.getHttpURI() != null ? request.getHttpURI().getQuery() : null;
        final var params = parseQuery(rawQuery);
        return normalize(params.getValue("name"));
    }

    private static MultiMap<String> parseQuery(final String rawQuery) {
        final var params = new MultiMap<String>();
        if (rawQuery != null && !rawQuery.isBlank()) {
            UrlEncoded.decodeTo(rawQuery, params, StandardCharsets.UTF_8);
        }
        return params;
    }

    private static Map<String, String> responseBody(final String name) {
        final var message = name == null ? "Hello!!" : "Hello!! " + name;
        return Map.of("message", message, "version", BuildVersion.current());
    }

    private static String normalize(final String value) {
        if (value == null) {
            return null;
        }
        final var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
