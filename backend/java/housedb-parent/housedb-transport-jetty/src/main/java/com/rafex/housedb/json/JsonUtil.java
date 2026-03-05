package com.rafex.housedb.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.rafex.ether.json.JacksonJsonCodec;
import dev.rafex.ether.json.JsonCodecBuilder;

public final class JsonUtil {

    public static final JacksonJsonCodec CODEC = JsonCodecBuilder.create().build();
    public static final ObjectMapper MAPPER = CODEC.mapper();

    private JsonUtil() {
    }

    public static String toJson(final Object value) {
        return CODEC.toJson(value);
    }
}
