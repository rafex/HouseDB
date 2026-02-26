package com.rafex.housedb.handlers.items;

import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.services.ItemFinderService;

import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class InventorySearchHandler {

    private static final Logger LOG = Logger.getLogger(InventorySearchHandler.class.getName());

    private final ItemFinderService service;

    InventorySearchHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback) {
        return EndpointSupport.execute(LOG, response, callback, () -> {
            final var query = ItemRequestParsers.parseQuery(request.getHttpURI().getQuery());
            final var userId = AuthzSupport.requireTokenUser(request);
            final String text = ItemRequestParsers.getValue(query, "q");
            final var houseId = ItemRequestParsers.parseOptionalUuid(query, "houseId");
            final var houseLocationLeafId = ItemRequestParsers.parseOptionalUuid(query, "houseLocationLeafId");
            final Integer limit = ItemRequestParsers.parseOptionalInt(query, "limit");

            final var items = service.searchInventoryItems(userId, text, houseId, houseLocationLeafId, limit);
            HttpUtil.ok(response, callback, Map.of("items", items, "count", items.size()));
        });
    }
}
