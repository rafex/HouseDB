package com.rafex.housedb.handlers.houses;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

final class HouseRequestParsers {

    private HouseRequestParsers() {
    }

    static MultiMap<String> parseQuery(final String rawQuery) {
        final var params = new MultiMap<String>();
        if (rawQuery != null && !rawQuery.isBlank()) {
            UrlEncoded.decodeTo(rawQuery, params, StandardCharsets.UTF_8);
        }
        return params;
    }

    static String getValue(final MultiMap<String> params, final String key) {
        final var value = params.getValue(key);
        if (value == null) {
            return null;
        }
        final var trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static UUID parseRequiredUuid(final MultiMap<String> params, final String key) {
        final var value = getValue(params, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        try {
            return UUID.fromString(value);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(key + " must be a valid UUID");
        }
    }

    static Integer parseOptionalInt(final MultiMap<String> params, final String key) {
        final var value = getValue(params, key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
    }

    static Boolean parseOptionalBoolean(final MultiMap<String> params, final String key) {
        final var value = getValue(params, key);
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException(key + " must be true or false");
    }

    static UUID extractHouseIdFromMembersPath(final String path) {
        final String prefix = "/houses/";
        final String suffix = "/members";
        return extractHouseId(path, prefix, suffix);
    }

    static UUID extractHouseIdFromLocationsPath(final String path) {
        final String prefix = "/houses/";
        final String suffix = "/locations";
        return extractHouseId(path, prefix, suffix);
    }

    private static UUID extractHouseId(final String path, final String prefix, final String suffix) {
        final int start = prefix.length();
        final int end = path.length() - suffix.length();
        if (end <= start) {
            throw new IllegalArgumentException("invalid house path");
        }
        final String raw = path.substring(start, end);
        if (raw.endsWith("/")) {
            throw new IllegalArgumentException("invalid house path");
        }
        try {
            return UUID.fromString(raw);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("houseId in path must be a valid UUID");
        }
    }
}
