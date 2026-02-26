package com.rafex.housedb.handlers.items;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.ItemFinderService;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class InventoryNearbyHandler {

    private static final Logger LOG = Logger.getLogger(InventoryNearbyHandler.class.getName());

    private final ItemFinderService service;

    InventoryNearbyHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback) {
        return EndpointSupport.execute(LOG, response, callback, () -> {
            final var query = ItemRequestParsers.parseQuery(request.getHttpURI().getQuery());
            final UUID userId = ItemRequestParsers.parseRequiredUuid(query, "userId");
            final double latitude = ItemRequestParsers.parseRequiredDouble(query, "latitude");
            final double longitude = ItemRequestParsers.parseRequiredDouble(query, "longitude");
            final Double radiusMeters = ItemRequestParsers.parseOptionalDouble(query, "radiusMeters");
            final Integer limit = ItemRequestParsers.parseOptionalInt(query, "limit");

            AuthzSupport.requireAuthorizedUser(request, userId);
            final var items = service.searchInventoryItemsNearPoint(userId, latitude, longitude, radiusMeters, limit);
            HttpUtil.ok(response, callback, Map.of("items", items, "count", items.size()));
        });
    }
}
