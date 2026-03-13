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
