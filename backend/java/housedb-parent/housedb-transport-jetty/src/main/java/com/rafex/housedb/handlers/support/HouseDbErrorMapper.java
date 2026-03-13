package com.rafex.housedb.handlers.support;

import java.sql.SQLException;

import dev.rafex.ether.http.core.DefaultErrorMapper;
import dev.rafex.ether.http.core.ErrorMapper;
import dev.rafex.ether.http.core.HttpError;

public final class HouseDbErrorMapper implements ErrorMapper {

    private static final ErrorMapper DEFAULT = new DefaultErrorMapper();

    @Override
    public HttpError map(final Throwable error) {
        if (error instanceof SecurityException) {
            return new HttpError(403, "forbidden", safeMessage(error.getMessage(), "forbidden"));
        }
        if (error instanceof SQLException || error.getCause() instanceof SQLException) {
            return new HttpError(500, "internal_server_error", "database error");
        }
        final var mapped = DEFAULT.map(error);
        if (mapped.status() == 500) {
            return new HttpError(500, "internal_server_error", "internal error");
        }
        return mapped;
    }

    private static String safeMessage(final String message, final String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }
}
