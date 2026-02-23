package com.rafex.housedb.dtos;

import java.util.UUID;

public record UpsertKiwiLocationRequest(
        UUID houseId,
        UUID kiwiLocationId,
        UUID kiwiParentLocationId,
        UUID parentHouseLocationId,
        String locationKind,
        String name,
        Boolean isLeaf,
        String path,
        String referenceCode,
        String notes,
        Double latitude,
        Double longitude,
        Boolean enabled
) {
}
