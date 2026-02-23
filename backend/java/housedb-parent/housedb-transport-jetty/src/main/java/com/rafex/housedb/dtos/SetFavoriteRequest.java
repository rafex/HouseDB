package com.rafex.housedb.dtos;

import java.util.UUID;

public record SetFavoriteRequest(
        UUID userId,
        Boolean isFavorite,
        String note
) {
}
