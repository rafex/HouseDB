package com.rafex.housedb.handlers.items;

import com.rafex.housedb.dtos.CreateInventoryItemRequest;
import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.kiwi.KiwiApiClient;
import com.rafex.housedb.services.ItemFinderService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.json.JsonUtils;

final class InventoryCreateHandler {

    private static final Logger LOG = Logger.getLogger(InventoryCreateHandler.class.getName());

    private final ItemFinderService service;
    private final KiwiApiClient kiwiApiClient;

    InventoryCreateHandler(final ItemFinderService service, final KiwiApiClient kiwiApiClient) {
        this.service = service;
        this.kiwiApiClient = kiwiApiClient;
    }

    boolean handle(final HttpExchange x) {
        return EndpointSupport.execute(LOG, x, () -> {
            final Request request = ExchangeAdapters.request(x);
            final var body = JsonUtils.fromJson(Request.asInputStream(request), CreateInventoryItemRequest.class);
            final var userId = AuthzSupport.requireTokenUser(x);
            final var kiwiLocationId = service.findKiwiLocationIdByHouseLocationId(body.houseLocationLeafId());
            if (kiwiLocationId == null) {
                throw new IllegalArgumentException("houseLocationLeafId is not synchronized with kiwi location");
            }

            final var tags = new ArrayList<String>();
            if (body.objectTags() != null) {
                for (final var t : body.objectTags()) {
                    if (t != null && !t.isBlank()) {
                        tags.add(t.trim());
                    }
                }
            }
            if (body.objectCategory() != null && !body.objectCategory().isBlank()) {
                tags.add(body.objectCategory().trim());
            }

            final var kiwiObjectId = kiwiApiClient.createObject(body.objectName(), body.objectDescription(),
                    kiwiLocationId, body.objectType(), tags, body.kiwiMetadata());
            final var objectId = service.upsertObjectFromKiwi(kiwiObjectId, body.objectName(), body.objectDescription(),
                    body.objectCategory(), null, true);
            final var housedbMetadataJson = body.housedbMetadata() == null
                    ? "{}"
                    : JsonUtils.toJson(body.housedbMetadata());

            final var result = service.createInventoryItem(userId, objectId, body.nickname(),
                    body.serialNumber(), body.conditionStatus(), housedbMetadataJson, body.houseLocationLeafId(), body.movedBy(),
                    body.notes());
            final var payload = new LinkedHashMap<String, Object>();
            payload.put("inventoryItemId", result.inventoryItemId());
            payload.put("itemMovementId", result.itemMovementId());
            payload.put("objectId", objectId);
            x.json(200, payload);
        });
    }
}
