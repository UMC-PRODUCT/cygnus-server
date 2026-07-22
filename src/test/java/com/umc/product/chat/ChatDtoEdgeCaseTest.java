package com.umc.product.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageForViewersQuery;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@DisplayName("Chat command·query DTO 경계")
class ChatDtoEdgeCaseTest {

    @Test
    @DisplayName("메시지 생성 command는 blank 첨부와 비양수 mention을 거절한다")
    void createCommand_rejectsInvalidCollections() {
        assertChatError(
            () -> command(List.of(" "), List.of()),
            ChatErrorCode.CHAT_MESSAGE_INVALID_ATTACHMENT
        );
        assertChatError(
            () -> command(Arrays.asList("file-1", null), List.of()),
            ChatErrorCode.CHAT_MESSAGE_INVALID_ATTACHMENT
        );
        assertChatError(
            () -> command(List.of(), List.of(0L)),
            ChatErrorCode.CHAT_MESSAGE_INVALID_MENTION
        );
        assertChatError(
            () -> command(List.of(), Arrays.asList(1L, null)),
            ChatErrorCode.CHAT_MESSAGE_INVALID_MENTION
        );
    }

    @Test
    @DisplayName("viewer batch query는 양수 ID만 허용하고 입력 중복을 제거한다")
    void viewerQuery_validatesAndDeduplicatesIds() {
        GetChatMessageForViewersQuery query =
            new GetChatMessageForViewersQuery(1L, 2L, List.of(3L, 3L, 4L));
        assertThat(query.viewerMemberIds()).containsExactly(3L, 4L);

        assertThatThrownBy(() -> new GetChatMessageForViewersQuery(0L, 2L, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GetChatMessageForViewersQuery(1L, 2L, List.of(0L)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GetChatMessageForViewersQuery(1L, 2L, null))
            .isInstanceOf(NullPointerException.class);
    }

    private CreateChatMessageCommand command(List<String> files, List<Long> mentions) {
        return new CreateChatMessageCommand(
            1L,
            2L,
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            MessageContentType.TEXT,
            "본문",
            files,
            mentions,
            null
        );
    }

    private void assertChatError(
        org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
        ChatErrorCode expected
    ) {
        assertThatThrownBy(callable)
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(expected);
    }
}
