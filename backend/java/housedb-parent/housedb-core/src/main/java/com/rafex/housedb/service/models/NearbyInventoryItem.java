package com.rafex.housedb.service.models;

import java.util.UUID;

public record NearbyInventoryItem(
        UUID inventoryItemId,
        UUID objectId,
        String objectName,
        UUID houseId,
        String houseName,
        UUID houseLocationLeafId,
        String houseLocationPath,
        double distanceMeters
) {
}
