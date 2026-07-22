package com.umc.product.community.adapter.in.websocket;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.dto.request.CreateCommunityThreadMessageRequest;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;

@DisplayName("Community STOMP clientMessageId resolver")
class CommunityStompClientMessageIdResolverTest {

    private static final String CLIENT_MESSAGE_ID = "0dce06f4-11bc-4dc2-b9fd-4f9cb88ea9cd";
    private final CommunityStompClientMessageIdResolver sut =
        new CommunityStompClientMessageIdResolver(new ObjectMapper());

    @Test
    @DisplayName("정확한 message create destination만 지원한다")
    void supportsOnlyExactMessageCreateDestination() {
        assertThat(sut.supports("/app/community/threads/10/messages")).isTrue();
        assertThat(sut.supports("/app/community/threads/10/messages/20/edit")).isFalse();
        assertThat(sut.supports("/app/community/threads/0/messages")).isFalse();
    }

    @Test
    @DisplayName("raw create payload의 canonical clientMessageId를 보존한다")
    void resolvesCanonicalClientMessageIdFromRawPayload() {
        byte[] payload = ("{\"clientMessageId\":\"" + CLIENT_MESSAGE_ID + "\"}").getBytes(UTF_8);

        assertThat(sut.resolve(payload)).contains(UUID.fromString(CLIENT_MESSAGE_ID));
    }

    @Test
    @DisplayName("canonical lowercase UUID가 아닌 clientMessageId는 보존하지 않는다")
    void rejectsNonCanonicalClientMessageId() {
        byte[] payload = "{\"clientMessageId\":\"0DCE06F4-11BC-4DC2-B9FD-4F9CB88EA9CD\"}"
            .getBytes(UTF_8);

        assertThat(sut.resolve(payload)).isEmpty();
    }

    @Test
    @DisplayName("typed request와 String JSON에서도 clientMessageId를 복원한다")
    void resolvesTypedAndStringPayloads() {
        CreateCommunityThreadMessageRequest request = new CreateCommunityThreadMessageRequest(
            CLIENT_MESSAGE_ID,
            CommunityThreadMessageType.TEXT,
            "메시지",
            java.util.List.of(),
            java.util.List.of(),
            null
        );

        assertThat(sut.resolve(request)).contains(UUID.fromString(CLIENT_MESSAGE_ID));
        assertThat(sut.resolve("{\"clientMessageId\":\"" + CLIENT_MESSAGE_ID + "\"}"))
            .contains(UUID.fromString(CLIENT_MESSAGE_ID));
    }

    @Test
    @DisplayName("null·미지원 payload·잘못된 JSON·비문자 필드는 빈 결과를 반환한다")
    void rejectsUnsupportedAndMalformedPayloads() {
        assertThat(sut.resolve(null)).isEmpty();
        assertThat(sut.resolve(1L)).isEmpty();
        assertThat(sut.resolve("{" )).isEmpty();
        assertThat(sut.resolve("null")).isEmpty();
        assertThat(sut.resolve("{\"clientMessageId\":1}")).isEmpty();
        assertThat(sut.resolve("{\"other\":\"value\"}")).isEmpty();
    }
}
