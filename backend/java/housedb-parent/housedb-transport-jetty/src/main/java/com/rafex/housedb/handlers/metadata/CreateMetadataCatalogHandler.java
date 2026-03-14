package com.rafex.housedb.handlers.metadata;

import com.rafex.housedb.dtos.CreateMetadataCatalogRequest;
import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.services.MetadataCatalogService;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.json.JsonCodec;

final class CreateMetadataCatalogHandler {

    private static final Logger LOG = Logger.getLogger(CreateMetadataCatalogHandler.class.getName());

    private final JsonCodec jsonCodec;
    private final MetadataCatalogService service;

    CreateMetadataCatalogHandler(final JsonCodec jsonCodec, final MetadataCatalogService service) {
        this.jsonCodec = jsonCodec;
        this.service = service;
    }

    boolean handle(final HttpExchange x) {
        return MetadataRouterSupport.execute(LOG, x, () -> {
            AuthzSupport.requireAppOrAdmin(x);

            final Request request = ExchangeAdapters.request(x);
            final var body = jsonCodec.readValue(Request.asInputStream(request), CreateMetadataCatalogRequest.class);
            final var result = service.createMetadataCatalog(
                    body.metadataTarget(),
                    body.code(),
                    body.name(),
                    body.description(),
                    body.payloadJson(),
                    body.enabled());
            x.json(200, result);
        });
    }
}
