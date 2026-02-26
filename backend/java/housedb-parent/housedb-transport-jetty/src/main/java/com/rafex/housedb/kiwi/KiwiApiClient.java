package com.rafex.housedb.kiwi;

import com.fasterxml.jackson.databind.JsonNode;
import com.rafex.housedb.json.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public final class KiwiApiClient {

    public static final class KiwiApiException extends RuntimeException {
        private final int statusCode;

        KiwiApiException(final int statusCode, final String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String bearerToken;

    public KiwiApiClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                System.getenv().getOrDefault("KIWI_API_BASE_URL", "http://localhost:8080"),
                System.getenv("KIWI_API_TOKEN"));
    }

    KiwiApiClient(final HttpClient httpClient, final String baseUrl, final String bearerToken) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.bearerToken = bearerToken;
    }

    public UUID createLocation(final String name, final UUID parentLocationId) throws IOException, InterruptedException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalStateException("KIWI_API_TOKEN is required");
        }

        final String endpoint = baseUrl.endsWith("/") ? baseUrl + "locations" : baseUrl + "/locations";
        final var json = JsonUtil.MAPPER.createObjectNode().put("name", name);
        if (parentLocationId != null) {
            json.put("parentLocationId", parentLocationId.toString());
        }
        final var body = JsonUtil.MAPPER.writeValueAsString(json);

        final var request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KiwiApiException(response.statusCode(),
                    "Kiwi API error creating location: HTTP " + response.statusCode());
        }

        final JsonNode responseJson = JsonUtil.MAPPER.readTree(response.body());
        final JsonNode locationId = responseJson.get("location_id");
        if (locationId == null || locationId.asText().isBlank()) {
            throw new IllegalStateException("Kiwi API response missing location_id");
        }
        return UUID.fromString(locationId.asText());
    }

    public JsonNode getObjectById(final UUID objectId) throws IOException, InterruptedException {
        if (objectId == null) {
            throw new IllegalArgumentException("objectId is required");
        }
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalStateException("KIWI_API_TOKEN is required");
        }

        final String endpointBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        final String endpoint = endpointBase + "/objects/" + objectId;

        final var request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KiwiApiException(response.statusCode(),
                    "Kiwi API error fetching object: HTTP " + response.statusCode());
        }
        return JsonUtil.MAPPER.readTree(response.body());
    }

    public UUID createObject(final String name, final String description, final UUID locationId, final String type,
            final Collection<String> tags, final Object metadata)
            throws IOException, InterruptedException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("locationId is required");
        }
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalStateException("KIWI_API_TOKEN is required");
        }

        final String endpoint = baseUrl.endsWith("/") ? baseUrl + "objects" : baseUrl + "/objects";
        final var json = JsonUtil.MAPPER.createObjectNode();
        json.put("name", name);
        if (description != null && !description.isBlank()) {
            json.put("description", description);
        }
        json.put("type", type == null || type.isBlank() ? "EQUIPMENT" : type.trim());
        json.put("locationId", locationId.toString());

        if (tags != null && !tags.isEmpty()) {
            final var cleanTags = new ArrayList<String>();
            for (final var tag : tags) {
                if (tag == null) {
                    continue;
                }
                final var v = tag.trim();
                if (!v.isBlank()) {
                    cleanTags.add(v);
                }
            }
            if (!cleanTags.isEmpty()) {
                json.putPOJO("tags", cleanTags);
            }
        }

        if (metadata != null) {
            json.set("metadata", JsonUtil.MAPPER.valueToTree(metadata));
        }

        final var body = JsonUtil.MAPPER.writeValueAsString(json);
        final var request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KiwiApiException(response.statusCode(),
                    "Kiwi API error creating object: HTTP " + response.statusCode());
        }

        final JsonNode responseJson = JsonUtil.MAPPER.readTree(response.body());
        final JsonNode objectIdNode = responseJson.get("object_id");
        if (objectIdNode == null || objectIdNode.asText().isBlank()) {
            throw new IllegalStateException("Kiwi API response missing object_id");
        }
        return UUID.fromString(objectIdNode.asText());
    }
}
