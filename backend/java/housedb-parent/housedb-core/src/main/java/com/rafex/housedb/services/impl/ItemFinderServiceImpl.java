package com.rafex.housedb.services.impl;

import com.rafex.housedb.repository.HouseLocationSyncRepository;
import com.rafex.housedb.repository.InventoryMutationRepository;
import com.rafex.housedb.repository.InventorySearchRepository;
import com.rafex.housedb.service.models.FavoriteState;
import com.rafex.housedb.service.models.HouseItem;
import com.rafex.housedb.service.models.InventoryCreateResult;
import com.rafex.housedb.service.models.InventoryItemDetail;
import com.rafex.housedb.service.models.InventoryTimelineEvent;
import com.rafex.housedb.service.models.ItemMovement;
import com.rafex.housedb.service.models.LocationInventoryItem;
import com.rafex.housedb.service.models.NearbyInventoryItem;
import com.rafex.housedb.services.ItemFinderService;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ItemFinderServiceImpl implements ItemFinderService {

    private static final int DEFAULT_LIMIT = 50;

    private final InventorySearchRepository searchRepository;
    private final InventoryMutationRepository mutationRepository;
    private final HouseLocationSyncRepository locationSyncRepository;
    private final ItemModelMapper mapper;

    public ItemFinderServiceImpl(final InventorySearchRepository searchRepository,
            final InventoryMutationRepository mutationRepository,
            final HouseLocationSyncRepository locationSyncRepository) {
        this(searchRepository, mutationRepository, locationSyncRepository, new ItemModelMapper());
    }

    ItemFinderServiceImpl(final InventorySearchRepository searchRepository,
            final InventoryMutationRepository mutationRepository,
            final HouseLocationSyncRepository locationSyncRepository,
            final ItemModelMapper mapper) {
        this.searchRepository = Objects.requireNonNull(searchRepository, "searchRepository");
        this.mutationRepository = Objects.requireNonNull(mutationRepository, "mutationRepository");
        this.locationSyncRepository = Objects.requireNonNull(locationSyncRepository, "locationSyncRepository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public List<HouseItem> searchInventoryItems(final UUID userId, final String text, final UUID houseId,
            final UUID houseLocationLeafId, final Integer limit) throws SQLException {
        requireUser(userId);
        return mapper.toHouseItems(
                searchRepository.searchInventoryItems(userId, text, houseId, houseLocationLeafId, normalizeLimit(limit)));
    }

    @Override
    public ItemMovement moveInventoryItem(final UUID inventoryItemId, final UUID toHouseLocationLeafId,
            final String movedBy, final String movementReason, final String notes) throws SQLException {
        if (inventoryItemId == null || toHouseLocationLeafId == null) {
            throw new IllegalArgumentException("inventoryItemId and toHouseLocationLeafId are required");
        }
        return mapper.toItemMovement(mutationRepository.moveInventoryItem(inventoryItemId, toHouseLocationLeafId,
                movedBy, movementReason, notes));
    }

    @Override
    public InventoryCreateResult createInventoryItem(final UUID userId, final UUID objectId, final String nickname,
            final String serialNumber, final String conditionStatus, final UUID houseLocationLeafId,
            final String movedBy, final String notes) throws SQLException {
        requireUser(userId);
        if (objectId == null) {
            throw new IllegalArgumentException("objectId is required");
        }
        final var safeStatus = conditionStatus == null || conditionStatus.isBlank() ? "active" : conditionStatus;
        return mapper.toInventoryCreateResult(mutationRepository.createInventoryItem(userId, objectId, nickname,
                serialNumber, safeStatus, houseLocationLeafId, movedBy, notes));
    }

    @Override
    public UUID upsertHouseLocationFromKiwi(final UUID houseId, final UUID kiwiLocationId,
            final UUID kiwiParentLocationId, final UUID parentHouseLocationId, final String locationKind,
            final String name, final Boolean isLeaf, final String path, final String referenceCode, final String notes,
            final Double latitude, final Double longitude, final Boolean enabled) throws SQLException {
        if (houseId == null || kiwiLocationId == null) {
            throw new IllegalArgumentException("houseId and kiwiLocationId are required");
        }
        final var safeKind = locationKind == null || locationKind.isBlank() ? "slot" : locationKind;
        return locationSyncRepository.upsertHouseLocationFromKiwi(houseId, kiwiLocationId, kiwiParentLocationId,
                parentHouseLocationId, safeKind, name, isLeaf, path, referenceCode, notes, latitude, longitude,
                enabled);
    }

    @Override
    public List<LocationInventoryItem> listInventoryByLocation(final UUID userId, final UUID houseId,
            final UUID houseLocationId, final Boolean includeDescendants, final Integer limit) throws SQLException {
        requireUser(userId);
        return mapper.toLocationInventoryItems(searchRepository.listInventoryByLocation(userId, houseId, houseLocationId,
                includeDescendants == null ? Boolean.TRUE : includeDescendants, normalizeLimit(limit, 200)));
    }

    @Override
    public List<InventoryTimelineEvent> inventoryItemTimeline(final UUID inventoryItemId, final Integer limit)
            throws SQLException {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId is required");
        }
        return mapper.toInventoryTimelineEvents(
                searchRepository.inventoryItemTimeline(inventoryItemId, normalizeLimit(limit, 100)));
    }

    @Override
    public FavoriteState setFavoriteItem(final UUID userId, final UUID inventoryItemId, final Boolean isFavorite,
            final String note) throws SQLException {
        requireUser(userId);
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId is required");
        }
        return mapper.toFavoriteState(
                mutationRepository.setFavoriteItem(userId, inventoryItemId, isFavorite == null ? Boolean.TRUE : isFavorite, note));
    }

    @Override
    public List<NearbyInventoryItem> searchInventoryItemsNearPoint(final UUID userId, final double latitude,
            final double longitude, final Double radiusMeters, final Integer limit) throws SQLException {
        requireUser(userId);
        final var safeRadius = radiusMeters == null || radiusMeters <= 0 ? 1000D : radiusMeters;
        return mapper.toNearbyInventoryItems(
                searchRepository.searchInventoryItemsNearPoint(userId, latitude, longitude, safeRadius, normalizeLimit(limit)));
    }

    @Override
    public InventoryItemDetail getInventoryItemDetail(final UUID inventoryItemId) throws SQLException {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("inventoryItemId is required");
        }
        return mapper.toInventoryItemDetail(searchRepository.getInventoryItemDetail(inventoryItemId));
    }

    private static void requireUser(final UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private static int normalizeLimit(final Integer value) {
        return normalizeLimit(value, DEFAULT_LIMIT);
    }

    private static int normalizeLimit(final Integer value, final int def) {
        if (value == null || value < 1) {
            return def;
        }
        return Math.min(value, 200);
    }
}
