package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.dto.request.CreateCommunityThreadMessageRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.DeleteCommunityThreadMessageRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.UpdateCommunityThreadReadRequest;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;

class CommunityStompRequestTest {

    private static final String CANONICAL_UUID = "abcdefab-cdef-abcd-efab-cdefabcdefab";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("clientMessageId는 canonical lowercase UUID만 허용한다")
    void acceptsOnlyCanonicalLowercaseClientMessageId() {
        // when
        CreateCommunityThreadMessageRequest valid = textRequest(CANONICAL_UUID);

        // then
        assertThat(valid.clientMessageUuid().toString()).isEqualTo(CANONICAL_UUID);
        assertThatThrownBy(() -> textRequest(CANONICAL_UUID.toUpperCase()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> textRequest("1-1-1-1-1"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TEXT와 IMAGE payload의 파일 및 content 조합을 경계에서 검증한다")
    void validatesMessageTypeSpecificPayload() {
        // when & then
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID,
            CommunityThreadMessageType.TEXT,
            " ",
            List.of(),
            List.of(),
            null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID,
            CommunityThreadMessageType.IMAGE,
            null,
            List.of(),
            List.of(),
            null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("delete payload는 정확히 빈 JSON object만 허용한다")
    void deleteAcceptsOnlyExactlyEmptyObject() throws Exception {
        // when
        DeleteCommunityThreadMessageRequest valid = objectMapper.readValue(
            "{}",
            DeleteCommunityThreadMessageRequest.class
        );

        // then
        assertThat(valid).isNotNull();
        assertThatThrownBy(() -> objectMapper.readValue(
            "{\"unexpected\":true}",
            DeleteCommunityThreadMessageRequest.class
        )).hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("read watermark는 양수 message ID만 허용한다")
    void readRequiresPositiveLastReadMessageId() {
        // when & then
        assertThatThrownBy(() -> new UpdateCommunityThreadReadRequest(0L))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(new UpdateCommunityThreadReadRequest(1L).lastReadMessageId()).isEqualTo(1L);
    }

    private CreateCommunityThreadMessageRequest textRequest(String clientMessageId) {
        return new CreateCommunityThreadMessageRequest(
            clientMessageId,
            CommunityThreadMessageType.TEXT,
            "안녕하세요",
            List.of(),
            List.of(),
            null
        );
    }
}
