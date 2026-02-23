package com.rafex.housedb.handlers.items;

import com.rafex.housedb.dtos.UpsertKiwiLocationRequest;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.services.ItemFinderService;

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class KiwiLocationSyncHandler {

    private static final Logger LOG = Logger.getLogger(KiwiLocationSyncHandler.class.getName());

    private final ItemFinderService service;

    KiwiLocationSyncHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback) {
        return EndpointSupport.execute(LOG, response, callback, () -> {
            final var body = JsonUtil.MAPPER.readValue(Request.asInputStream(request), UpsertKiwiLocationRequest.class);
            final var houseLocationId = service.upsertHouseLocationFromKiwi(body.houseId(), body.kiwiLocationId(),
                    body.kiwiParentLocationId(), body.parentHouseLocationId(), body.locationKind(), body.name(),
                    body.isLeaf(), body.path(), body.referenceCode(), body.notes(), body.latitude(),
                    body.longitude(), body.enabled());
            HttpUtil.ok(response, callback, Map.of("houseLocationId", houseLocationId));
        });
    }
}
