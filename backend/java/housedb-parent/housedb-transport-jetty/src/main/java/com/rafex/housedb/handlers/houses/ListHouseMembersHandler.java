package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.HouseService;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class ListHouseMembersHandler {

    private static final Logger LOG = Logger.getLogger(ListHouseMembersHandler.class.getName());

    private final HouseService service;

    ListHouseMembersHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback, final UUID houseId) {
        return HouseEndpointSupport.execute(LOG, response, callback, () -> {
            final var query = HouseRequestParsers.parseQuery(request.getHttpURI().getQuery());
            final var includeDisabled = HouseRequestParsers.parseOptionalBoolean(query, "includeDisabled");
            final var limit = HouseRequestParsers.parseOptionalInt(query, "limit");

            final var members = service.listHouseMembers(houseId, includeDisabled, limit);
            HttpUtil.ok(response, callback, Map.of("members", members, "count", members.size()));
        });
    }
}
