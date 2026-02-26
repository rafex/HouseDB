package com.rafex.housedb.services.impl;

import com.rafex.housedb.repository.models.FavoriteStateEntity;
import com.rafex.housedb.repository.models.HouseItemEntity;
import com.rafex.housedb.repository.models.InventoryCreateResultEntity;
import com.rafex.housedb.repository.models.InventoryItemDetailEntity;
import com.rafex.housedb.repository.models.InventoryTimelineEventEntity;
import com.rafex.housedb.repository.models.ItemMovementEntity;
import com.rafex.housedb.repository.models.LocationInventoryItemEntity;
import com.rafex.housedb.repository.models.NearbyInventoryItemEntity;
import com.rafex.housedb.service.models.FavoriteState;
import com.rafex.housedb.service.models.HouseItem;
import com.rafex.housedb.service.models.InventoryCreateResult;
import com.rafex.housedb.service.models.InventoryItemDetail;
import com.rafex.housedb.service.models.InventoryTimelineEvent;
import com.rafex.housedb.service.models.ItemMovement;
import com.rafex.housedb.service.models.LocationInventoryItem;
import com.rafex.housedb.service.models.NearbyInventoryItem;

import java.util.List;

final class ItemModelMapper {

    List<HouseItem> toHouseItems(final List<HouseItemEntity> source) {
        return source.stream().map(this::toHouseItem).toList();
    }

    HouseItem toHouseItem(final HouseItemEntity source) {
        return new HouseItem(source.inventoryItemId(), source.objectId(), source.objectKiwiId(), source.objectName(),
                source.objectDescription(), source.objectCategory(), source.nickname(), source.houseId(), source.houseName(),
                source.houseLocationLeafId(), source.houseLocationPath(), source.rank());
    }

    ItemMovement toItemMovement(final ItemMovementEntity source) {
        return new ItemMovement(source.itemMovementId(), source.inventoryItemId(), source.fromHouseLocationLeafId(),
                source.toHouseLocationLeafId(), source.movedAt());
    }

    InventoryCreateResult toInventoryCreateResult(final InventoryCreateResultEntity source) {
        return new InventoryCreateResult(source.inventoryItemId(), source.itemMovementId());
    }

    List<LocationInventoryItem> toLocationInventoryItems(final List<LocationInventoryItemEntity> source) {
        return source.stream().map(this::toLocationInventoryItem).toList();
    }

    LocationInventoryItem toLocationInventoryItem(final LocationInventoryItemEntity source) {
        return new LocationInventoryItem(source.inventoryItemId(), source.objectId(), source.objectName(),
                source.nickname(), source.houseId(), source.houseName(), source.houseLocationLeafId(),
                source.houseLocationPath(), source.assignedAt());
    }

    List<InventoryTimelineEvent> toInventoryTimelineEvents(final List<InventoryTimelineEventEntity> source) {
        return source.stream().map(this::toInventoryTimelineEvent).toList();
    }

    InventoryTimelineEvent toInventoryTimelineEvent(final InventoryTimelineEventEntity source) {
        return new InventoryTimelineEvent(source.itemMovementId(), source.inventoryItemId(), source.movementReason(),
                source.movedBy(), source.movedAt(), source.fromHouseLocationLeafId(), source.fromHouseLocationPath(),
                source.toHouseLocationLeafId(), source.toHouseLocationPath(), source.notes());
    }

    FavoriteState toFavoriteState(final FavoriteStateEntity source) {
        return new FavoriteState(source.userId(), source.inventoryItemId(), source.isFavorite());
    }

    List<NearbyInventoryItem> toNearbyInventoryItems(final List<NearbyInventoryItemEntity> source) {
        return source.stream().map(this::toNearbyInventoryItem).toList();
    }

    NearbyInventoryItem toNearbyInventoryItem(final NearbyInventoryItemEntity source) {
        return new NearbyInventoryItem(source.inventoryItemId(), source.objectId(), source.objectName(),
                source.houseId(), source.houseName(), source.houseLocationLeafId(), source.houseLocationPath(),
                source.distanceMeters());
    }

    InventoryItemDetail toInventoryItemDetail(final InventoryItemDetailEntity source) {
        if (source == null) {
            return null;
        }
        return new InventoryItemDetail(source.inventoryItemId(), source.userId(), source.objectId(), source.objectKiwiId(),
                source.nickname(), source.serialNumber(), source.conditionStatus(), source.inventoryItemEnabled(),
                source.houseId(), source.houseName(), source.houseLocationLeafId(), source.houseLocationPath(),
                source.assignedAt(), source.createdAt(), source.updatedAt());
    }
}
