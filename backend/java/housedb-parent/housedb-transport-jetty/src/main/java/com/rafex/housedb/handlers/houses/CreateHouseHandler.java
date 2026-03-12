package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.dtos.CreateHouseRequest;
import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.services.HouseService;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.json.JsonUtils;

final class CreateHouseHandler {

    private static final Logger LOG = Logger.getLogger(CreateHouseHandler.class.getName());

    private final HouseService service;

    CreateHouseHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x) {
        return HouseEndpointSupport.execute(LOG, x, () -> {
            final Request request = ExchangeAdapters.request(x);
            final var body = JsonUtils.fromJson(Request.asInputStream(request), CreateHouseRequest.class);
            final var ownerUserId = AuthzSupport.requireTokenUser(x);
            final var result = service.createHouse(ownerUserId, body.name(), body.description(), body.street(),
                    body.numberExt(), body.numberInt(), body.neighborhood(), body.city(), body.state(),
                    body.zipCode(), body.country(), body.latitude(), body.longitude(), body.urlMap());
            x.json(200, result);
        });
    }
}
