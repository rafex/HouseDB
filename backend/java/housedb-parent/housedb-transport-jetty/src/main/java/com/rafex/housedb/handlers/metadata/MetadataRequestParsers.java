package com.rafex.housedb.handlers.metadata;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class MetadataRequestParsers {

    private MetadataRequestParsers() {
    }

    static Map<String, String> parseQuery(final String rawQuery) {
        final var result = new LinkedHashMap<String, String>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return result;
        }
        for (final var part : rawQuery.split("&")) {
            if (part == null || part.isBlank()) {
                continue;
            }
            final int idx = part.indexOf('=');
            final String rawKey = idx >= 0 ? part.substring(0, idx) : part;
            final String rawValue = idx >= 0 ? part.substring(idx + 1) : "";
            final String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
            final String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }

    static String parseOptionalString(final Map<String, String> query, final String name) {
        final var raw = query.get(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    static Boolean parseOptionalBoolean(final Map<String, String> query, final String name) {
        final var raw = query.get(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(raw);
    }

    static Integer parseOptionalInt(final Map<String, String> query, final String name) {
        final var raw = query.get(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Integer.parseInt(raw.trim());
    }
}
