package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.HouseService;

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class ListHousesHandler {

    private static final Logger LOG = Logger.getLogger(ListHousesHandler.class.getName());

    private final HouseService service;

    ListHousesHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback) {
        return HouseEndpointSupport.execute(LOG, response, callback, () -> {
            final var query = HouseRequestParsers.parseQuery(request.getHttpURI().getQuery());
            final var userId = HouseRequestParsers.parseRequiredUuid(query, "userId");
            final var includeDisabled = HouseRequestParsers.parseOptionalBoolean(query, "includeDisabled");
            final var limit = HouseRequestParsers.parseOptionalInt(query, "limit");

            AuthzSupport.requireAuthorizedUser(request, userId);
            final var houses = service.listUserHouses(userId, includeDisabled, limit);
            HttpUtil.ok(response, callback, Map.of("houses", houses, "count", houses.size()));
        });
    }
}
