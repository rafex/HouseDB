package com.rafex.housedb.dtos;

public record CreateMetadataCatalogRequest(
        String metadataTarget,
        String code,
        String name,
        String description,
        String payloadJson,
        Boolean enabled
) {
}
