package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.dtos.UpsertHouseMemberRequest;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.services.HouseService;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.json.JsonCodec;

final class UpsertHouseMemberHandler {

    private static final Logger LOG = Logger.getLogger(UpsertHouseMemberHandler.class.getName());

    private final JsonCodec jsonCodec;
    private final HouseService service;

    UpsertHouseMemberHandler(final JsonCodec jsonCodec, final HouseService service) {
        this.jsonCodec = jsonCodec;
        this.service = service;
    }

    boolean handle(final HttpExchange x, final UUID houseId) {
        return HouseEndpointSupport.execute(LOG, x, () -> {
            final Request request = ExchangeAdapters.request(x);
            final var body = jsonCodec.readValue(Request.asInputStream(request), UpsertHouseMemberRequest.class);
            final var result = service.upsertHouseMember(houseId, body.userId(), body.role(), body.enabled());
            x.json(200, result);
        });
    }
}
