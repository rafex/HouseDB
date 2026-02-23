package com.rafex.housedb.handlers.items;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

final class ItemRequestParsers {

    private ItemRequestParsers() {
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

    static UUID parseOptionalUuid(final MultiMap<String> params, final String key) {
        final var value = getValue(params, key);
        if (value == null) {
            return null;
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

    static Double parseOptionalDouble(final MultiMap<String> params, final String key) {
        final var value = getValue(params, key);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number");
        }
    }

    static double parseRequiredDouble(final MultiMap<String> params, final String key) {
        final var value = parseOptionalDouble(params, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
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

    static UUID extractInventoryItemId(final String path, final String suffix) {
        final String prefix = "/items/";
        final int start = prefix.length();
        final int end = path.length() - suffix.length();
        if (end <= start) {
            throw new IllegalArgumentException("invalid inventory item path");
        }
        final String raw = path.substring(start, end);
        if (raw.endsWith("/")) {
            throw new IllegalArgumentException("invalid inventory item path");
        }
        try {
            return UUID.fromString(raw);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("inventoryItemId in path must be a valid UUID");
        }
    }
}
