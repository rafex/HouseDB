package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.dtos.CreateHouseRequest;
import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.services.HouseService;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class CreateHouseHandler {

    private static final Logger LOG = Logger.getLogger(CreateHouseHandler.class.getName());

    private final HouseService service;

    CreateHouseHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback) {
        return HouseEndpointSupport.execute(LOG, response, callback, () -> {
            final var body = JsonUtil.MAPPER.readValue(Request.asInputStream(request), CreateHouseRequest.class);
            AuthzSupport.requireAuthorizedUser(request, body.ownerUserId());
            final var result = service.createHouse(body.ownerUserId(), body.name(), body.description(), body.street(),
                    body.numberExt(), body.numberInt(), body.neighborhood(), body.city(), body.state(),
                    body.zipCode(), body.country(), body.latitude(), body.longitude(), body.urlMap());
            HttpUtil.ok(response, callback, result);
        });
    }
}
