package com.rafex.housedb.handlers.metadata;

import com.rafex.housedb.handlers.support.HouseDbErrorMapper;
import com.rafex.housedb.services.MetadataCatalogService;

import java.util.List;
import java.util.Set;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

public final class MetadataCatalogsRouterHandler extends NonBlockingResourceHandler {

    private final CreateMetadataCatalogHandler createMetadataCatalogHandler;
    private final ListMetadataCatalogsHandler listMetadataCatalogsHandler;

    public MetadataCatalogsRouterHandler(final JsonCodec jsonCodec, final MetadataCatalogService service) {
        super(jsonCodec, new HouseDbErrorMapper());
        createMetadataCatalogHandler = new CreateMetadataCatalogHandler(jsonCodec, service);
        listMetadataCatalogsHandler = new ListMetadataCatalogsHandler(service);
    }

    @Override
    protected String basePath() {
        return "/metadata-catalogs";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/", Set.of("GET", "POST")));
    }

    @Override
    public boolean get(final HttpExchange x) {
        if ("/metadata-catalogs".equals(x.path())) {
            return listMetadataCatalogsHandler.handle(x);
        }
        return false;
    }

    @Override
    public boolean post(final HttpExchange x) {
        if ("/metadata-catalogs".equals(x.path())) {
            return createMetadataCatalogHandler.handle(x);
        }
        return false;
    }
}
