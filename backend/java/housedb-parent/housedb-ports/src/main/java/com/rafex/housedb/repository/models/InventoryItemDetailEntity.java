package com.rafex.housedb.repository.models;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemDetailEntity(
        UUID inventoryItemId,
        UUID userId,
        UUID objectId,
        UUID objectKiwiId,
        String nickname,
        String serialNumber,
        String conditionStatus,
        boolean inventoryItemEnabled,
        UUID houseId,
        String houseName,
        UUID houseLocationLeafId,
        String houseLocationPath,
        Instant assignedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
