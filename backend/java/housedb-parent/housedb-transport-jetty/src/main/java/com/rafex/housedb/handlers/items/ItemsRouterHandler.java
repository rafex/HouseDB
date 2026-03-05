package com.rafex.housedb.handlers.items;

import com.rafex.housedb.kiwi.KiwiApiClient;
import com.rafex.housedb.services.ItemFinderService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

public final class ItemsRouterHandler extends NonBlockingResourceHandler {

    private final InventoryListHandler listHandler;
    private final InventorySearchHandler searchHandler;
    private final InventoryNearbyHandler nearbyHandler;
    private final InventoryByLocationHandler byLocationHandler;
    private final InventoryCreateHandler createHandler;
    private final InventoryMoveHandler moveHandler;
    private final InventoryTimelineHandler timelineHandler;
    private final InventoryFavoriteHandler favoriteHandler;
    private final ItemDetailHandler itemDetailHandler;

    public ItemsRouterHandler(final JsonCodec jsonCodec, final ItemFinderService service, final KiwiApiClient kiwiApiClient) {
        super(jsonCodec);
        listHandler = new InventoryListHandler(service);
        searchHandler = new InventorySearchHandler(service);
        nearbyHandler = new InventoryNearbyHandler(service);
        byLocationHandler = new InventoryByLocationHandler(service);
        createHandler = new InventoryCreateHandler(service, kiwiApiClient);
        moveHandler = new InventoryMoveHandler(service);
        timelineHandler = new InventoryTimelineHandler(service);
        favoriteHandler = new InventoryFavoriteHandler(service);
        itemDetailHandler = new ItemDetailHandler(kiwiApiClient, service);
    }

    @Override
    protected String basePath() {
        return "/items";
    }

    @Override
    protected List<Route> routes() {
        return List.of(
                Route.of("/", Set.of("GET", "POST")),
                Route.of("/search", Set.of("GET")),
                Route.of("/nearby", Set.of("GET")),
                Route.of("/by-location", Set.of("GET")),
                Route.of("/{inventoryItemId}/move", Set.of("PATCH")),
                Route.of("/{inventoryItemId}/timeline", Set.of("GET")),
                Route.of("/{inventoryItemId}/favorite", Set.of("PUT")),
                Route.of("/{itemId}", Set.of("GET")));
    }

    @Override
    public boolean get(final HttpExchange x) {
        final var path = x.path();
        if ("/items".equals(path)) {
            return listHandler.handle(x);
        }
        if ("/items/search".equals(path)) {
            return searchHandler.handle(x);
        }
        if ("/items/nearby".equals(path)) {
            return nearbyHandler.handle(x);
        }
        if ("/items/by-location".equals(path)) {
            return byLocationHandler.handle(x);
        }
        final var timelineId = x.pathParam("inventoryItemId");
        if (timelineId != null && path.endsWith("/timeline")) {
            return timelineHandler.handle(x, UUID.fromString(timelineId));
        }
        final var itemId = x.pathParam("itemId");
        if (itemId != null) {
            return itemDetailHandler.handle(x, UUID.fromString(itemId));
        }
        return false;
    }

    @Override
    public boolean post(final HttpExchange x) {
        if ("/items".equals(x.path())) {
            return createHandler.handle(x);
        }
        return false;
    }

    @Override
    public boolean patch(final HttpExchange x) {
        final var inventoryItemId = x.pathParam("inventoryItemId");
        if (inventoryItemId != null) {
            return moveHandler.handle(x, UUID.fromString(inventoryItemId));
        }
        return false;
    }

    @Override
    public boolean put(final HttpExchange x) {
        final var inventoryItemId = x.pathParam("inventoryItemId");
        if (inventoryItemId != null) {
            return favoriteHandler.handle(x, UUID.fromString(inventoryItemId));
        }
        return false;
    }
}
