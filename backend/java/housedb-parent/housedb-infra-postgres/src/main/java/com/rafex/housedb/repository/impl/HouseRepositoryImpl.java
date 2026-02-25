package com.rafex.housedb.repository.impl;

import com.rafex.housedb.repository.HouseManagementRepository;
import com.rafex.housedb.repository.models.HouseCreateResultEntity;
import com.rafex.housedb.repository.models.HouseMemberEntity;
import com.rafex.housedb.repository.models.HouseSummaryEntity;

import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

public final class HouseRepositoryImpl implements HouseManagementRepository {

    private static final String SQL_CREATE_HOUSE = """
            SELECT house_id,
                   house_member_id
              FROM api_create_house(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_UPSERT_HOUSE_MEMBER = """
            SELECT house_member_id,
                   house_id,
                   user_id,
                   role,
                   enabled
              FROM api_upsert_house_member(?, ?, ?::house_member_role, ?)
            """;
    private static final String SQL_LIST_USER_HOUSES = """
            SELECT house_id,
                   name,
                   description,
                   city,
                   state,
                   country,
                   role,
                   member_enabled,
                   house_enabled
              FROM api_list_user_houses(?, ?, ?)
            """;
    private static final String SQL_LIST_HOUSE_MEMBERS = """
            SELECT house_member_id,
                   house_id,
                   user_id,
                   role,
                   enabled
              FROM api_list_house_members(?, ?, ?)
            """;

    private final DataSource dataSource;

    public HouseRepositoryImpl(final DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public HouseCreateResultEntity createHouse(final UUID ownerUserId, final String name, final String description,
            final String street, final String numberExt, final String numberInt, final String neighborhood,
            final String city, final String state, final String zipCode, final String country, final Double latitude,
            final Double longitude, final String urlMap) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_CREATE_HOUSE)) {
            ps.setObject(1, ownerUserId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, street);
            ps.setString(5, numberExt);
            ps.setString(6, numberInt);
            ps.setString(7, neighborhood);
            ps.setString(8, city);
            ps.setString(9, state);
            ps.setString(10, zipCode);
            ps.setString(11, country);
            ps.setObject(12, latitude);
            ps.setObject(13, longitude);
            ps.setString(14, urlMap);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("api_create_house returned no rows");
                }

                return new HouseCreateResultEntity(rs.getObject("house_id", UUID.class),
                        rs.getObject("house_member_id", UUID.class));
            }
        }
    }

    @Override
    public HouseMemberEntity upsertHouseMember(final UUID houseId, final UUID userId, final String role,
            final Boolean enabled) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_UPSERT_HOUSE_MEMBER)) {
            ps.setObject(1, houseId);
            ps.setObject(2, userId);
            ps.setString(3, role);
            ps.setObject(4, enabled);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("api_upsert_house_member returned no rows");
                }

                return new HouseMemberEntity(
                        rs.getObject("house_member_id", UUID.class),
                        rs.getObject("house_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getString("role"),
                        rs.getBoolean("enabled"));
            }
        }
    }

    @Override
    public List<HouseSummaryEntity> listUserHouses(final UUID userId, final Boolean includeDisabled, final int limit)
            throws SQLException {
        final var result = new ArrayList<HouseSummaryEntity>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_LIST_USER_HOUSES)) {
            ps.setObject(1, userId);
            ps.setObject(2, includeDisabled);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new HouseSummaryEntity(
                            rs.getObject("house_id", UUID.class),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("city"),
                            rs.getString("state"),
                            rs.getString("country"),
                            rs.getString("role"),
                            rs.getBoolean("member_enabled"),
                            rs.getBoolean("house_enabled")));
                }
            }
        }
        return result;
    }

    @Override
    public List<HouseMemberEntity> listHouseMembers(final UUID houseId, final Boolean includeDisabled, final int limit)
            throws SQLException {
        final var result = new ArrayList<HouseMemberEntity>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement ps = connection.prepareStatement(SQL_LIST_HOUSE_MEMBERS)) {
            ps.setObject(1, houseId);
            ps.setObject(2, includeDisabled);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new HouseMemberEntity(
                            rs.getObject("house_member_id", UUID.class),
                            rs.getObject("house_id", UUID.class),
                            rs.getObject("user_id", UUID.class),
                            rs.getString("role"),
                            rs.getBoolean("enabled")));
                }
            }
        }
        return result;
    }
}
