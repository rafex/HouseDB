package com.rafex.housedb.handlers.items;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.handlers.support.PaginationSupport;
import com.rafex.housedb.services.ItemFinderService;

import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class InventoryListHandler {

    private static final Logger LOG = Logger.getLogger(InventoryListHandler.class.getName());

    private final ItemFinderService service;

    InventoryListHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x) {
        return EndpointSupport.execute(LOG, x, () -> {
            final var query = ItemRequestParsers.parseQuery(ExchangeAdapters.rawQuery(x));
            final var userId = AuthzSupport.requireTokenUser(x);
            final Integer limit = ItemRequestParsers.parseOptionalInt(query, "limit");
            final Integer offset = ItemRequestParsers.parseOptionalInt(query, "offset");
            final var page = PaginationSupport.request(limit, offset, 50, 200);
            final var items = service.listOwnedInventoryItems(userId, page.fetchLimit(), page.offset());
            x.json(200, PaginationSupport.response("items", items, page));
        });
    }
}
