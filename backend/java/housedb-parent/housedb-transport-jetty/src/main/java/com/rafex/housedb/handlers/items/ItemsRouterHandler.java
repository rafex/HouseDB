package com.rafex.housedb.handlers.items;

import com.rafex.housedb.kiwi.KiwiApiClient;
import com.rafex.housedb.services.ItemFinderService;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class ItemsRouterHandler extends Handler.Abstract {

    private final InventorySearchHandler searchHandler;
    private final InventoryNearbyHandler nearbyHandler;
    private final InventoryByLocationHandler byLocationHandler;
    private final InventoryCreateHandler createHandler;
    private final InventoryMoveHandler moveHandler;
    private final InventoryTimelineHandler timelineHandler;
    private final InventoryFavoriteHandler favoriteHandler;
    private final ItemDetailHandler itemDetailHandler;

    public ItemsRouterHandler(final ItemFinderService service, final KiwiApiClient kiwiApiClient) {
        searchHandler = new InventorySearchHandler(service);
        nearbyHandler = new InventoryNearbyHandler(service);
        byLocationHandler = new InventoryByLocationHandler(service);
        createHandler = new InventoryCreateHandler(service);
        moveHandler = new InventoryMoveHandler(service);
        timelineHandler = new InventoryTimelineHandler(service);
        favoriteHandler = new InventoryFavoriteHandler(service);
        itemDetailHandler = new ItemDetailHandler(kiwiApiClient, service);
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) {
        final String method = request.getMethod();
        final String path = request.getHttpURI().getPath();

        if ("GET".equals(method) && "/items/search".equals(path)) {
            return searchHandler.handle(request, response, callback);
        }
        if ("GET".equals(method) && "/items/nearby".equals(path)) {
            return nearbyHandler.handle(request, response, callback);
        }
        if ("GET".equals(method) && "/items/by-location".equals(path)) {
            return byLocationHandler.handle(request, response, callback);
        }
        if ("POST".equals(method) && "/items".equals(path)) {
            return createHandler.handle(request, response, callback);
        }
        if ("PATCH".equals(method) && path.startsWith("/items/") && path.endsWith("/move")) {
            final var inventoryItemId = ItemRequestParsers.extractInventoryItemId(path, "/move");
            return moveHandler.handle(request, response, callback, inventoryItemId);
        }
        if ("GET".equals(method) && path.startsWith("/items/") && path.endsWith("/timeline")) {
            final var inventoryItemId = ItemRequestParsers.extractInventoryItemId(path, "/timeline");
            return timelineHandler.handle(request, response, callback, inventoryItemId);
        }
        if ("PUT".equals(method) && path.startsWith("/items/") && path.endsWith("/favorite")) {
            final var inventoryItemId = ItemRequestParsers.extractInventoryItemId(path, "/favorite");
            return favoriteHandler.handle(request, response, callback, inventoryItemId);
        }
        if ("GET".equals(method) && (path.startsWith("/items/") || path.startsWith("/item/"))) {
            final var itemId = ItemRequestParsers.extractItemId(path);
            return itemDetailHandler.handle(request, response, callback, itemId);
        }
        return false;
    }
}
