package com.rafex.housedb.services;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.rafex.housedb.service.models.FavoriteState;
import com.rafex.housedb.service.models.HouseItem;
import com.rafex.housedb.service.models.InventoryCreateResult;
import com.rafex.housedb.service.models.InventoryItemDetail;
import com.rafex.housedb.service.models.InventoryTimelineEvent;
import com.rafex.housedb.service.models.ItemMovement;
import com.rafex.housedb.service.models.LocationInventoryItem;
import com.rafex.housedb.service.models.NearbyInventoryItem;

public interface ItemFinderService {

    List<HouseItem> searchInventoryItems(UUID userId, String text, UUID houseId, UUID houseLocationLeafId,
            Integer limit) throws SQLException;

    ItemMovement moveInventoryItem(UUID inventoryItemId, UUID toHouseLocationLeafId, String movedBy,
            String movementReason, String notes) throws SQLException;

    InventoryCreateResult createInventoryItem(UUID userId, UUID objectId, String nickname, String serialNumber,
            String conditionStatus, String metadataJson, UUID houseLocationLeafId, String movedBy, String notes)
            throws SQLException;

    UUID upsertHouseLocationFromKiwi(UUID houseId, UUID kiwiLocationId, UUID kiwiParentLocationId,
            UUID parentHouseLocationId, String locationKind, String name, Boolean isLeaf, String path,
            String referenceCode, String notes, Double latitude, Double longitude, Boolean enabled) throws SQLException;

    UUID findKiwiLocationIdByHouseLocationId(UUID houseLocationId) throws SQLException;

    UUID upsertObjectFromKiwi(UUID kiwiObjectId, String name, String description, String category, String bucketImage,
            Boolean enabled) throws SQLException;

    List<LocationInventoryItem> listInventoryByLocation(UUID userId, UUID houseId, UUID houseLocationId,
            Boolean includeDescendants, Integer limit) throws SQLException;

    List<InventoryTimelineEvent> inventoryItemTimeline(UUID inventoryItemId, Integer limit) throws SQLException;

    FavoriteState setFavoriteItem(UUID userId, UUID inventoryItemId, Boolean isFavorite, String note)
            throws SQLException;

    List<NearbyInventoryItem> searchInventoryItemsNearPoint(UUID userId, double latitude, double longitude,
            Double radiusMeters, Integer limit) throws SQLException;

    InventoryItemDetail getInventoryItemDetail(UUID inventoryItemId) throws SQLException;
}
