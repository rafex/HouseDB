package com.rafex.housedb.handlers.items;

import com.rafex.housedb.dtos.CreateInventoryItemRequest;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.services.ItemFinderService;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class InventoryCreateHandler {

    private static final Logger LOG = Logger.getLogger(InventoryCreateHandler.class.getName());

    private final ItemFinderService service;

    InventoryCreateHandler(final ItemFinderService service) {
        this.service = service;
    }

    boolean handle(final Request request, final Response response, final Callback callback) {
        return EndpointSupport.execute(LOG, response, callback, () -> {
            final var body = JsonUtil.MAPPER.readValue(Request.asInputStream(request), CreateInventoryItemRequest.class);
            final var result = service.createInventoryItem(body.userId(), body.objectId(), body.nickname(),
                    body.serialNumber(), body.conditionStatus(), body.houseLocationLeafId(), body.movedBy(),
                    body.notes());
            HttpUtil.ok(response, callback, result);
        });
    }
}
