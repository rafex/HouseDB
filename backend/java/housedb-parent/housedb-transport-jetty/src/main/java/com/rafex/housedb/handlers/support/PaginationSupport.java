package com.rafex.housedb.handlers.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PaginationSupport {

    private PaginationSupport() {
    }

    public static PaginationRequest request(final Integer limit, final Integer offset, final int defaultLimit,
            final int maxLimit) {
        final int safeLimit;
        if (limit == null || limit < 1) {
            safeLimit = defaultLimit;
        } else {
            safeLimit = Math.min(limit, maxLimit);
        }

        final int safeOffset;
        if (offset == null) {
            safeOffset = 0;
        } else if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        } else {
            safeOffset = offset;
        }

        return new PaginationRequest(safeLimit, safeOffset, safeLimit + 1);
    }

    public static Map<String, Object> response(final String collectionKey, final List<?> source,
            final PaginationRequest page) {
        final boolean hasMore = source.size() > page.limit();
        final List<?> visible = hasMore ? new ArrayList<>(source.subList(0, page.limit())) : source;

        final Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("limit", page.limit());
        pagination.put("offset", page.offset());
        pagination.put("returned", visible.size());
        pagination.put("hasMore", hasMore);
        pagination.put("previousOffset", page.offset() > 0 ? Math.max(0, page.offset() - page.limit()) : null);
        pagination.put("nextOffset", hasMore ? page.offset() + page.limit() : null);

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(collectionKey, visible);
        payload.put("count", visible.size());
        payload.put("pagination", pagination);
        return payload;
    }

    public record PaginationRequest(int limit, int offset, int fetchLimit) {
    }
}
