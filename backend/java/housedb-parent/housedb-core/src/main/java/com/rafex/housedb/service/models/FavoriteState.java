package com.rafex.housedb.service.models;

import java.util.UUID;

public record FavoriteState(UUID userId, UUID inventoryItemId, boolean isFavorite) {
}
