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

    private final CreateMetadataTemplateHandler createMetadataTemplateHandler;
    private final ListMetadataTemplatesHandler listMetadataTemplatesHandler;

    public MetadataTemplatesRouterHandler(final JsonCodec jsonCodec, final MetadataTemplateService service) {
        super(jsonCodec, new HouseDbErrorMapper());
        createMetadataTemplateHandler = new CreateMetadataTemplateHandler(jsonCodec, service);
        listMetadataTemplatesHandler = new ListMetadataTemplatesHandler(service);
    }

    @Override
    protected String basePath() {
        return "/metadata-templates";
    }

    @Override
    protected List<Route> routes() {
        return List.of(Route.of("/", Set.of("GET", "POST")));
    }

    @Override
    public boolean get(final HttpExchange x) {
        if ("/metadata-templates".equals(x.path())) {
            return listMetadataTemplatesHandler.handle(x);
        }
        return false;
    }

    @Override
    public boolean post(final HttpExchange x) {
        if ("/metadata-templates".equals(x.path())) {
            return createMetadataTemplateHandler.handle(x);
        }
        return false;
    }
}
