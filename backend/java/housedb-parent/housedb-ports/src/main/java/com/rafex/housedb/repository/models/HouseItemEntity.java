package com.rafex.housedb.repository.models;

import java.util.UUID;

public record HouseItemEntity(
        UUID inventoryItemId,
        UUID objectId,
        UUID objectKiwiId,
        String objectName,
        String objectDescription,
        String objectCategory,
        String nickname,
        UUID houseId,
        String houseName,
        UUID houseLocationLeafId,
        String houseLocationPath,
        float rank
) {
}
