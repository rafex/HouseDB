package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.handlers.support.PaginationSupport;
import com.rafex.housedb.services.HouseService;

import java.util.UUID;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class ListHouseMembersHandler {

    private static final Logger LOG = Logger.getLogger(ListHouseMembersHandler.class.getName());

    private final HouseService service;

    ListHouseMembersHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x, final UUID houseId) {
        return HouseEndpointSupport.execute(LOG, x, () -> {
            final var query = HouseRequestParsers.parseQuery(ExchangeAdapters.rawQuery(x));
            final var includeDisabled = HouseRequestParsers.parseOptionalBoolean(query, "includeDisabled");
            final var limit = HouseRequestParsers.parseOptionalInt(query, "limit");
            final var offset = HouseRequestParsers.parseOptionalInt(query, "offset");
            final var page = PaginationSupport.request(limit, offset, 50, 200);

            final var members = service.listHouseMembers(houseId, includeDisabled, page.fetchLimit(), page.offset());
            x.json(200, PaginationSupport.response("members", members, page));
        });
    }
}
