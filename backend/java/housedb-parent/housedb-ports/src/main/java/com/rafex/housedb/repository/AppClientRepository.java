package com.rafex.housedb.repository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppClientRepository {

    void createClient(UUID appClientId, String clientId, String name, byte[] secretHash, byte[] salt, int iterations,
            List<String> roles) throws SQLException;

    Optional<AppClientRow> findByClientId(String clientId) throws SQLException;

    void touchLastUsed(UUID appClientId) throws SQLException;

    record AppClientRow(UUID appClientId, String clientId, String name, byte[] secretHash, byte[] salt,
            int iterations, List<String> roles, String status, Instant lastUsedAt, Instant createdAt,
            Instant updatedAt) {
    }
}
