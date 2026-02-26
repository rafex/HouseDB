package com.rafex.housedb.services.impl;

import com.rafex.housedb.repository.UserRepository;
import com.rafex.housedb.security.PasswordHasherPBKDF2;
import com.rafex.housedb.services.AuthService;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AuthServiceImpl implements AuthService {

    private static final Logger LOG = Logger.getLogger(AuthServiceImpl.class.getName());

    private final UserRepository repository;
    private final PasswordHasherPBKDF2 hasher;

    public AuthServiceImpl(final UserRepository userRepo, final PasswordHasherPBKDF2 hasher) {
        repository = Objects.requireNonNull(userRepo);
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public AuthResult authenticate(final String username, final char[] password) throws Exception {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            return AuthResult.bad("bad_credentials");
        }

        try {
            final var userOpt = repository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return AuthResult.bad("bad_credentials");
            }

            final var user = userOpt.get();
            if (user.status() == null || !"active".equalsIgnoreCase(user.status())) {
                return AuthResult.bad("user_disabled");
            }

            final var ok = hasher.verify(password, user.salt(), user.iterations(), user.passwordHash());
            if (!ok) {
                return AuthResult.bad("bad_credentials");
            }

            final var roles = repository.findRoleNamesByUserId(user.userId());
            return AuthResult.ok(user.userId(), user.username(), roles);

        } catch (final SQLException e) {
            LOG.log(Level.SEVERE, "Error authenticating user " + username, e);
            return AuthResult.bad("error");
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
