package com.rafex.housedb.handlers;

import com.rafex.housedb.tools.BuildVersion;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.json.JsonUtils;

public final class HelloHandler extends NonBlockingResourceHandler {

    public HelloHandler(final JsonCodec jsonCodec) {
        super(jsonCodec);
    }

    @Override
    protected String basePath() {
        return "/hello";
    }

    @Override
    protected List<Route> routes() {
        return List.of(
                Route.of("/", Set.of("GET")),
                Route.of("/name", Set.of("GET", "POST")));
    }

    @Override
    public boolean get(final HttpExchange x) {
        if ("/hello".equals(x.path())) {
            x.json(200, responseBody(null));
            return true;
        }
        if ("/hello/name".equals(x.path())) {
            final var name = normalize(x.queryFirst("name"));
            x.json(200, responseBody(name));
            return true;
        }
        return false;
    }

    @Override
    public boolean post(final HttpExchange x) {
        if (!"/hello/name".equals(x.path())) {
            return false;
        }
        final var queryName = normalize(x.queryFirst("name"));
        final var request = ExchangeAdapters.request(x);
        String body = null;
        try {
            body = Content.Source.asString(request, StandardCharsets.UTF_8);
        } catch (final Exception ignored) {
            x.json(200, responseBody(queryName));
            return true;
        }

        if (body == null || body.isBlank()) {
            x.json(200, responseBody(queryName));
            return true;
        }

        try {
            final var json = JsonUtils.parseTree(body);
            final var node = json != null ? json.get("name") : null;
            final var bodyName = node != null && node.isTextual() ? normalize(node.asText()) : null;
            x.json(200, responseBody(bodyName != null ? bodyName : queryName));
            return true;
        } catch (final Exception ignored) {
            final var form = parseQuery(body);
            final var formName = normalize(form.getValue("name"));
            x.json(200, responseBody(formName != null ? formName : queryName));
            return true;
        }
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
