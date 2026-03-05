package com.rafex.housedb.handlers.items;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.ItemFinderService;

import java.util.Map;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class InventorySearchHandler {

    private static final Logger LOG = Logger.getLogger(InventorySearchHandler.class.getName());

    private final ItemFinderService service;

    InventorySearchHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x) {
        return EndpointSupport.execute(LOG, x, () -> {
            final var query = ItemRequestParsers.parseQuery(ExchangeAdapters.rawQuery(x));
            final var userId = AuthzSupport.requireTokenUser(x);
            final String text = ItemRequestParsers.getValue(query, "q");
            final var houseId = ItemRequestParsers.parseOptionalUuid(query, "houseId");
            final var houseLocationLeafId = ItemRequestParsers.parseOptionalUuid(query, "houseLocationLeafId");
            final Integer limit = ItemRequestParsers.parseOptionalInt(query, "limit");
            final Integer offset = ItemRequestParsers.parseOptionalInt(query, "offset");

            final var items = service.searchInventoryItems(userId, text, houseId, houseLocationLeafId, limit, offset);
            HttpUtil.ok(x, Map.of("items", items, "count", items.size()));
        });
    }
}
