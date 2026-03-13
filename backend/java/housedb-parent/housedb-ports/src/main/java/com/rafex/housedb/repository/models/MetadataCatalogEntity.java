package com.rafex.housedb.repository.models;

import java.util.UUID;

public record MetadataCatalogEntity(
        UUID metadataCatalogId,
        String metadataTarget,
        String code,
        String name,
        String description,
        String payloadJson,
        boolean enabled
) {
}
