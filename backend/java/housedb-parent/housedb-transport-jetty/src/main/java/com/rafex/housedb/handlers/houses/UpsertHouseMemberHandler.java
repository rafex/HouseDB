package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.dtos.UpsertHouseMemberRequest;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.services.HouseService;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class UpsertHouseMemberHandler {

    private static final Logger LOG = Logger.getLogger(UpsertHouseMemberHandler.class.getName());

    private final HouseService service;

    UpsertHouseMemberHandler(final HouseService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback, final UUID houseId) {
        return HouseEndpointSupport.execute(LOG, response, callback, () -> {
            final var body = JsonUtil.MAPPER.readValue(Request.asInputStream(request), UpsertHouseMemberRequest.class);
            final var result = service.upsertHouseMember(houseId, body.userId(), body.role(), body.enabled());
            HttpUtil.ok(response, callback, result);
        });
    }
}
