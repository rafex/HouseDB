package com.rafex.housedb.dtos;

public record SetFavoriteRequest(
        Boolean isFavorite,
        String note
) {
}
