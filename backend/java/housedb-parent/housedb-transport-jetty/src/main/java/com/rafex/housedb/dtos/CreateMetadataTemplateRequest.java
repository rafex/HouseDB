package com.rafex.housedb.dtos;

public record CreateMetadataTemplateRequest(
        String metadataTarget,
        String code,
        String name,
        String description,
        String definitionJson,
        Boolean enabled
) {
}
