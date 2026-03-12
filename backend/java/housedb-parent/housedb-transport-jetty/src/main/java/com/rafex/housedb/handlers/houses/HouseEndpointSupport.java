package com.rafex.housedb.handlers.houses;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class HouseEndpointSupport {

    private HouseEndpointSupport() {
    }

    static boolean execute(final Logger logger, final HttpExchange exchange,
            final AutoCloseable action) {
        try {
            action.close();
            return true;
        } catch (final SecurityException e) {
            exchange.json(403, Map.of("error", "forbidden", "code", e.getMessage(), "timestamp", Instant.now().toString()));
            return true;
        } catch (final IllegalArgumentException e) {
            exchange.json(400,
                    Map.of("error", "bad_request", "message", e.getMessage(), "timestamp", Instant.now().toString()));
            return true;
        } catch (final SQLException e) {
            logger.log(Level.SEVERE, "SQL error handling request", e);
            exchange.json(500, Map.of("error", "internal_server_error", "message", "database error",
                    "timestamp", Instant.now().toString()));
            return true;
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Unhandled error", e);
            exchange.json(500, Map.of("error", "internal_server_error", "message", "internal error",
                    "timestamp", Instant.now().toString()));
            return true;
        }
    }
}
