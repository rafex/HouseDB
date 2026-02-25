package com.rafex.housedb.dtos;

import java.util.UUID;

public record CreateHouseRequest(
        UUID ownerUserId,
        String name,
        String description,
        String street,
        String numberExt,
        String numberInt,
        String neighborhood,
        String city,
        String state,
        String zipCode,
        String country,
        Double latitude,
        Double longitude,
        String urlMap
) {
}
