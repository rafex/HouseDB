package com.rafex.housedb.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.rafex.housedb.repository.models.HouseItemEntity;
import com.rafex.housedb.repository.models.InventoryItemDetailEntity;
import com.rafex.housedb.repository.models.InventoryTimelineEventEntity;
import com.rafex.housedb.repository.models.LocationInventoryItemEntity;
import com.rafex.housedb.repository.models.NearbyInventoryItemEntity;

public interface InventorySearchRepository {

    List<HouseItemEntity> searchInventoryItems(UUID userId, String text, UUID houseId, UUID houseLocationLeafId,
            int limit) throws SQLException;

    List<LocationInventoryItemEntity> listInventoryByLocation(UUID userId, UUID houseId, UUID houseLocationId,
            Boolean includeDescendants, int limit) throws SQLException;

    List<InventoryTimelineEventEntity> inventoryItemTimeline(UUID inventoryItemId, int limit) throws SQLException;

    List<NearbyInventoryItemEntity> searchInventoryItemsNearPoint(UUID userId, double latitude, double longitude,
            double radiusMeters, int limit) throws SQLException;

    InventoryItemDetailEntity getInventoryItemDetail(UUID inventoryItemId) throws SQLException;
}
