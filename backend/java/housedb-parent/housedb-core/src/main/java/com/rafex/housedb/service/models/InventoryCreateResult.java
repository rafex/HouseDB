package com.rafex.housedb.service.models;

import java.util.UUID;

public record InventoryCreateResult(UUID inventoryItemId, UUID itemMovementId) {
}
