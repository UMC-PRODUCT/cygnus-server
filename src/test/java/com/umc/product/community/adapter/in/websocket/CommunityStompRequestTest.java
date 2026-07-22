package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.dto.request.ChangeCommunityThreadReactionRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.CreateCommunityThreadMessageRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.DeleteCommunityThreadMessageRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.EditCommunityThreadMessageRequest;
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
        assertThat(CommunityStompUuid.parseOrNull(null)).isNull();
        assertThat(CommunityStompUuid.parseOrNull("not-a-uuid")).isNull();
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

    @Test
    @DisplayName("message create는 파일·mention·reply·길이·type 경계를 모두 검증한다")
    void validatesCreateMessageEdgeCases() {
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID, null, "메시지", List.of(), List.of(), null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID, CommunityThreadMessageType.TEXT, "메시지", List.of("file"), List.of(), null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID, CommunityThreadMessageType.IMAGE, null, List.of(" "), List.of(), null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID, CommunityThreadMessageType.IMAGE, null, List.of("a", "a"), List.of(), null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID,
            CommunityThreadMessageType.IMAGE,
            null,
            List.of("a", "b", "c", "d", "e"),
            List.of(),
            null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID, CommunityThreadMessageType.TEXT, "메시지", List.of(), List.of(0L), null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID,
            CommunityThreadMessageType.TEXT,
            "메시지",
            List.of(),
            java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList(),
            null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID, CommunityThreadMessageType.TEXT, "메시지", null, null, 0L
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID, CommunityThreadMessageType.TEXT, "가".repeat(2_001), null, null, null
        )).isInstanceOf(IllegalArgumentException.class);

        CreateCommunityThreadMessageRequest image = new CreateCommunityThreadMessageRequest(
            CANONICAL_UUID,
            CommunityThreadMessageType.IMAGE,
            null,
            List.of("a"),
            List.of(3L, 1L, 3L),
            2L
        );
        assertThat(image.mentionedMemberIds()).containsExactly(1L, 3L);
        assertThat(image.toCommand(10L, 20L).clientMessageId().toString()).isEqualTo(CANONICAL_UUID);
        assertThatThrownBy(() -> image.rejectUnknownField("extra", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("request DTO는 unknown field와 emoji·content 경계를 fail-fast로 거부한다")
    void validatesOtherRequestContracts() {
        assertThatThrownBy(() -> new ChangeCommunityThreadReactionRequest(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChangeCommunityThreadReactionRequest("ab"))
            .isInstanceOf(IllegalArgumentException.class);
        ChangeCommunityThreadReactionRequest reaction = new ChangeCommunityThreadReactionRequest("👍🏽");
        assertThat(reaction.toCommand(1L, 2L, 3L).emoji()).isEqualTo("👍🏽");
        assertThatThrownBy(() -> reaction.rejectUnknownField("extra", null))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new EditCommunityThreadMessageRequest("가".repeat(2_001)))
            .isInstanceOf(IllegalArgumentException.class);
        EditCommunityThreadMessageRequest edit = new EditCommunityThreadMessageRequest(null);
        assertThat(edit.toCommand(1L, 2L, 3L).content()).isNull();
        assertThatThrownBy(() -> edit.rejectUnknownField("extra", null))
            .isInstanceOf(IllegalArgumentException.class);

        UpdateCommunityThreadReadRequest read = new UpdateCommunityThreadReadRequest(4L);
        assertThat(read.toCommand(1L, 2L).lastReadMessageId()).isEqualTo(4L);
        assertThatThrownBy(() -> read.rejectUnknownField("extra", null))
            .isInstanceOf(IllegalArgumentException.class);

        DeleteCommunityThreadMessageRequest delete = new DeleteCommunityThreadMessageRequest();
        assertThat(delete.toCommand(1L, 2L, 3L).messageId()).isEqualTo(2L);
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
