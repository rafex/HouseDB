package com.rafex.housedb.dtos;

import java.util.List;
import java.util.UUID;

public record CreateInventoryItemRequest(
        String objectName,
        String objectDescription,
        String objectCategory,
        String objectType,
        List<String> objectTags,
        Object kiwiMetadata,
        Object housedbMetadata,
        String nickname,
        String serialNumber,
        String conditionStatus,
        UUID houseLocationLeafId,
        String movedBy,
        String notes
) {
}
