package com.rafex.housedb.services;

import com.rafex.housedb.service.models.HouseCreateResult;
import com.rafex.housedb.service.models.HouseMember;
import com.rafex.housedb.service.models.HouseSummary;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface HouseService {

    HouseCreateResult createHouse(UUID ownerUserId, String name, String description, String street, String numberExt,
            String numberInt, String neighborhood, String city, String state, String zipCode, String country,
            Double latitude, Double longitude, String urlMap) throws SQLException;

    HouseMember upsertHouseMember(UUID houseId, UUID userId, String role, Boolean enabled) throws SQLException;

    List<HouseSummary> listUserHouses(UUID userId, Boolean includeDisabled, Integer limit) throws SQLException;

    List<HouseMember> listHouseMembers(UUID houseId, Boolean includeDisabled, Integer limit) throws SQLException;
}
