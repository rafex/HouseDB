package com.rafex.housedb.dtos;

import java.util.UUID;

public record CreateUserRequest(
        UUID userId,
        String username,
        String password
) {
}
