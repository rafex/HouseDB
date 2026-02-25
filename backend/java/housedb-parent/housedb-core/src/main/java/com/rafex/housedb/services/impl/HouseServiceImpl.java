package com.rafex.housedb.services.impl;

import com.rafex.housedb.repository.HouseManagementRepository;
import com.rafex.housedb.service.models.HouseCreateResult;
import com.rafex.housedb.service.models.HouseMember;
import com.rafex.housedb.service.models.HouseSummary;
import com.rafex.housedb.services.HouseService;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class HouseServiceImpl implements HouseService {

    private static final Set<String> ALLOWED_ROLES = Set.of("owner", "family", "guest");
    private static final int DEFAULT_LIMIT = 50;

    private final HouseManagementRepository repository;
    private final HouseModelMapper mapper;

    public HouseServiceImpl(final HouseManagementRepository repository) {
        this(repository, new HouseModelMapper());
    }

    HouseServiceImpl(final HouseManagementRepository repository, final HouseModelMapper mapper) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public HouseCreateResult createHouse(final UUID ownerUserId, final String name, final String description,
            final String street, final String numberExt, final String numberInt, final String neighborhood,
            final String city, final String state, final String zipCode, final String country, final Double latitude,
            final Double longitude, final String urlMap) throws SQLException {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("ownerUserId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("latitude and longitude must be provided together");
        }

        return mapper.toHouseCreateResult(repository.createHouse(ownerUserId, name, description, street, numberExt,
                numberInt, neighborhood, city, state, zipCode, country, latitude, longitude, urlMap));
    }

    @Override
    public HouseMember upsertHouseMember(final UUID houseId, final UUID userId, final String role,
            final Boolean enabled) throws SQLException {
        if (houseId == null || userId == null) {
            throw new IllegalArgumentException("houseId and userId are required");
        }

        final String safeRole = role == null || role.isBlank() ? "guest" : role.trim().toLowerCase();
        if (!ALLOWED_ROLES.contains(safeRole)) {
            throw new IllegalArgumentException("role must be one of: owner, family, guest");
        }

        return mapper.toHouseMember(repository.upsertHouseMember(houseId, userId, safeRole,
                enabled == null ? Boolean.TRUE : enabled));
    }

    @Override
    public List<HouseSummary> listUserHouses(final UUID userId, final Boolean includeDisabled, final Integer limit)
            throws SQLException {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return mapper.toHouseSummaries(repository.listUserHouses(userId,
                includeDisabled == null ? Boolean.FALSE : includeDisabled, normalizeLimit(limit, 200)));
    }

    @Override
    public List<HouseMember> listHouseMembers(final UUID houseId, final Boolean includeDisabled, final Integer limit)
            throws SQLException {
        if (houseId == null) {
            throw new IllegalArgumentException("houseId is required");
        }
        return mapper.toHouseMembers(repository.listHouseMembers(houseId,
                includeDisabled == null ? Boolean.FALSE : includeDisabled, normalizeLimit(limit, 200)));
    }

    private static int normalizeLimit(final Integer value, final int max) {
        if (value == null || value < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, max);
    }
}
