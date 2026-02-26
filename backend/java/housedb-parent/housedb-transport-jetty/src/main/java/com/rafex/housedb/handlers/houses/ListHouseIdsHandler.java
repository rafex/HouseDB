package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.HouseService;

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class ListHouseIdsHandler {

    private static final Logger LOG = Logger.getLogger(ListHouseIdsHandler.class.getName());

    private final HouseService service;

    ListHouseIdsHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback) {
        return HouseEndpointSupport.execute(LOG, response, callback, () -> {
            final var query = HouseRequestParsers.parseQuery(request.getHttpURI().getQuery());
            final var includeDisabled = HouseRequestParsers.parseOptionalBoolean(query, "includeDisabled");
            final var limit = HouseRequestParsers.parseOptionalInt(query, "limit");

            final var userId = AuthzSupport.requireTokenUser(request);
            final var houses = service.listUserHouses(userId, includeDisabled, limit);
            final var ids = houses.stream().map(h -> h.houseId()).toList();

            HttpUtil.ok(response, callback, Map.of("houseIds", ids, "count", ids.size()));
        });
    }
}
