package com.rafex.housedb.services.impl;

import com.rafex.housedb.repository.MetadataTemplateRepository;
import com.rafex.housedb.service.models.MetadataTemplate;
import com.rafex.housedb.services.MetadataTemplateService;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MetadataTemplateServiceImpl implements MetadataTemplateService {

    private static final int DEFAULT_LIMIT = 50;
    private static final Set<String> ALLOWED_TARGETS = Set.of("kiwi_object", "inventory_item");

    private final MetadataTemplateRepository repository;

    public MetadataTemplateServiceImpl(final MetadataTemplateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public List<MetadataTemplate> listMetadataTemplates(final String metadataTarget, final Boolean includeDisabled,
            final Integer limit, final Integer offset) throws SQLException {
        final String safeTarget;
        if (metadataTarget == null || metadataTarget.isBlank()) {
            safeTarget = null;
        } else {
            safeTarget = metadataTarget.trim().toLowerCase();
            if (!ALLOWED_TARGETS.contains(safeTarget)) {
                throw new IllegalArgumentException("metadataTarget must be one of: kiwi_object, inventory_item");
            }
        }

        return repository.listMetadataTemplates(safeTarget,
                includeDisabled == null ? Boolean.FALSE : includeDisabled,
                normalizeLimit(limit, 500),
                normalizeOffset(offset)).stream()
                        .map(entity -> new MetadataTemplate(
                                entity.metadataTemplateId(),
                                entity.metadataTarget(),
                                entity.code(),
                                entity.name(),
                                entity.description(),
                                entity.definitionJson(),
                                entity.enabled()))
                        .toList();
    }

    @Override
    public MetadataTemplate createMetadataTemplate(final String metadataTarget, final String code, final String name,
            final String description, final String definitionJson, final Boolean enabled) throws SQLException {
        final var entity = repository.createMetadataTemplate(
                normalizeTarget(metadataTarget),
                requireText(code, "code"),
                requireText(name, "name"),
                normalizeOptionalText(description),
                normalizeDefinitionJson(definitionJson),
                enabled == null ? true : enabled);

        return new MetadataTemplate(
                entity.metadataTemplateId(),
                entity.metadataTarget(),
                entity.code(),
                entity.name(),
                entity.description(),
                entity.definitionJson(),
                entity.enabled());
    }

    private static String normalizeTarget(final String metadataTarget) {
        if (metadataTarget == null || metadataTarget.isBlank()) {
            throw new IllegalArgumentException("metadataTarget is required");
        }

        final var safeTarget = metadataTarget.trim().toLowerCase();
        if (!ALLOWED_TARGETS.contains(safeTarget)) {
            throw new IllegalArgumentException("metadataTarget must be one of: kiwi_object, inventory_item");
        }
        return safeTarget;
    }

    private static String requireText(final String value, final String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalText(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeDefinitionJson(final String value) {
        if (value == null || value.isBlank()) {
            return "[]";
        }
        return value.trim();
    }

    private static int normalizeLimit(final Integer value, final int max) {
        if (value == null || value < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, max);
    }

    private static int normalizeOffset(final Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        return value;
    }
}
