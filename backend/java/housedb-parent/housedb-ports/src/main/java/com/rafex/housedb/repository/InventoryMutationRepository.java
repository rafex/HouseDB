package com.rafex.housedb.repository;

import com.rafex.housedb.repository.models.FavoriteStateEntity;
import com.rafex.housedb.repository.models.InventoryCreateResultEntity;
import com.rafex.housedb.repository.models.ItemMovementEntity;

import java.sql.SQLException;
import java.util.UUID;

public interface InventoryMutationRepository {

    ItemMovementEntity moveInventoryItem(UUID inventoryItemId, UUID toHouseLocationLeafId, String movedBy,
            String movementReason, String notes) throws SQLException;

    InventoryCreateResultEntity createInventoryItem(UUID userId, UUID objectId, String nickname, String serialNumber,
            String conditionStatus, UUID houseLocationLeafId, String movedBy, String notes) throws SQLException;

    FavoriteStateEntity setFavoriteItem(UUID userId, UUID inventoryItemId, Boolean isFavorite, String note)
            throws SQLException;
}
