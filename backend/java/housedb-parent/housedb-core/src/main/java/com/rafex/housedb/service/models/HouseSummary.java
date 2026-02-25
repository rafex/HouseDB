package com.rafex.housedb.service.models;

import java.util.UUID;

public record HouseSummary(
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
