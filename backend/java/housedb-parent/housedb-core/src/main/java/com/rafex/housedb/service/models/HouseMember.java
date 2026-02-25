package com.rafex.housedb.service.models;

import java.util.UUID;

public record HouseMember(UUID houseMemberId, UUID houseId, UUID userId, String role, boolean enabled) {
}
