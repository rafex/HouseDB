package com.rafex.housedb.handlers.items;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.handlers.support.PaginationSupport;
import com.rafex.housedb.services.ItemFinderService;

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
            final var page = PaginationSupport.request(limit, offset, 50, 200);

            final var items = service.searchInventoryItems(userId, text, houseId, houseLocationLeafId,
                    page.fetchLimit(), page.offset());
            x.json(200, PaginationSupport.response("items", items, page));
        });
    }
}
