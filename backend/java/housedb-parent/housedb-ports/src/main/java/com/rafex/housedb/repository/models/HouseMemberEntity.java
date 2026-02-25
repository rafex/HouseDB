package com.rafex.housedb.repository.models;

import java.util.UUID;

public record HouseMemberEntity(UUID houseMemberId, UUID houseId, UUID userId, String role, boolean enabled) {
}
