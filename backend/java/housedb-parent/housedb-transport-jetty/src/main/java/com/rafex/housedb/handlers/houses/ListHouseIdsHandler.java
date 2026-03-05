package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.HouseService;

import java.util.Map;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class ListHouseIdsHandler {

    private static final Logger LOG = Logger.getLogger(ListHouseIdsHandler.class.getName());

    private final HouseService service;

    ListHouseIdsHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x) {
        return HouseEndpointSupport.execute(LOG, x, () -> {
            final var query = HouseRequestParsers.parseQuery(ExchangeAdapters.rawQuery(x));
            final var includeDisabled = HouseRequestParsers.parseOptionalBoolean(query, "includeDisabled");
            final var limit = HouseRequestParsers.parseOptionalInt(query, "limit");
            final var offset = HouseRequestParsers.parseOptionalInt(query, "offset");

            final var userId = AuthzSupport.requireTokenUser(x);
            final var houses = service.listUserHouses(userId, includeDisabled, limit, offset);
            final var ids = houses.stream().map(h -> h.houseId()).toList();

            HttpUtil.ok(x, Map.of("houseIds", ids, "count", ids.size()));
        });
    }
}
