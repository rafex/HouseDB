package com.rafex.housedb.repository.impl;

import com.rafex.housedb.repository.HouseLocationSyncRepository;
import com.rafex.housedb.repository.InventoryMutationRepository;
import com.rafex.housedb.repository.InventorySearchRepository;
import com.rafex.housedb.repository.models.FavoriteStateEntity;
import com.rafex.housedb.repository.models.HouseItemEntity;
import com.rafex.housedb.repository.models.InventoryCreateResultEntity;
import com.rafex.housedb.repository.models.InventoryItemDetailEntity;
import com.rafex.housedb.repository.models.InventoryTimelineEventEntity;
import com.rafex.housedb.repository.models.ItemMovementEntity;
import com.rafex.housedb.repository.models.LocationInventoryItemEntity;
import com.rafex.housedb.repository.models.NearbyInventoryItemEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

public final class ItemRepositoryImpl
        implements InventorySearchRepository, InventoryMutationRepository, HouseLocationSyncRepository {

    private static final String SQL_SEARCH = """
            SELECT inventory_item_id,
                   object_id,
                   object_kiwi_id,
                   object_name,
                   object_description,
                   object_category,
                   nickname,
                   house_id,
                   house_name,
                   house_location_leaf_id,
                   house_location_path,
                   rank
              FROM api_search_inventory_items(?, ?, ?, ?, ?)
            """;

    private static final String SQL_MOVE = """
            SELECT item_movement_id,
                   inventory_item_id,
                   from_house_location_leaf_id,
                   to_house_location_leaf_id,
                   moved_at
              FROM api_move_inventory_item(?, ?, ?, ?, ?, now())
            """;

    private static final String SQL_CREATE = """
            SELECT inventory_item_id,
                   item_movement_id
              FROM api_create_inventory_item(?, ?, ?, ?, ?::inventory_item_status, ?, ?, ?)
            """;

    private static final String SQL_UPSERT_KIWI_LOCATION = """
            SELECT api_upsert_house_location_from_kiwi(
              ?, ?, ?, ?, ?::location_kind, ?, ?, ?, ?, ?, ?, ?, ?
            ) AS house_location_id
            """;

    private static final String SQL_LIST_BY_LOCATION = """
            SELECT inventory_item_id,
                   object_id,
                   object_name,
                   nickname,
                   house_id,
                   house_name,
                   house_location_leaf_id,
                   house_location_path,
                   assigned_at
              FROM api_list_inventory_by_location(?, ?, ?, ?, ?)
            """;

    private static final String SQL_TIMELINE = """
            SELECT item_movement_id,
                   inventory_item_id,
                   movement_reason,
                   moved_by,
                   moved_at,
                   from_house_location_leaf_id,
                   from_house_location_path,
                   to_house_location_leaf_id,
                   to_house_location_path,
                   notes
              FROM api_inventory_item_timeline(?, ?)
            """;

    private static final String SQL_SET_FAVORITE = """
            SELECT user_id,
                   inventory_item_id,
                   is_favorite
              FROM api_set_favorite_item(?, ?, ?, ?)
            """;

    private static final String SQL_NEARBY = """
            SELECT inventory_item_id,
                   object_id,
                   object_name,
                   house_id,
                   house_name,
                   house_location_leaf_id,
                   house_location_path,
                   distance_meters
              FROM api_search_inventory_items_near_point(?, ?, ?, ?, ?)
            """;
    private static final String SQL_ITEM_DETAIL = """
            SELECT inventory_item_id,
                   user_id,
                   object_id,
                   object_kiwi_id,
                   nickname,
                   serial_number,
                   condition_status,
                   inventory_item_enabled,
                   house_id,
                   house_name,
                   house_location_leaf_id,
                   house_location_path,
                   assigned_at,
                   created_at,
                   updated_at
              FROM api_get_inventory_item_detail(?)
            """;

    private final DataSource dataSource;

    public ItemRepositoryImpl(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public List<HouseItemEntity> searchInventoryItems(final UUID userId, final String text, final UUID houseId,
            final UUID houseLocationLeafId, final int limit) throws SQLException {
        final var result = new ArrayList<HouseItemEntity>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_SEARCH)) {
            ps.setObject(1, userId);
            ps.setString(2, text);
            ps.setObject(3, houseId);
            ps.setObject(4, houseLocationLeafId);
            ps.setInt(5, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new HouseItemEntity(
                            rs.getObject("inventory_item_id", UUID.class),
                            rs.getObject("object_id", UUID.class),
                            rs.getObject("object_kiwi_id", UUID.class),
                            rs.getString("object_name"),
                            rs.getString("object_description"),
                            rs.getString("object_category"),
                            rs.getString("nickname"),
                            rs.getObject("house_id", UUID.class),
                            rs.getString("house_name"),
                            rs.getObject("house_location_leaf_id", UUID.class),
                            rs.getString("house_location_path"),
                            rs.getFloat("rank")));
                }
            }
        }

        return result;
    }

    @Override
    public ItemMovementEntity moveInventoryItem(final UUID inventoryItemId, final UUID toHouseLocationLeafId,
            final String movedBy, final String movementReason, final String notes) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_MOVE)) {
            ps.setObject(1, inventoryItemId);
            ps.setObject(2, toHouseLocationLeafId);
            ps.setString(3, movedBy);
            ps.setString(4, movementReason);
            ps.setString(5, notes);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("api_move_inventory_item returned no rows");
                }

                return new ItemMovementEntity(
                        rs.getObject("item_movement_id", UUID.class),
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getObject("from_house_location_leaf_id", UUID.class),
                        rs.getObject("to_house_location_leaf_id", UUID.class),
                        asInstant(rs, "moved_at"));
            }
        }
    }

    @Override
    public InventoryCreateResultEntity createInventoryItem(final UUID userId, final UUID objectId,
            final String nickname, final String serialNumber, final String conditionStatus,
            final UUID houseLocationLeafId, final String movedBy, final String notes) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_CREATE)) {
            ps.setObject(1, userId);
            ps.setObject(2, objectId);
            ps.setString(3, nickname);
            ps.setString(4, serialNumber);
            ps.setString(5, conditionStatus);
            ps.setObject(6, houseLocationLeafId);
            ps.setString(7, movedBy);
            ps.setString(8, notes);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("api_create_inventory_item returned no rows");
                }

                return new InventoryCreateResultEntity(
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getObject("item_movement_id", UUID.class));
            }
        }
    }

    @Override
    public UUID upsertHouseLocationFromKiwi(final UUID houseId, final UUID kiwiLocationId,
            final UUID kiwiParentLocationId, final UUID parentHouseLocationId, final String locationKind,
            final String name, final Boolean isLeaf, final String path, final String referenceCode, final String notes,
            final Double latitude, final Double longitude, final Boolean enabled) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_UPSERT_KIWI_LOCATION)) {
            ps.setObject(1, houseId);
            ps.setObject(2, kiwiLocationId);
            ps.setObject(3, kiwiParentLocationId);
            ps.setObject(4, parentHouseLocationId);
            ps.setString(5, locationKind);
            ps.setString(6, name);
            ps.setObject(7, isLeaf);
            ps.setString(8, path);
            ps.setString(9, referenceCode);
            ps.setString(10, notes);
            ps.setObject(11, latitude);
            ps.setObject(12, longitude);
            ps.setObject(13, enabled);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("api_upsert_house_location_from_kiwi returned no rows");
                }
                return rs.getObject("house_location_id", UUID.class);
            }
        }
    }

    @Override
    public List<LocationInventoryItemEntity> listInventoryByLocation(final UUID userId, final UUID houseId,
            final UUID houseLocationId, final Boolean includeDescendants, final int limit) throws SQLException {
        final var result = new ArrayList<LocationInventoryItemEntity>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_LIST_BY_LOCATION)) {
            ps.setObject(1, userId);
            ps.setObject(2, houseId);
            ps.setObject(3, houseLocationId);
            ps.setObject(4, includeDescendants);
            ps.setInt(5, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new LocationInventoryItemEntity(
                            rs.getObject("inventory_item_id", UUID.class),
                            rs.getObject("object_id", UUID.class),
                            rs.getString("object_name"),
                            rs.getString("nickname"),
                            rs.getObject("house_id", UUID.class),
                            rs.getString("house_name"),
                            rs.getObject("house_location_leaf_id", UUID.class),
                            rs.getString("house_location_path"),
                            asInstant(rs, "assigned_at")));
                }
            }
        }

        return result;
    }

    @Override
    public List<InventoryTimelineEventEntity> inventoryItemTimeline(final UUID inventoryItemId, final int limit)
            throws SQLException {
        final var result = new ArrayList<InventoryTimelineEventEntity>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_TIMELINE)) {
            ps.setObject(1, inventoryItemId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new InventoryTimelineEventEntity(
                            rs.getObject("item_movement_id", UUID.class),
                            rs.getObject("inventory_item_id", UUID.class),
                            rs.getString("movement_reason"),
                            rs.getString("moved_by"),
                            asInstant(rs, "moved_at"),
                            rs.getObject("from_house_location_leaf_id", UUID.class),
                            rs.getString("from_house_location_path"),
                            rs.getObject("to_house_location_leaf_id", UUID.class),
                            rs.getString("to_house_location_path"),
                            rs.getString("notes")));
                }
            }
        }

        return result;
    }

    @Override
    public FavoriteStateEntity setFavoriteItem(final UUID userId, final UUID inventoryItemId,
            final Boolean isFavorite, final String note) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_SET_FAVORITE)) {
            ps.setObject(1, userId);
            ps.setObject(2, inventoryItemId);
            ps.setObject(3, isFavorite);
            ps.setString(4, note);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("api_set_favorite_item returned no rows");
                }

                return new FavoriteStateEntity(
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getBoolean("is_favorite"));
            }
        }
    }

    @Override
    public List<NearbyInventoryItemEntity> searchInventoryItemsNearPoint(final UUID userId, final double latitude,
            final double longitude, final double radiusMeters, final int limit) throws SQLException {
        final var result = new ArrayList<NearbyInventoryItemEntity>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_NEARBY)) {
            ps.setObject(1, userId);
            ps.setDouble(2, latitude);
            ps.setDouble(3, longitude);
            ps.setDouble(4, radiusMeters);
            ps.setInt(5, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new NearbyInventoryItemEntity(
                            rs.getObject("inventory_item_id", UUID.class),
                            rs.getObject("object_id", UUID.class),
                            rs.getString("object_name"),
                            rs.getObject("house_id", UUID.class),
                            rs.getString("house_name"),
                            rs.getObject("house_location_leaf_id", UUID.class),
                            rs.getString("house_location_path"),
                            rs.getDouble("distance_meters")));
                }
            }
        }

        return result;
    }

    @Override
    public InventoryItemDetailEntity getInventoryItemDetail(final UUID inventoryItemId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_ITEM_DETAIL)) {
            ps.setObject(1, inventoryItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new InventoryItemDetailEntity(
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getObject("object_id", UUID.class),
                        rs.getObject("object_kiwi_id", UUID.class),
                        rs.getString("nickname"),
                        rs.getString("serial_number"),
                        rs.getString("condition_status"),
                        rs.getBoolean("inventory_item_enabled"),
                        rs.getObject("house_id", UUID.class),
                        rs.getString("house_name"),
                        rs.getObject("house_location_leaf_id", UUID.class),
                        rs.getString("house_location_path"),
                        asInstant(rs, "assigned_at"),
                        asInstant(rs, "created_at"),
                        asInstant(rs, "updated_at"));
            }
        }
    }

    private static Instant asInstant(final ResultSet rs, final String column) throws SQLException {
        final Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }
}
