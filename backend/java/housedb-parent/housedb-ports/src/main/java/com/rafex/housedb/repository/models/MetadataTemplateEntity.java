package com.rafex.housedb.repository.models;

import java.util.UUID;

public record MetadataTemplateEntity(
        UUID metadataTemplateId,
        String metadataTarget,
        String code,
        String name,
        String description,
        String definitionJson,
        boolean enabled) {
}
