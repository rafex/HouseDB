package com.rafex.housedb.handlers.items;

import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.kiwi.KiwiApiClient;
import com.rafex.housedb.services.ItemFinderService;

import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class ItemDetailHandler {

    private static final Logger LOG = Logger.getLogger(ItemDetailHandler.class.getName());

    private final KiwiApiClient kiwiApiClient;
    private final ItemFinderService itemService;

    ItemDetailHandler(final KiwiApiClient kiwiApiClient, final ItemFinderService itemService) {
        this.kiwiApiClient = kiwiApiClient;
        this.itemService = itemService;
    }

    boolean handle(final HttpExchange x, final UUID itemId) {
        try {
            final var inventoryItem = itemService.getInventoryItemDetail(itemId);
            if (inventoryItem == null) {
                HttpUtil.notFound(x, x.path());
                return true;
            }

            final var payload = new HashMap<String, Object>();
            payload.put("inventoryItem", inventoryItem);
            payload.put("kiwiObject", null);
            payload.put("kiwiStatus", "not_linked");

            if (inventoryItem.objectKiwiId() != null) {
                try {
                    final var kiwiObject = kiwiApiClient.getObjectById(inventoryItem.objectKiwiId());
                    payload.put("kiwiObject", kiwiObject);
                    payload.put("kiwiStatus", "ok");
                } catch (final KiwiApiClient.KiwiApiException e) {
                    if (e.statusCode() == 404) {
                        payload.put("kiwiStatus", "not_found");
                    } else {
                        throw e;
                    }
                }
            }

            HttpUtil.ok(x, payload);
            return true;
        } catch (final IllegalArgumentException e) {
            HttpUtil.badRequest(x, e.getMessage());
            return true;
        } catch (final KiwiApiClient.KiwiApiException e) {
            if (e.statusCode() == 404) {
                HttpUtil.notFound(x, x.path());
                return true;
            }
            LOG.log(Level.SEVERE, "Kiwi API error", e);
            HttpUtil.internalServerError(x, "kiwi api error");
            return true;
        } catch (final Exception e) {
            LOG.log(Level.SEVERE, "Unhandled error", e);
            HttpUtil.internalServerError(x, "internal error");
            return true;
        }
    }
}
