package com.rafex.housedb.handlers.items;

import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.ItemFinderService;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class InventoryTimelineHandler {

    private static final Logger LOG = Logger.getLogger(InventoryTimelineHandler.class.getName());

    private final ItemFinderService service;

    InventoryTimelineHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback, final UUID inventoryItemId) {
        return EndpointSupport.execute(LOG, response, callback, () -> {
            final var query = ItemRequestParsers.parseQuery(request.getHttpURI().getQuery());
            final Integer limit = ItemRequestParsers.parseOptionalInt(query, "limit");

            final var events = service.inventoryItemTimeline(inventoryItemId, limit);
            HttpUtil.ok(response, callback, Map.of("events", events, "count", events.size()));
        });
    }
}
