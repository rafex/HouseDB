package com.rafex.housedb.handlers.metadata;

import com.rafex.housedb.handlers.ExchangeAdapters;
import com.rafex.housedb.services.MetadataTemplateService;

import java.util.Map;
import java.util.logging.Logger;

import dev.rafex.ether.http.core.HttpExchange;

final class ListMetadataTemplatesHandler {

    private static final Logger LOG = Logger.getLogger(ListMetadataTemplatesHandler.class.getName());

    private final MetadataTemplateService service;

    ListMetadataTemplatesHandler(final MetadataTemplateService service) {
        this.service = service;
    }

    boolean handle(final HttpExchange x) {
        return MetadataRouterSupport.execute(LOG, x, () -> {
            final var query = MetadataRequestParsers.parseQuery(ExchangeAdapters.rawQuery(x));
            final var metadataTarget = MetadataRequestParsers.parseOptionalString(query, "metadataTarget");
            final var includeDisabled = MetadataRequestParsers.parseOptionalBoolean(query, "includeDisabled");
            final var limit = MetadataRequestParsers.parseOptionalInt(query, "limit");
            final var offset = MetadataRequestParsers.parseOptionalInt(query, "offset");

            final var templates = service.listMetadataTemplates(metadataTarget, includeDisabled, limit, offset);
            x.json(200, Map.of("templates", templates, "count", templates.size()));
        });
    }
}
