package com.rafex.housedb.handlers;

import com.rafex.housedb.http.HttpUtil;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

public final class HealthHandler extends NonBlockingResourceHandler {

    public HealthHandler(final JsonCodec jsonCodec) {
        super(jsonCodec);
    }

    @Override
    protected String basePath() {
        return "/health";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/", Set.of("GET")));
    }

    @Override
    public boolean get(final HttpExchange x) {
        HttpUtil.ok(x, Map.of("status", "UP", "timestamp", Instant.now().toString()));
        return true;
    }
}
