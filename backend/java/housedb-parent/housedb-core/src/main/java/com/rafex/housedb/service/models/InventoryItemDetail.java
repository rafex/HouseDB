package com.rafex.housedb.service.models;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemDetail(
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
