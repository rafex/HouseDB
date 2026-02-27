package com.rafex.housedb.kiwi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.rafex.housedb.json.JsonUtil;

public final class KiwiApiClient {

    private static final Logger LOG = Logger.getLogger(KiwiApiClient.class.getName());

    public static final class KiwiApiException extends RuntimeException {

        private static final long serialVersionUID = -6318062762215704910L;

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
    private volatile String cachedBearerToken;
    private volatile Instant cachedBearerTokenExpiresAt;

    public KiwiApiClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                System.getenv().getOrDefault("KIWI_API_BASE_URL", "https://kiwi.v1.rafex.cloud"));
    }

    KiwiApiClient(final HttpClient httpClient, final String baseUrl) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
    }

    public UUID createLocation(final String name, final UUID parentLocationId)
            throws IOException, InterruptedException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        final var token = resolveBearerToken();

        final var endpoint = baseUrl.endsWith("/") ? baseUrl + "locations" : baseUrl + "/locations";
        final var json = JsonUtil.MAPPER.createObjectNode().put("name", name);
        if (parentLocationId != null) {
            json.put("parentLocationId", parentLocationId.toString());
        }
        final var body = JsonUtil.MAPPER.writeValueAsString(json);

        final var request = HttpRequest.newBuilder().uri(URI.create(endpoint)).timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KiwiApiException(response.statusCode(),
                    "Kiwi API error creating location: HTTP " + response.statusCode());
        }

        final var responseJson = JsonUtil.MAPPER.readTree(response.body());
        final var locationId = responseJson.get("location_id");
        if (locationId == null || locationId.asText().isBlank()) {
            throw new IllegalStateException("Kiwi API response missing location_id");
        }
        return UUID.fromString(locationId.asText());
    }

    public JsonNode getObjectById(final UUID objectId) throws IOException, InterruptedException {
        if (objectId == null) {
            throw new IllegalArgumentException("objectId is required");
        }
        final var token = resolveBearerToken();

        final var endpointBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        final var endpoint = endpointBase + "/objects/" + objectId;

        final var request = HttpRequest.newBuilder().uri(URI.create(endpoint)).timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token).header("Accept", "application/json").GET().build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KiwiApiException(response.statusCode(),
                    "Kiwi API error fetching object: HTTP " + response.statusCode());
        }
        return JsonUtil.MAPPER.readTree(response.body());
    }

    public UUID createObject(final String name, final String description, final UUID locationId, final String type,
            final Collection<String> tags, final Object metadata) throws IOException, InterruptedException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("locationId is required");
        }
        final var token = resolveBearerToken();

        final var endpoint = baseUrl.endsWith("/") ? baseUrl + "objects" : baseUrl + "/objects";
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
        final var request = HttpRequest.newBuilder().uri(URI.create(endpoint)).timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KiwiApiException(response.statusCode(),
                    "Kiwi API error creating object: HTTP " + response.statusCode());
        }

        final var responseJson = JsonUtil.MAPPER.readTree(response.body());
        final var objectIdNode = responseJson.get("object_id");
        if (objectIdNode == null || objectIdNode.asText().isBlank()) {
            throw new IllegalStateException("Kiwi API response missing object_id");
        }
        return UUID.fromString(objectIdNode.asText());
    }

    public void bootstrapAppClientFromEnv() throws IOException, InterruptedException {
        final var enabled = Boolean.parseBoolean(System.getenv().getOrDefault("KIWI_BOOTSTRAP_APP_CLIENT", "false"));
        if (!enabled) {
            return;
        }

        final var adminUsername = System.getenv("KIWI_ADMIN_USERNAME");
        final var adminPassword = System.getenv("KIWI_ADMIN_PASSWORD");
        final var clientId = System.getenv("KIWI_APP_CLIENT_ID");
        final var clientSecret = System.getenv("KIWI_APP_CLIENT_SECRET");
        final var clientName = System.getenv().getOrDefault("KIWI_APP_CLIENT_NAME", "HouseDB");
        final var clientRoles = System.getenv().getOrDefault("KIWI_APP_CLIENT_ROLES", "ADMIN");

        if (isBlank(adminUsername) || isBlank(adminPassword) || isBlank(clientId) || isBlank(clientSecret)) {
            throw new IllegalStateException(
                    "Missing env vars for Kiwi bootstrap. Required: KIWI_ADMIN_USERNAME, KIWI_ADMIN_PASSWORD, "
                            + "KIWI_APP_CLIENT_ID, KIWI_APP_CLIENT_SECRET");
        }

        final var adminToken = loginAdmin(adminUsername, adminPassword);
        createAppClient(adminToken, clientId, clientSecret, clientName, clientRoles);
        LOG.info("Kiwi app client bootstrap completed for client_id=" + clientId);
    }

    private String resolveBearerToken() throws IOException, InterruptedException {
        final var cached = cachedBearerToken;
        final var expiresAt = cachedBearerTokenExpiresAt;
        if (!isBlank(cached) && expiresAt != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
            return cached;
        }

        final var clientId = System.getenv("KIWI_APP_CLIENT_ID");
        final var clientSecret = System.getenv("KIWI_APP_CLIENT_SECRET");
        if (isBlank(clientId) || isBlank(clientSecret)) {
            throw new IllegalStateException(
                    "Missing Kiwi credentials: set KIWI_APP_CLIENT_ID + KIWI_APP_CLIENT_SECRET");
        }

        synchronized (this) {
            if (!isBlank(cachedBearerToken)
                    && cachedBearerTokenExpiresAt != null
                    && Instant.now().isBefore(cachedBearerTokenExpiresAt.minusSeconds(30))) {
                return cachedBearerToken;
            }
            final var tokenData = requestClientCredentialsToken(clientId, clientSecret);
            cachedBearerToken = tokenData.token();
            cachedBearerTokenExpiresAt = tokenData.expiresAt();
            return tokenData.token();
        }
    }

    private String loginAdmin(final String username, final String password) throws IOException, InterruptedException {
        final var endpoint = baseUrl.endsWith("/") ? baseUrl + "auth/login" : baseUrl + "/auth/login";
        final var body = JsonUtil.MAPPER.writeValueAsString(
                JsonUtil.MAPPER.createObjectNode().put("username", username).put("password", password));

        final var request = HttpRequest.newBuilder().uri(URI.create(endpoint)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KiwiApiException(response.statusCode(),
                    "Kiwi API admin login failed: HTTP " + response.statusCode());
        }
        return extractAccessToken(response.body(), "Kiwi API admin login");
    }

    private TokenData requestClientCredentialsToken(final String clientId, final String clientSecret)
            throws IOException, InterruptedException {
        final var endpoint = baseUrl.endsWith("/") ? baseUrl + "auth/token" : baseUrl + "/auth/token";
        final var body = JsonUtil.MAPPER.writeValueAsString(
                JsonUtil.MAPPER.createObjectNode()
                        .put("client_id", clientId)
                        .put("client_secret", clientSecret)
                        .put("grant_type", "client_credentials"));

        final var request = HttpRequest.newBuilder().uri(URI.create(endpoint)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KiwiApiException(response.statusCode(),
                    "Kiwi API client token failed: HTTP " + response.statusCode());
        }
        final var json = JsonUtil.MAPPER.readTree(response.body());
        final var token = extractAccessToken(json, "Kiwi API client token");
        final var expiresIn = json.path("expires_in").asLong(300);
        return new TokenData(token, Instant.now().plusSeconds(Math.max(60, expiresIn)));
    }

    private void createAppClient(final String adminToken, final String clientId, final String clientSecret,
            final String name, final String csvRoles) throws IOException, InterruptedException {
        final var endpoint = baseUrl.endsWith("/") ? baseUrl + "admin/app-clients" : baseUrl + "/admin/app-clients";
        final var json = JsonUtil.MAPPER.createObjectNode();
        json.put("client_id", clientId);
        json.put("client_secret", clientSecret);
        json.put("name", name);
        final var rolesNode = json.putArray("roles");
        for (final var raw : csvRoles.split(",")) {
            final var role = raw == null ? "" : raw.trim();
            if (!role.isBlank()) {
                rolesNode.add(role);
            }
        }

        final var request = HttpRequest.newBuilder().uri(URI.create(endpoint)).timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.MAPPER.writeValueAsString(json)))
                .build();

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return;
        }
        // Idempotent behavior for already-existing app client.
        if (response.statusCode() == 409) {
            LOG.info("Kiwi app client already exists: " + clientId);
            return;
        }
        throw new KiwiApiException(response.statusCode(),
                "Kiwi API create app client failed: HTTP " + response.statusCode());
    }

    private static String extractAccessToken(final String body, final String context) throws IOException {
        return extractAccessToken(JsonUtil.MAPPER.readTree(body), context);
    }

    private static String extractAccessToken(final JsonNode json, final String context) {
        final var accessToken = json.get("access_token");
        if (accessToken != null && !accessToken.asText().isBlank()) {
            return accessToken.asText();
        }
        final var token = json.get("token");
        if (token != null && !token.asText().isBlank()) {
            return token.asText();
        }
        throw new IllegalStateException(context + " response missing access_token");
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private record TokenData(String token, Instant expiresAt) {
    }
}
