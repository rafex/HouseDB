package com.rafex.housedb.dtos;

import java.util.UUID;

public record UpsertHouseMemberRequest(
        UUID userId,
        String role,
        Boolean enabled
) {
}
