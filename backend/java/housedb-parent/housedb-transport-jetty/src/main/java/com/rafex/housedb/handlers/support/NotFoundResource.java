package com.rafex.housedb.handlers.support;

import java.util.List;
import java.util.Set;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

public final class NotFoundResource extends NonBlockingResourceHandler {

    private static final Set<String> ALL_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    public NotFoundResource(final JsonCodec jsonCodec) {
        super(jsonCodec, new HouseDbErrorMapper());
    }

    @Override
    protected String basePath() {
        return "/";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/**", ALL_METHODS));
    }

    @Override
    public boolean get(final HttpExchange x) {
        EtherJettyErrors.notFound(x);
        return true;
    }

    @Override
    public boolean post(final HttpExchange x) {
        EtherJettyErrors.notFound(x);
        return true;
    }

    @Override
    public boolean put(final HttpExchange x) {
        EtherJettyErrors.notFound(x);
        return true;
    }

    @Override
    public boolean patch(final HttpExchange x) {
        EtherJettyErrors.notFound(x);
        return true;
    }

    @Override
    public boolean delete(final HttpExchange x) {
        EtherJettyErrors.notFound(x);
        return true;
    }

    @Override
    public boolean options(final HttpExchange x) {
        EtherJettyErrors.notFound(x);
        return true;
    }
}
