package com.rafex.housedb.repository;

import com.rafex.housedb.repository.models.HouseCreateResultEntity;
import com.rafex.housedb.repository.models.HouseMemberEntity;
import com.rafex.housedb.repository.models.HouseSummaryEntity;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface HouseManagementRepository {

    HouseCreateResultEntity createHouse(UUID ownerUserId, String name, String description, String street,
            String numberExt, String numberInt, String neighborhood, String city, String state, String zipCode,
            String country, Double latitude, Double longitude, String urlMap) throws SQLException;

    HouseMemberEntity upsertHouseMember(UUID houseId, UUID userId, String role, Boolean enabled) throws SQLException;

    List<HouseSummaryEntity> listUserHouses(UUID userId, Boolean includeDisabled, int limit) throws SQLException;

    List<HouseMemberEntity> listHouseMembers(UUID houseId, Boolean includeDisabled, int limit) throws SQLException;
}
