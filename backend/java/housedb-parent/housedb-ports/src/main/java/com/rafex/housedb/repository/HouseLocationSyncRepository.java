package com.rafex.housedb.repository;

import java.sql.SQLException;
import java.util.UUID;

public interface HouseLocationSyncRepository {

    UUID upsertHouseLocationFromKiwi(UUID houseId, UUID kiwiLocationId, UUID kiwiParentLocationId,
            UUID parentHouseLocationId, String locationKind, String name, Boolean isLeaf, String path,
            String referenceCode, String notes, Double latitude, Double longitude, Boolean enabled) throws SQLException;
}
