package com.rafex.housedb.repository.models;

import java.time.Instant;
import java.util.UUID;

public record InventoryTimelineEventEntity(
        UUID itemMovementId,
        UUID inventoryItemId,
        String movementReason,
        String movedBy,
        Instant movedAt,
        UUID fromHouseLocationLeafId,
        String fromHouseLocationPath,
        UUID toHouseLocationLeafId,
        String toHouseLocationPath,
        String notes
) {
}
