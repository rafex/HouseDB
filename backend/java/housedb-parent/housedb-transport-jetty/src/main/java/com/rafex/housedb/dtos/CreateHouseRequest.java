package com.rafex.housedb.dtos;

public record CreateHouseRequest(
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
