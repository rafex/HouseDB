package com.rafex.housedb.service.models;

import java.time.Instant;
import java.util.UUID;

public record ItemMovement(
        UUID itemMovementId,
        UUID inventoryItemId,
        UUID fromHouseLocationLeafId,
        UUID toHouseLocationLeafId,
        Instant movedAt
) {
}
