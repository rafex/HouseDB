package com.rafex.housedb.handlers.metadata;

import com.rafex.housedb.dtos.CreateMetadataTemplateRequest;
import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.services.MetadataTemplateService;

import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;

import dev.rafex.ether.http.core.HttpExchange;
import dev.rafex.ether.json.JsonCodec;

final class CreateMetadataTemplateHandler {

    private static final Logger LOG = Logger.getLogger(CreateMetadataTemplateHandler.class.getName());

    private final JsonCodec jsonCodec;
    private final MetadataTemplateService service;

    CreateMetadataTemplateHandler(final JsonCodec jsonCodec, final MetadataTemplateService service) {
        this.jsonCodec = jsonCodec;
        this.service = service;
    }

    boolean handle(final HttpExchange x) {
        return MetadataRouterSupport.execute(LOG, x, () -> {
            AuthzSupport.requireAppOrAdmin(x);

            final Request request = ExchangeAdapters.request(x);
            final var body = jsonCodec.readValue(Request.asInputStream(request), CreateMetadataTemplateRequest.class);
            final var result = service.createMetadataTemplate(
                    body.metadataTarget(),
                    body.code(),
                    body.name(),
                    body.description(),
                    body.definitionJson(),
                    body.enabled());
            x.json(200, result);
        });
    }
}
