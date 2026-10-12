package com.campus.lostfound.client.service;

import com.campus.lostfound.client.model.*;
import com.campus.lostfound.client.util.MultipartBodyPublisher;
import com.campus.lostfound.client.util.SessionManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

public class ApiClient {

    private static final ApiClient INSTANCE = new ApiClient();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String baseUrl = "http://localhost:8080";

    private ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        loadConfiguration();
    }

    public static ApiClient getInstance() {
        return INSTANCE;
    }

    private void loadConfiguration() {
        try (InputStream in = getClass().getResourceAsStream("/config.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String url = props.getProperty("api.base-url");
                if (url != null && !url.isBlank()) {
                    this.baseUrl = url.trim().replaceAll("/+$", "");
                }
            }
        } catch (Exception ignored) {}
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // ===============================================================
    // AUTHENTICATION
    // ===============================================================

    public UserDto login(String identifier, String password) throws IOException, InterruptedException, ApiException {
        Map<String, String> body = Map.of("identifier", identifier, "password", password);
        String json = objectMapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);

        JsonNode root = objectMapper.readTree(resp.body());
        String token = root.path("token").asText();
        UserDto user = objectMapper.treeToValue(root.path("user"), UserDto.class);

        SessionManager.getInstance().setSession(token, user);
        return user;
    }

    public UserDto register(String name, String email, String rollNumber, String password) throws IOException, InterruptedException, ApiException {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        if (rollNumber != null && !rollNumber.isBlank()) {
            body.put("rollNumber", rollNumber.trim().toUpperCase());
        }
        body.put("password", password);

        String json = objectMapper.writeValueAsString(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);

        return objectMapper.readValue(resp.body(), UserDto.class);
    }

    public UserDto getMe() throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/auth/me").GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), UserDto.class);
    }

    // ===============================================================
    // ITEMS
    // ===============================================================

    public PageResponse<ItemDto> searchItems(String q, Category category, String location, ItemStatus status,
                                            ItemType type, LocalDate dateFrom, LocalDate dateTo,
                                            int page, int size) throws IOException, InterruptedException, ApiException {
        StringBuilder query = new StringBuilder("?page=").append(page).append("&size=").append(size);
        if (q != null && !q.isBlank()) query.append("&q=").append(URLEncoder.encode(q, StandardCharsets.UTF_8));
        if (category != null) query.append("&category=").append(category.name());
        if (location != null && !location.isBlank()) query.append("&location=").append(URLEncoder.encode(location, StandardCharsets.UTF_8));
        if (status != null) query.append("&status=").append(status.name());
        if (type != null) query.append("&type=").append(type.name());
        if (dateFrom != null) query.append("&dateFrom=").append(dateFrom);
        if (dateTo != null) query.append("&dateTo=").append(dateTo);

        HttpRequest req = authRequest("/api/items/search" + query).GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);

        return objectMapper.readValue(resp.body(), new TypeReference<PageResponse<ItemDto>>() {});
    }

    public List<ItemDto> getMyReports() throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/items/mine").GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), new TypeReference<List<ItemDto>>() {});
    }

    public PageResponse<ItemDto> getInventory(int page, int size) throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/items/inventory?page=" + page + "&size=" + size).GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), new TypeReference<PageResponse<ItemDto>>() {});
    }

    public ItemDto getItemById(Long id) throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/items/" + id).GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), ItemDto.class);
    }

    public byte[] getItemPhoto(Long id) throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/items/" + id + "/photo").GET().build();
        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() == 404) return null;
        if (resp.statusCode() >= 400) {
            throw new ApiException(resp.statusCode(), "Failed to load photo");
        }
        return resp.body();
    }

    public ItemDto createLostItem(Map<String, Object> data, byte[] photoBytes, String photoFilename, String photoContentType)
            throws IOException, InterruptedException, ApiException {
        data.put("type", "LOST");
        return submitItemWithOptionalPhoto(data, photoBytes, photoFilename, photoContentType);
    }

    public ItemDto createFoundItem(Map<String, Object> data, byte[] photoBytes, String photoFilename, String photoContentType)
            throws IOException, InterruptedException, ApiException {
        data.put("type", "FOUND");
        return submitItemWithOptionalPhoto(data, photoBytes, photoFilename, photoContentType);
    }

    private ItemDto submitItemWithOptionalPhoto(Map<String, Object> data, byte[] photoBytes, String filename, String contentType)
            throws IOException, InterruptedException, ApiException {
        String json = objectMapper.writeValueAsString(data);

        HttpRequest req;
        if (photoBytes != null && photoBytes.length > 0) {
            MultipartBodyPublisher multipart = MultipartBodyPublisher.newBuilder()
                    .addJsonPart("data", json)
                    .addFilePart("photo", filename != null ? filename : "photo.jpg", contentType != null ? contentType : "image/jpeg", photoBytes);

            req = authRequest("/api/items")
                    .header("Content-Type", multipart.getContentType())
                    .POST(multipart.build())
                    .build();
        } else {
            req = authRequest("/api/items")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
        }

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), ItemDto.class);
    }

    public ItemDto editLostItem(Long id, Map<String, Object> data) throws IOException, InterruptedException, ApiException {
        String json = objectMapper.writeValueAsString(data);
        HttpRequest req = authRequest("/api/items/" + id)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), ItemDto.class);
    }

    public ItemDto cancelLostItem(Long id) throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/items/" + id + "/cancel")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), ItemDto.class);
    }

    public ItemDto disposeItem(Long id, String action) throws IOException, InterruptedException, ApiException {
        Map<String, String> body = Map.of("action", action);
        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = authRequest("/api/items/" + id + "/dispose")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), ItemDto.class);
    }

    // ===============================================================
    // CLAIMS
    // ===============================================================

    public ClaimDto submitClaim(Long itemId, String answer, Long lostItemId, byte[] proofBytes, String proofFilename, String proofContentType)
            throws IOException, InterruptedException, ApiException {
        Map<String, Object> data = new HashMap<>();
        data.put("itemId", itemId);
        data.put("answer", answer);
        if (lostItemId != null) data.put("lostItemId", lostItemId);

        String json = objectMapper.writeValueAsString(data);
        HttpRequest req;

        if (proofBytes != null && proofBytes.length > 0) {
            MultipartBodyPublisher multipart = MultipartBodyPublisher.newBuilder()
                    .addJsonPart("data", json)
                    .addFilePart("proof", proofFilename != null ? proofFilename : "proof.jpg",
                            proofContentType != null ? proofContentType : "image/jpeg", proofBytes);

            req = authRequest("/api/claims")
                    .header("Content-Type", multipart.getContentType())
                    .POST(multipart.build())
                    .build();
        } else {
            req = authRequest("/api/claims")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
        }

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), ClaimDto.class);
    }

    public List<ClaimDto> getMyClaims() throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/claims/mine").GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), new TypeReference<List<ClaimDto>>() {});
    }

    public PageResponse<ClaimDto> getClaims(ClaimStatus status, int page, int size) throws IOException, InterruptedException, ApiException {
        String path = "/api/claims?page=" + page + "&size=" + size;
        if (status != null) path += "&status=" + status.name();
        HttpRequest req = authRequest(path).GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), new TypeReference<PageResponse<ClaimDto>>() {});
    }

    public ClaimDto reviewClaim(Long claimId, String decision, String note) throws IOException, InterruptedException, ApiException {
        Map<String, String> body = new HashMap<>();
        body.put("decision", decision);
        if (note != null) body.put("note", note);

        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = authRequest("/api/claims/" + claimId + "/review")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), ClaimDto.class);
    }

    public byte[] getClaimProof(Long claimId) throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/claims/" + claimId + "/proof").GET().build();
        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() == 404) return null;
        if (resp.statusCode() >= 400) {
            throw new ApiException(resp.statusCode(), "Failed to load claim proof document");
        }
        return resp.body();
    }

    // ===============================================================
    // MATCHES
    // ===============================================================

    public List<MatchDto> getMatches() throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/matches").GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), new TypeReference<List<MatchDto>>() {});
    }

    public void dismissMatch(Long matchId) throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/matches/" + matchId + "/dismiss")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
    }

    // ===============================================================
    // NOTIFICATIONS
    // ===============================================================

    public PageResponse<NotificationDto> getNotifications(int page, int size) throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/notifications?page=" + page + "&size=" + size).GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), new TypeReference<PageResponse<NotificationDto>>() {});
    }

    public long getUnreadNotificationCount() {
        try {
            HttpRequest req = authRequest("/api/notifications/unread-count").GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(resp.body());
                return node.path("count").asLong(0);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public void markNotificationRead(Long id) throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/notifications/" + id + "/read")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
    }

    public void markAllNotificationsRead() throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/notifications/read-all")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
    }

    // ===============================================================
    // ADMIN
    // ===============================================================

    public AdminStatsDto getAdminStats() throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/admin/stats").GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), AdminStatsDto.class);
    }

    public List<UserDto> getAdminUsers() throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/admin/users").GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), new TypeReference<List<UserDto>>() {});
    }

    public UserDto createAdminUser(String name, String email, String rollNumber, String password, Role role)
            throws IOException, InterruptedException, ApiException {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        if (rollNumber != null && !rollNumber.isBlank()) body.put("rollNumber", rollNumber.trim().toUpperCase());
        body.put("password", password);
        body.put("role", role.name());

        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = authRequest("/api/admin/users")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), UserDto.class);
    }

    public UserDto updateUserActive(Long id, boolean active) throws IOException, InterruptedException, ApiException {
        Map<String, Boolean> body = Map.of("active", active);
        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = authRequest("/api/admin/users/" + id + "/active")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), UserDto.class);
    }

    public UserDto updateUserRole(Long id, Role role) throws IOException, InterruptedException, ApiException {
        Map<String, String> body = Map.of("role", role.name());
        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = authRequest("/api/admin/users/" + id + "/role")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), UserDto.class);
    }

    public MatchDto createManualMatch(Long lostId, Long foundId) throws IOException, InterruptedException, ApiException {
        Map<String, Long> body = Map.of("lostId", lostId, "foundId", foundId);
        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = authRequest("/api/admin/matches")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), MatchDto.class);
    }

    public void mergeItems(Long keepId, Long removeId) throws IOException, InterruptedException, ApiException {
        Map<String, Long> body = Map.of("keepId", keepId, "removeId", removeId);
        String json = objectMapper.writeValueAsString(body);
        HttpRequest req = authRequest("/api/admin/items/merge")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
    }

    public PageResponse<Map<String, Object>> getAuditLogs(int page, int size) throws IOException, InterruptedException, ApiException {
        HttpRequest req = authRequest("/api/admin/audit?page=" + page + "&size=" + size).GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        handleErrorResponse(resp);
        return objectMapper.readValue(resp.body(), new TypeReference<PageResponse<Map<String, Object>>>() {});
    }

    // ===============================================================
    // HELPERS
    // ===============================================================

    private HttpRequest.Builder authRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path));
        String token = SessionManager.getInstance().getToken();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private void handleErrorResponse(HttpResponse<String> resp) throws ApiException {
        int code = resp.statusCode();
        if (code >= 200 && code < 300) return;

        String message = "Request failed with status " + code;
        try {
            JsonNode errorNode = objectMapper.readTree(resp.body());
            if (errorNode.has("message") && !errorNode.path("message").asText().isBlank()) {
                message = errorNode.path("message").asText();
            } else if (errorNode.has("error")) {
                message = errorNode.path("error").asText();
            }
        } catch (Exception ignored) {
            if (resp.body() != null && !resp.body().isBlank()) {
                message = resp.body();
            }
        }
        throw new ApiException(code, message);
    }
}
