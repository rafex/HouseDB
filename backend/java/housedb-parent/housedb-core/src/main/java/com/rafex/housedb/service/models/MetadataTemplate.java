package com.rafex.housedb.service.models;

import java.util.UUID;

public record MetadataTemplate(
        UUID metadataTemplateId,
        String metadataTarget,
        String code,
        String name,
        String description,
        String definitionJson,
        boolean enabled) {
}
