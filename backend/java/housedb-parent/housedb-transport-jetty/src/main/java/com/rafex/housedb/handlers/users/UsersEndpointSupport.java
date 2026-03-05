package com.rafex.housedb.handlers.users;

import com.rafex.housedb.http.HttpUtil;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class UsersEndpointSupport {

    private UsersEndpointSupport() {
    }

    static boolean execute(final Logger logger, final HttpExchange exchange,
            final AutoCloseable action) {
        try {
            action.close();
            return true;
        } catch (final SecurityException e) {
            HttpUtil.forbidden(exchange, e.getMessage());
            return true;
        } catch (final IllegalArgumentException e) {
            HttpUtil.badRequest(exchange, e.getMessage());
            return true;
        } catch (final SQLException e) {
            logger.log(Level.SEVERE, "SQL error handling request", e);
            HttpUtil.internalServerError(exchange, "database error");
            return true;
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Unhandled error", e);
            HttpUtil.internalServerError(exchange, "internal error");
            return true;
        }
    }
}
