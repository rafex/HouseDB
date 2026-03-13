package com.rafex.housedb.handlers.users;

import com.rafex.housedb.handlers.support.EtherJettyErrors;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.HttpError;

final class UsersEndpointSupport {

    private UsersEndpointSupport() {
    }

    static boolean execute(final Logger logger, final HttpExchange exchange,
            final AutoCloseable action) {
        try {
            action.close();
            return true;
        } catch (final SecurityException e) {
            EtherJettyErrors.error(exchange, new HttpError(403, "forbidden", e.getMessage()));
            return true;
        } catch (final IllegalArgumentException e) {
            EtherJettyErrors.error(exchange, new HttpError(400, "bad_request", e.getMessage()));
            return true;
        } catch (final SQLException e) {
            logger.log(Level.SEVERE, "SQL error handling request", e);
            EtherJettyErrors.internalServerError(exchange, "database error");
            return true;
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Unhandled error", e);
            EtherJettyErrors.internalServerError(exchange, "internal error");
            return true;
        }
    }
}
