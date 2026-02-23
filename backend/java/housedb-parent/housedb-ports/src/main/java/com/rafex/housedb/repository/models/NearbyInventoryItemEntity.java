package com.rafex.housedb.repository.models;

import java.util.UUID;

public record NearbyInventoryItemEntity(
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
