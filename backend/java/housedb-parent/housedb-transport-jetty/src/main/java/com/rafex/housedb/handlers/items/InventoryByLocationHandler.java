package com.rafex.housedb.handlers.items;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.services.ItemFinderService;

import java.util.Map;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class InventoryByLocationHandler {

    private static final Logger LOG = Logger.getLogger(InventoryByLocationHandler.class.getName());

    private final ItemFinderService service;

    InventoryByLocationHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x) {
        return EndpointSupport.execute(LOG, x, () -> {
            final var query = ItemRequestParsers.parseQuery(ExchangeAdapters.rawQuery(x));
            final var userId = AuthzSupport.requireTokenUser(x);
            final var houseId = ItemRequestParsers.parseOptionalUuid(query, "houseId");
            final var houseLocationId = ItemRequestParsers.parseOptionalUuid(query, "houseLocationId");
            final Boolean includeDescendants = ItemRequestParsers.parseOptionalBoolean(query, "includeDescendants");
            final Integer limit = ItemRequestParsers.parseOptionalInt(query, "limit");
            final Integer offset = ItemRequestParsers.parseOptionalInt(query, "offset");

            final var items = service.listInventoryByLocation(userId, houseId, houseLocationId, includeDescendants,
                    limit, offset);
            x.json(200, Map.of("items", items, "count", items.size()));
        });
    }
}
