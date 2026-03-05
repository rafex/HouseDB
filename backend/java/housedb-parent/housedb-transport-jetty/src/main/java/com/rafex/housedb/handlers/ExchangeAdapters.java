package com.rafex.housedb.handlers;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.jetty12.JettyHttpExchange;

public final class ExchangeAdapters {

    private ExchangeAdapters() {
    }

    public static JettyHttpExchange jetty(final HttpExchange exchange) {
        if (exchange instanceof final JettyHttpExchange jx) {
            return jx;
        }
        throw new IllegalStateException("Jetty exchange required");
    }

    public static Request request(final HttpExchange exchange) {
        return jetty(exchange).request();
    }

    public static String rawQuery(final HttpExchange exchange) {
        final var request = request(exchange);
        return request.getHttpURI() == null ? null : request.getHttpURI().getQuery();
    }
}
