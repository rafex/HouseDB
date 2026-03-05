package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.dtos.UpsertHouseMemberRequest;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.services.HouseService;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;

final class UpsertHouseMemberHandler {

    private static final Logger LOG = Logger.getLogger(UpsertHouseMemberHandler.class.getName());

    private final HouseService service;

    UpsertHouseMemberHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x, final UUID houseId) {
        return HouseEndpointSupport.execute(LOG, x, () -> {
            final Request request = ExchangeAdapters.request(x);
            final var body = JsonUtil.MAPPER.readValue(Request.asInputStream(request), UpsertHouseMemberRequest.class);
            final var result = service.upsertHouseMember(houseId, body.userId(), body.role(), body.enabled());
            HttpUtil.ok(x, result);
        });
    }
}
