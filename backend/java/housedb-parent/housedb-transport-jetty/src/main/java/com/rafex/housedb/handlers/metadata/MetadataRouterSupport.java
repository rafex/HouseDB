package com.rafex.housedb.handlers.metadata;

import com.rafex.housedb.handlers.support.EtherJettyErrors;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.HttpError;

final class MetadataRouterSupport {

    private MetadataRouterSupport() {
    }

    static boolean execute(final Logger log, final HttpExchange x, final AutoCloseable action) {
        try {
            action.close();
            return true;
        } catch (final SecurityException e) {
            EtherJettyErrors.error(x, new HttpError(403, "forbidden", e.getMessage()));
            return true;
        } catch (final IllegalArgumentException e) {
            EtherJettyErrors.error(x, new HttpError(400, "bad_request", e.getMessage()));
            return true;
        } catch (final SQLException e) {
            log.log(Level.SEVERE, "SQL error handling metadata request", e);
            EtherJettyErrors.internalServerError(x, "database error");
            return true;
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Unhandled metadata route error", e);
            EtherJettyErrors.internalServerError(x, "internal error");
            return true;
        }
    }
}
