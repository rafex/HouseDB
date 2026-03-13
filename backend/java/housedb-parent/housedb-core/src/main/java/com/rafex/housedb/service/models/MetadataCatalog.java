package com.rafex.housedb.service.models;

import java.util.UUID;

public record MetadataCatalog(
        UUID metadataCatalogId,
        String metadataTarget,
        String code,
        String name,
        String description,
        String payloadJson,
        boolean enabled
) {
}
