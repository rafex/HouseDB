package com.rafex.housedb.handlers.houses;

import com.rafex.housedb.kiwi.KiwiApiClient;
import com.rafex.housedb.services.HouseService;
import com.rafex.housedb.services.ItemFinderService;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class HousesRouterHandler extends Handler.Abstract {

    private final CreateHouseHandler createHouseHandler;
    private final UpsertHouseMemberHandler upsertHouseMemberHandler;
    private final ListHousesHandler listHousesHandler;
    private final ListHouseMembersHandler listHouseMembersHandler;
    private final CreateHouseLocationHandler createHouseLocationHandler;

    public HousesRouterHandler(final HouseService houseService, final ItemFinderService itemService,
            final KiwiApiClient kiwiApiClient) {
        createHouseHandler = new CreateHouseHandler(houseService);
        upsertHouseMemberHandler = new UpsertHouseMemberHandler(houseService);
        listHousesHandler = new ListHousesHandler(houseService);
        listHouseMembersHandler = new ListHouseMembersHandler(houseService);
        createHouseLocationHandler = new CreateHouseLocationHandler(kiwiApiClient, itemService);
    }

    @Override
    public boolean handle(final Request request, final Response response, final Callback callback) {
        final String method = request.getMethod();
        final String path = request.getHttpURI().getPath();

        if ("POST".equals(method) && "/houses".equals(path)) {
            return createHouseHandler.handle(request, response, callback);
        }
        if ("GET".equals(method) && "/houses".equals(path)) {
            return listHousesHandler.handle(request, response, callback);
        }

        if (("POST".equals(method) || "PUT".equals(method))
                && path.startsWith("/houses/")
                && path.endsWith("/members")) {
            final var houseId = HouseRequestParsers.extractHouseIdFromMembersPath(path);
            return upsertHouseMemberHandler.handle(request, response, callback, houseId);
        }
        if ("GET".equals(method)
                && path.startsWith("/houses/")
                && path.endsWith("/members")) {
            final var houseId = HouseRequestParsers.extractHouseIdFromMembersPath(path);
            return listHouseMembersHandler.handle(request, response, callback, houseId);
        }
        if ("POST".equals(method)
                && path.startsWith("/houses/")
                && path.endsWith("/locations")) {
            final var houseId = HouseRequestParsers.extractHouseIdFromLocationsPath(path);
            return createHouseLocationHandler.handle(request, response, callback, houseId);
        }

        return false;
    }
}
