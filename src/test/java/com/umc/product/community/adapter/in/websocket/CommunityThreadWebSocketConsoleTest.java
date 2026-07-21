package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@DisplayName("Community Thread WebSocket 테스트 콘솔")
class CommunityThreadWebSocketConsoleTest {

    private static final String RESOURCE_PATH =
        "static/docs/community-thread-websocket.html";

    @Test
    @DisplayName("native WebSocket 연결과 전체 STOMP destination 계약을 제공한다")
    void exposesNativeWebSocketAndStompDestinations() throws Exception {
        String html = html();

        assertThat(html).contains(
            "/ws/websocket",
            "/app/community/threads/${threadId}/messages",
            "/app/community/threads/${threadId}/messages/${messageId}/edit",
            "/app/community/threads/${threadId}/messages/${messageId}/delete",
            "/app/community/threads/${threadId}/messages/${messageId}/reactions/add",
            "/app/community/threads/${threadId}/messages/${messageId}/reactions/remove",
            "/app/community/threads/${threadId}/read",
            "/topic/community/threads/${threadId}/members/${memberId}/events",
            "/topic/community/members/${memberId}/events",
            "/user/queue/errors",
            "x-command-id"
        );
    }

    @Test
    @DisplayName("테스트 콘솔은 외부 script나 브라우저 저장소에 access token을 의존하지 않는다")
    void doesNotDependOnExternalScriptsOrBrowserStorage() throws Exception {
        String html = html();

        assertThat(html)
            .doesNotContain("<script src=", "localStorage", "sessionStorage")
            .contains("Bearer [REDACTED]");
    }

    private String html() throws Exception {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        assertThat(resource.exists()).isTrue();
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
