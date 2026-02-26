package com.rafex.housedb.handlers.users;

import com.rafex.housedb.dtos.CreateUserRequest;
import com.rafex.housedb.handlers.AuthzSupport;
import com.rafex.housedb.http.HttpUtil;
import com.rafex.housedb.json.JsonUtil;
import com.rafex.housedb.repository.UserRepository;
import com.rafex.housedb.security.PasswordHasherPBKDF2;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

final class CreateUserHandler {

    private static final Logger LOG = Logger.getLogger(CreateUserHandler.class.getName());

    private final UserRepository userRepository;
    private final PasswordHasherPBKDF2 hasher;
    private final int saltBytes;
    private final int pbkdf2Iterations;

    CreateUserHandler(final UserRepository userRepository, final PasswordHasherPBKDF2 hasher) {
        this.userRepository = userRepository;
        this.hasher = hasher;
        saltBytes = Math.max(16, parseIntEnv("AUTH_SALT_BYTES", 16));
        pbkdf2Iterations = Math.max(10000, parseIntEnv("AUTH_PBKDF2_ITERATIONS", 120000));
    }

    boolean handle(final Request request, final Response response, final Callback callback) {
        return UsersEndpointSupport.execute(LOG, response, callback, () -> {
            AuthzSupport.requireAppOrAdmin(request);

            final var body = JsonUtil.MAPPER.readValue(Request.asInputStream(request), CreateUserRequest.class);
            final var username = requireText(body.username(), "username");
            final var password = requireText(body.password(), "password");
            final var userId = body.userId() != null ? body.userId() : UUID.randomUUID();

            final var passChars = password.toCharArray();
            try {
                final var salt = new byte[saltBytes];
                DbRandomHolder.fill(salt);

                final var hashResult = hasher.hash(passChars, salt, pbkdf2Iterations);
                userRepository.createUser(userId, username, hashResult.hash(), hashResult.salt(), hashResult.iterations());
            } finally {
                Arrays.fill(passChars, '\0');
            }
            HttpUtil.ok(response, callback, Map.of("userId", userId, "username", username));
        });
    }

    private static int parseIntEnv(final String key, final int def) {
        final var raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return def;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException e) {
            return def;
        }
    }

    private static String requireText(final String value, final String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static final class DbRandomHolder {
        private static final java.security.SecureRandom RNG = new java.security.SecureRandom();

        private DbRandomHolder() {
        }

        static void fill(final byte[] out) {
            RNG.nextBytes(out);
        }
    }
}
