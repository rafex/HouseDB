package com.rafex.housedb.service.models;

import java.time.Instant;
import java.util.UUID;

public record InventoryTimelineEvent(
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
