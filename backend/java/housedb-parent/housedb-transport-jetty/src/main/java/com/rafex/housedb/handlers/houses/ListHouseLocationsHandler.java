package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.services.HouseService;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class ListHouseLocationsHandler {

    private static final Logger LOG = Logger.getLogger(ListHouseLocationsHandler.class.getName());

    private final HouseService service;

    ListHouseLocationsHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x, final UUID houseId) {
        return HouseEndpointSupport.execute(LOG, x, () -> {
            final var query = HouseRequestParsers.parseQuery(ExchangeAdapters.rawQuery(x));
            final var includeDisabled = HouseRequestParsers.parseOptionalBoolean(query, "includeDisabled");
            final var limit = HouseRequestParsers.parseOptionalInt(query, "limit");
            final var offset = HouseRequestParsers.parseOptionalInt(query, "offset");

            final var locations = service.listHouseLocations(houseId, includeDisabled, limit, offset);
            x.json(200, Map.of("locations", locations, "count", locations.size()));
        });
    }
}
