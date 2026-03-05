package com.rafex.housedb.handlers.items;

import com.rafex.housedb.dtos.MoveInventoryItemRequest;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.services.ItemFinderService;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;

final class InventoryMoveHandler {

    private static final Logger LOG = Logger.getLogger(InventoryMoveHandler.class.getName());

    private final ItemFinderService service;

    InventoryMoveHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x, final UUID inventoryItemId) {
        return EndpointSupport.execute(LOG, x, () -> {
            final Request request = ExchangeAdapters.request(x);
            final var body = JsonUtil.MAPPER.readValue(Request.asInputStream(request), MoveInventoryItemRequest.class);
            final var movement = service.moveInventoryItem(inventoryItemId, body.toHouseLocationLeafId(), body.movedBy(),
                    body.movementReason(), body.notes());
            HttpUtil.ok(x, movement);
        });
    }
}
