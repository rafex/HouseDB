package com.rafex.housedb.repository.models;

import java.util.UUID;

public record HouseSummaryEntity(
        UUID houseId,
        String name,
        String description,
        String city,
        String state,
        String country,
        String role,
        boolean memberEnabled,
        boolean houseEnabled
) {
}
