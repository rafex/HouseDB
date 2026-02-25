package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.http.HttpUtil;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class HouseEndpointSupport {

    private HouseEndpointSupport() {
    }

    interface EndpointAction {
        void run() throws Exception;
    }

    static boolean execute(final Logger logger, final Response response, final Callback callback,
            final EndpointAction action) {
        try {
            action.run();
            return true;
        } catch (final IllegalArgumentException e) {
            HttpUtil.badRequest(response, callback, e.getMessage());
            return true;
        } catch (final SQLException e) {
            logger.log(Level.SEVERE, "SQL error handling request", e);
            HttpUtil.internalServerError(response, callback, "database error");
            return true;
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "Unhandled error", e);
            HttpUtil.internalServerError(response, callback, "internal error");
            return true;
        }
    }
}
