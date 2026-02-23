package com.rafex.housedb.dtos;

import java.util.UUID;

public record CreateInventoryItemRequest(
        UUID userId,
        UUID objectId,
        String nickname,
        String serialNumber,
        String conditionStatus,
        UUID houseLocationLeafId,
        String movedBy,
        String notes
) {
}
