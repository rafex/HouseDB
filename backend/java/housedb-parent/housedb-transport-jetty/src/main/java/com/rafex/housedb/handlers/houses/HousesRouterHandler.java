package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.handlers.support.HouseDbErrorMapper;
import com.rafex.housedb.kiwi.KiwiApiClient;
import com.rafex.housedb.services.HouseService;
import com.rafex.housedb.services.ItemFinderService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

public final class HousesRouterHandler extends NonBlockingResourceHandler {

    private final CreateHouseHandler createHouseHandler;
    private final UpsertHouseMemberHandler upsertHouseMemberHandler;
    private final ListHousesHandler listHousesHandler;
    private final ListHouseIdsHandler listHouseIdsHandler;
    private final ListHouseMembersHandler listHouseMembersHandler;
    private final ListHouseLocationsHandler listHouseLocationsHandler;
    private final CreateHouseLocationHandler createHouseLocationHandler;

    public HousesRouterHandler(final JsonCodec jsonCodec, final HouseService houseService, final ItemFinderService itemService,
            final KiwiApiClient kiwiApiClient) {
        super(jsonCodec, new HouseDbErrorMapper());
        createHouseHandler = new CreateHouseHandler(jsonCodec, houseService);
        upsertHouseMemberHandler = new UpsertHouseMemberHandler(jsonCodec, houseService);
        listHousesHandler = new ListHousesHandler(houseService);
        listHouseIdsHandler = new ListHouseIdsHandler(houseService);
        listHouseMembersHandler = new ListHouseMembersHandler(houseService);
        listHouseLocationsHandler = new ListHouseLocationsHandler(houseService);
        createHouseLocationHandler = new CreateHouseLocationHandler(jsonCodec, kiwiApiClient, itemService);
    }

    @Override
    protected String basePath() {
        return "/houses";
    }

    @Override
    protected List<Route> routes() {
        return List.of(
                Route.of("/", Set.of("GET", "POST")),
                Route.of("/ids", Set.of("GET")),
                Route.of("/{houseId}/members", Set.of("GET", "POST", "PUT")),
                Route.of("/{houseId}/locations", Set.of("GET", "POST")));
    }

    @Override
    public boolean get(final HttpExchange x) {
        final var path = x.path();
        if ("/houses".equals(path)) {
            return listHousesHandler.handle(x);
        }
        if ("/houses/ids".equals(path)) {
            return listHouseIdsHandler.handle(x);
        }
        final var houseId = x.pathParam("houseId");
        if (houseId != null && path.endsWith("/members")) {
            return listHouseMembersHandler.handle(x, UUID.fromString(houseId));
        }
        if (houseId != null && path.endsWith("/locations")) {
            return listHouseLocationsHandler.handle(x, UUID.fromString(houseId));
        }
        return false;
    }

    @Override
    public boolean post(final HttpExchange x) {
        final var path = x.path();
        if ("/houses".equals(path)) {
            return createHouseHandler.handle(x);
        }
        final var houseId = x.pathParam("houseId");
        if (houseId == null) {
            return false;
        }
        if (path.endsWith("/members")) {
            return upsertHouseMemberHandler.handle(x, UUID.fromString(houseId));
        }
        if (path.endsWith("/locations")) {
            return createHouseLocationHandler.handle(x, UUID.fromString(houseId));
        }
        return false;
    }

    @Override
    public boolean put(final HttpExchange x) {
        final var houseId = x.pathParam("houseId");
        if (houseId != null && x.path().endsWith("/members")) {
            return upsertHouseMemberHandler.handle(x, UUID.fromString(houseId));
        }
        return false;
    }
}
