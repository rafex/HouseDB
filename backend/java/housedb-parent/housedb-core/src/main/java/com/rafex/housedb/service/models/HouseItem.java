package com.rafex.housedb.service.models;

import java.util.UUID;

public record HouseItem(
        UUID inventoryItemId,
        UUID objectId,
        UUID objectKiwiId,
        String objectName,
        String objectDescription,
        String nickname,
        UUID houseId,
        String houseName,
        UUID houseLocationLeafId,
        String houseLocationPath,
        float rank
) {
}
