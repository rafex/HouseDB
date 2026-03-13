package com.rafex.housedb.handlers.support;

import com.rafex.housedb.handlers.ExchangeAdapters;

import dev.rafex.ether.http.core.HttpError;
import dev.rafex.ether.http.jetty12.JettyApiErrorResponses;
import dev.rafex.ether.json.JsonCodecBuilder;

public final class EtherJettyErrors {

    private static final JettyApiErrorResponses RESPONSES =
            new JettyApiErrorResponses(JsonCodecBuilder.create().build());

    private EtherJettyErrors() {
    }

    public static void error(final dev.rafex.ether.http.core.HttpExchange exchange, final HttpError error) {
        final var jettyExchange = ExchangeAdapters.jetty(exchange);
        RESPONSES.error(jettyExchange.response(), jettyExchange.callback(), error, exchange.path());
    }

    public static void badRequest(final dev.rafex.ether.http.core.HttpExchange exchange, final String message) {
        final var jettyExchange = ExchangeAdapters.jetty(exchange);
        RESPONSES.badRequest(jettyExchange.response(), jettyExchange.callback(), message);
    }

    public static void unauthorized(final dev.rafex.ether.http.core.HttpExchange exchange, final String code) {
        final var jettyExchange = ExchangeAdapters.jetty(exchange);
        RESPONSES.unauthorized(jettyExchange.response(), jettyExchange.callback(), code);
    }

    public static void forbidden(final dev.rafex.ether.http.core.HttpExchange exchange, final String code) {
        final var jettyExchange = ExchangeAdapters.jetty(exchange);
        RESPONSES.forbidden(jettyExchange.response(), jettyExchange.callback(), code);
    }

    public static void notFound(final dev.rafex.ether.http.core.HttpExchange exchange) {
        final var jettyExchange = ExchangeAdapters.jetty(exchange);
        RESPONSES.notFound(jettyExchange.response(), jettyExchange.callback(), exchange.path());
    }

    public static void internalServerError(final dev.rafex.ether.http.core.HttpExchange exchange, final String message) {
        final var jettyExchange = ExchangeAdapters.jetty(exchange);
        RESPONSES.internalServerError(jettyExchange.response(), jettyExchange.callback(), message);
    }
}
