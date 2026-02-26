package com.rafex.housedb.handlers.items;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.ItemFinderService;

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class InventoryByLocationHandler {

    private static final Logger LOG = Logger.getLogger(InventoryByLocationHandler.class.getName());

    private final ItemFinderService service;

    InventoryByLocationHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback) {
        return EndpointSupport.execute(LOG, response, callback, () -> {
            final var query = ItemRequestParsers.parseQuery(request.getHttpURI().getQuery());
            final var userId = AuthzSupport.requireTokenUser(request);
            final var houseId = ItemRequestParsers.parseOptionalUuid(query, "houseId");
            final var houseLocationId = ItemRequestParsers.parseOptionalUuid(query, "houseLocationId");
            final Boolean includeDescendants = ItemRequestParsers.parseOptionalBoolean(query, "includeDescendants");
            final Integer limit = ItemRequestParsers.parseOptionalInt(query, "limit");

            final var items = service.listInventoryByLocation(userId, houseId, houseLocationId, includeDescendants,
                    limit);
            HttpUtil.ok(response, callback, Map.of("items", items, "count", items.size()));
        });
    }
}
