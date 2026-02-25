package com.rafex.housedb.service.models;

import java.util.UUID;

public record HouseCreateResult(UUID houseId, UUID houseMemberId) {
}
