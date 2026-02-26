package com.rafex.housedb.repository;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    void createUser(UUID userId, String username, byte[] passwordHash, byte[] salt, int iterations)
            throws SQLException;

    Optional<UserRow> findByUsername(String username) throws SQLException;

    List<String> findRoleNamesByUserId(UUID userId) throws SQLException;

    Optional<UserWithRoles> findByUsernameWithRoles(String username) throws SQLException;

    int countUsers() throws SQLException;

    record UserRow(UUID userId, String username, byte[] passwordHash, byte[] salt, int iterations, String status,
            Instant createdAt, Instant updatedAt) {
    }

    record UserWithRoles(UserRow user, List<String> roles) {
    }
}
