package com.rafex.housedb.handlers.items;

import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.ItemFinderService;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class InventoryTimelineHandler {

    private static final Logger LOG = Logger.getLogger(InventoryTimelineHandler.class.getName());

    private final ItemFinderService service;

    InventoryTimelineHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x, final UUID inventoryItemId) {
        return EndpointSupport.execute(LOG, x, () -> {
            final var query = ItemRequestParsers.parseQuery(ExchangeAdapters.rawQuery(x));
            final Integer limit = ItemRequestParsers.parseOptionalInt(query, "limit");
            final Integer offset = ItemRequestParsers.parseOptionalInt(query, "offset");

            final var events = service.inventoryItemTimeline(inventoryItemId, limit, offset);
            HttpUtil.ok(x, Map.of("events", events, "count", events.size()));
        });
    }
}
