package com.rafex.housedb.dtos;

import java.util.UUID;

public record MoveInventoryItemRequest(
        UUID toHouseLocationLeafId,
        String movedBy,
        String movementReason,
        String notes
) {
}
