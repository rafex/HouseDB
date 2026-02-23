package com.rafex.housedb.service.models;

import java.time.Instant;
import java.util.UUID;

public record LocationInventoryItem(
        UUID inventoryItemId,
        UUID objectId,
        String objectName,
        String nickname,
        UUID houseId,
        String houseName,
        UUID houseLocationLeafId,
        String houseLocationPath,
        Instant assignedAt
) {
}
