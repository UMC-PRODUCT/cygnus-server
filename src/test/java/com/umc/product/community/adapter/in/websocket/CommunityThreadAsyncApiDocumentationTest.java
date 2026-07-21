package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@DisplayName("Community Thread AsyncAPI 문서")
class CommunityThreadAsyncApiDocumentationTest {

    private static final String DOCUMENT_RESOURCE = "static/docs/asyncapi.yaml";
    private static final String HTML_RESOURCE = "static/docs/asyncapi.html";

    @Test
    @DisplayName("AsyncAPI 명세는 연결과 전체 STOMP command destination을 제공한다")
    void specificationContainsConnectionAndCommandContracts() throws Exception {
        String document = resource(DOCUMENT_RESOURCE);

        assertThat(document).contains(
            "asyncapi: 3.0.0",
            "pathname: /ws/websocket",
            "protocol: stomp",
            "protocolVersion: \"1.2\"",
            "x-sockjs-path: /ws",
            "/app/community/threads/{threadId}/messages",
            "/app/community/threads/{threadId}/messages/{messageId}/edit",
            "/app/community/threads/{threadId}/messages/{messageId}/delete",
            "/app/community/threads/{threadId}/messages/{messageId}/reactions/add",
            "/app/community/threads/{threadId}/messages/{messageId}/reactions/remove",
            "/app/community/threads/{threadId}/read",
            "AddReactionCommand:",
            "RemoveReactionCommand:",
            "x-command-id"
        );
    }

    @Test
    @DisplayName("AsyncAPI 명세는 구독 destination과 모든 event type 및 error payload를 제공한다")
    void specificationContainsSubscriptionAndEventContracts() throws Exception {
        String document = resource(DOCUMENT_RESOURCE);

        assertThat(document).contains(
            "/topic/community/threads/{threadId}/members/{memberId}/events",
            "/topic/community/members/{memberId}/events",
            "/user/queue/errors",
            "command.acknowledged",
            "message.created",
            "message.updated",
            "message.deleted",
            "reaction.changed",
            "read.updated",
            "thread.invited",
            "thread.updated",
            "thread.deleted",
            "member.kicked",
            "member.left",
            "CommandErrorPayload:"
        ).doesNotContain("chatRoomId", "roomId");
    }

    @Test
    @DisplayName("AsyncAPI UI는 고정 버전 renderer와 로컬 명세 및 테스트 콘솔을 연결한다")
    void htmlUsesPinnedRendererAndLocalResources() throws Exception {
        String html = resource(HTML_RESOURCE);

        assertThat(html).contains(
            "@asyncapi/react-component@3.1.4/styles/default.min.css",
            "@asyncapi/react-component@3.1.4/browser/standalone/index.js",
            "AsyncApiStandalone.render",
            "url: \"/docs/asyncapi.yaml\"",
            "href=\"/docs/community-thread.html\"",
            "href=\"/docs\""
        ).doesNotContain("@asyncapi/react-component@latest");
    }

    private String resource(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        assertThat(resource.exists()).isTrue();
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
