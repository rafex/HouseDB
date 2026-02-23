package com.rafex.housedb.repository.models;

import java.time.Instant;
import java.util.UUID;

public record ItemMovementEntity(
        UUID itemMovementId,
        UUID inventoryItemId,
        UUID fromHouseLocationLeafId,
        UUID toHouseLocationLeafId,
        Instant movedAt
) {
}
