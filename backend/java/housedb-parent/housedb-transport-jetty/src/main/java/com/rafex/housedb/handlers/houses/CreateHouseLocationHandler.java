package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.dtos.CreateHouseLocationRequest;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.kiwi.KiwiApiClient;
import com.rafex.housedb.services.ItemFinderService;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class CreateHouseLocationHandler {

    private static final Logger LOG = Logger.getLogger(CreateHouseLocationHandler.class.getName());

    private final KiwiApiClient kiwiApiClient;
    private final ItemFinderService itemService;

    CreateHouseLocationHandler(final KiwiApiClient kiwiApiClient, final ItemFinderService itemService) {
        this.kiwiApiClient = kiwiApiClient;
        this.itemService = itemService;
    }

    boolean handle(final Request request, final Response response, final Callback callback, final UUID houseId) {
        return HouseEndpointSupport.execute(LOG, response, callback, () -> {
            final var body = JsonUtil.MAPPER.readValue(Request.asInputStream(request), CreateHouseLocationRequest.class);
            final UUID kiwiParentLocationId;
            final UUID parentHouseLocationId = body.parentHouseLocationId();
            if (parentHouseLocationId != null) {
                kiwiParentLocationId = itemService.findKiwiLocationIdByHouseLocationId(parentHouseLocationId);
                if (kiwiParentLocationId == null) {
                    throw new IllegalArgumentException("parentHouseLocationId is not synchronized with kiwi location");
                }
            } else {
                kiwiParentLocationId = itemService.findRootKiwiLocationIdByHouseId(houseId);
            }

            final UUID kiwiLocationId = kiwiApiClient.createLocation(body.name(), kiwiParentLocationId);

            final UUID houseLocationId = itemService.upsertHouseLocationFromKiwi(
                    houseId,
                    kiwiLocationId,
                    kiwiParentLocationId,
                    parentHouseLocationId,
                    body.locationKind(),
                    body.name(),
                    body.isLeaf(),
                    body.path(),
                    body.referenceCode(),
                    body.notes(),
                    body.latitude(),
                    body.longitude(),
                    body.enabled());

            HttpUtil.ok(response, callback, Map.of(
                    "houseLocationId", houseLocationId));
        });
    }
}
