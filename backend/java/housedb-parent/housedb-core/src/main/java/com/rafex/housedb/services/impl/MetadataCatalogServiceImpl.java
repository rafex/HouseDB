package com.rafex.housedb.services.impl;

import com.rafex.housedb.repository.MetadataCatalogRepository;
import com.rafex.housedb.service.models.MetadataCatalog;
import com.rafex.housedb.services.MetadataCatalogService;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MetadataCatalogServiceImpl implements MetadataCatalogService {

    private static final int DEFAULT_LIMIT = 50;
    private static final Set<String> ALLOWED_TARGETS = Set.of("kiwi_object", "inventory_item");

    private final MetadataCatalogRepository repository;

    public MetadataCatalogServiceImpl(final MetadataCatalogRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public List<MetadataCatalog> listMetadataCatalogs(final String metadataTarget, final Boolean includeDisabled,
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

        return repository.listMetadataCatalogs(safeTarget,
                includeDisabled == null ? Boolean.FALSE : includeDisabled,
                normalizeLimit(limit, 500),
                normalizeOffset(offset)).stream()
                        .map(entity -> new MetadataCatalog(
                                entity.metadataCatalogId(),
                                entity.metadataTarget(),
                                entity.code(),
                                entity.name(),
                                entity.description(),
                                entity.payloadJson(),
                                entity.enabled()))
                        .toList();
    }

    @Override
    public MetadataCatalog createMetadataCatalog(final String metadataTarget, final String code, final String name,
            final String description, final String payloadJson, final Boolean enabled) throws SQLException {
        final var entity = repository.createMetadataCatalog(
                normalizeTarget(metadataTarget),
                requireText(code, "code"),
                requireText(name, "name"),
                normalizeOptionalText(description),
                normalizePayloadJson(payloadJson),
                enabled == null ? true : enabled);

        return new MetadataCatalog(
                entity.metadataCatalogId(),
                entity.metadataTarget(),
                entity.code(),
                entity.name(),
                entity.description(),
                entity.payloadJson(),
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

    private static String normalizePayloadJson(final String value) {
        if (value == null || value.isBlank()) {
            return "{}";
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
