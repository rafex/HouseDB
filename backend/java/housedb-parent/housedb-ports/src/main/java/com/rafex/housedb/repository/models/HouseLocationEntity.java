package com.rafex.housedb.repository.models;

import java.util.UUID;

public record HouseLocationEntity(
        UUID houseLocationId,
        UUID houseId,
        UUID kiwiLocationId,
        UUID kiwiParentLocationId,
        UUID parentHouseLocationId,
        String locationKind,
        String name,
        String path,
        int levelDepth,
        Double latitude,
        Double longitude,
        String referenceCode,
        boolean isLeaf,
        String notes,
        boolean enabled
) {
}
