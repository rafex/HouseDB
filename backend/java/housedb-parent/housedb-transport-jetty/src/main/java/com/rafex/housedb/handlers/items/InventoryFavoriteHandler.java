package com.rafex.housedb.handlers.items;

import com.rafex.housedb.dtos.SetFavoriteRequest;
import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.services.ItemFinderService;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.json.JsonUtils;

final class InventoryFavoriteHandler {

    private static final Logger LOG = Logger.getLogger(InventoryFavoriteHandler.class.getName());

    private final ItemFinderService service;

    InventoryFavoriteHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x, final UUID inventoryItemId) {
        return EndpointSupport.execute(LOG, x, () -> {
            final Request request = ExchangeAdapters.request(x);
            final var body = JsonUtils.fromJson(Request.asInputStream(request), SetFavoriteRequest.class);
            final var userId = AuthzSupport.requireTokenUser(x);
            final var state = service.setFavoriteItem(userId, inventoryItemId, body.isFavorite(), body.note());
            x.json(200, state);
        });
    }
}
