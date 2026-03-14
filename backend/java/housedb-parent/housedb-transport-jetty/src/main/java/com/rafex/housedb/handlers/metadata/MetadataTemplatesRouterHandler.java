package com.rafex.housedb.handlers.metadata;

import com.rafex.housedb.handlers.support.HouseDbErrorMapper;
import com.rafex.housedb.services.MetadataTemplateService;

import java.util.List;
import java.util.Set;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.http.core.Route;
import dev.rafex.ether.http.jetty12.NonBlockingResourceHandler;
import dev.rafex.ether.json.JsonCodec;

public final class MetadataTemplatesRouterHandler extends NonBlockingResourceHandler {

    private final ListMetadataTemplatesHandler listMetadataTemplatesHandler;

    public MetadataTemplatesRouterHandler(final JsonCodec jsonCodec, final MetadataTemplateService service) {
        super(jsonCodec, new HouseDbErrorMapper());
        listMetadataTemplatesHandler = new ListMetadataTemplatesHandler(service);
    }

    @Override
    protected String basePath() {
        return "/metadata-templates";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/", Set.of("GET")));
    }

    @Override
    public boolean get(final HttpExchange x) {
        if ("/metadata-templates".equals(x.path())) {
            return listMetadataTemplatesHandler.handle(x);
        }
        return false;
    }
}
