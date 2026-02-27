package com.rafex.housedb.dtos;

import java.util.UUID;

public record CreateHouseLocationRequest(
        String name,
        UUID parentHouseLocationId,
        String locationKind,
        Boolean isLeaf,
        String path,
        String referenceCode,
        String notes,
        Double latitude,
        Double longitude,
        Boolean enabled
) {
}
