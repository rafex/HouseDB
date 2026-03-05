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

public final class ItemAliasRouterHandler extends NonBlockingResourceHandler {

    private final ItemDetailHandler itemDetailHandler;

    public ItemAliasRouterHandler(final JsonCodec jsonCodec, final KiwiApiClient kiwiApiClient,
            final ItemFinderService itemFinderService) {
        super(jsonCodec);
        itemDetailHandler = new ItemDetailHandler(kiwiApiClient, itemFinderService);
    }

    @Override
    protected String basePath() {
        return "/item";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/{itemId}", Set.of("GET")));
    }

    @Override
    public boolean get(final HttpExchange x) {
        final var itemId = x.pathParam("itemId");
        return itemDetailHandler.handle(x, UUID.fromString(itemId));
    }
}
