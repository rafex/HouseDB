package com.rafex.housedb.repository.models;

import java.util.UUID;

public record FavoriteStateEntity(UUID userId, UUID inventoryItemId, boolean isFavorite) {
}
