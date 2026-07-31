package com.umc.product.community.adapter.in.websocket;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class CommunityThreadE2EHttpClient implements AutoCloseable {

    private final int port;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    CommunityThreadE2EHttpClient(int port, ObjectMapper objectMapper) {
        this.port = port;
        this.objectMapper = objectMapper;
    }

    JsonNode history(Long threadId, String accessToken) throws IOException, InterruptedException {
        return successResult(request(
            "GET",
            "/api/v1/community/threads/%d/messages?limit=100".formatted(threadId),
            accessToken,
            null
        ));
    }

    JsonNode kick(Long threadId, Long memberId, String accessToken) throws IOException, InterruptedException {
        return successResult(request(
            "DELETE",
            "/api/v1/community/threads/%d/members/%d".formatted(threadId, memberId),
            accessToken,
            null
        ));
    }

    JsonNode invite(Long threadId, Long memberId, String accessToken) throws IOException, InterruptedException {
        return successResult(request(
            "POST",
            "/api/v1/community/threads/%d/invite".formatted(threadId),
            accessToken,
            Map.of("memberIds", List.of(memberId))
        ));
    }

    JsonNode transferOwnership(Long threadId, Long memberId, String accessToken)
        throws IOException, InterruptedException {
        return successResult(request(
            "PATCH",
            "/api/v1/community/threads/%d/members/%d/role".formatted(threadId, memberId),
            accessToken,
            Map.of("role", "OWNER")
        ));
    }

    JsonNode leave(Long threadId, String accessToken) throws IOException, InterruptedException {
        return successResult(request(
            "POST",
            "/api/v1/community/threads/%d/leave".formatted(threadId),
            accessToken,
            null
        ));
    }

    JsonNode deleteThread(Long threadId, String accessToken) throws IOException, InterruptedException {
        return successResult(request(
            "DELETE",
            "/api/v1/community/threads/%d".formatted(threadId),
            accessToken,
            null
        ));
    }

    HttpResult historyResult(Long threadId, String accessToken) throws IOException, InterruptedException {
        HttpResponse<String> response = request(
            "GET",
            "/api/v1/community/threads/%d/messages?limit=100".formatted(threadId),
            accessToken,
            null
        );
        return new HttpResult(response.statusCode(), objectMapper.readTree(response.body()));
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private HttpResponse<String> request(String method, String path, String accessToken, Object body)
        throws IOException, InterruptedException {
        HttpRequest.BodyPublisher publisher = body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:%d%s".formatted(port, path)))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .method(method, publisher)
            .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode successResult(HttpResponse<String> response) throws IOException {
        JsonNode body = objectMapper.readTree(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || !body.path("success").asBoolean()) {
            throw new AssertionError("Community REST 요청이 실패했습니다: status="
                + response.statusCode() + ", body=" + response.body());
        }
        return body.path("result");
    }

    record HttpResult(int status, JsonNode body) {
    }
}
