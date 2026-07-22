package com.umc.product.chat.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.OutboxDispatchMode;

@DisplayName("Chat realtime event dispatch mode")
class ChatRealtimeEventDispatchModeTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-18T00:00:00Z");

    @ParameterizedTest
    @MethodSource("realtimeEvents")
    @DisplayName("실시간 이벤트는 비트랜잭션 outbox dispatch를 사용한다")
    void realtimeEventsUseNonTransactionalDispatch(DomainEvent event) {
        // given
        // when
        OutboxDispatchMode dispatchMode = event.outboxDispatchMode();

        // then
        assertThat(event).isInstanceOf(ChatRealtimeEvent.class);
        assertThat(dispatchMode).isEqualTo(OutboxDispatchMode.NON_TRANSACTIONAL);
    }

    @Test
    @DisplayName("고정 메시지 변경 이벤트는 기존 트랜잭션 dispatch를 유지한다")
    void pinnedMessageEventKeepsTransactionalDispatch() {
        // given
        DomainEvent event = ChatRoomPinnedMessageChangedEvent.of(2L, 3L);

        // when
        OutboxDispatchMode dispatchMode = event.outboxDispatchMode();

        // then
        assertThat(event).isNotInstanceOf(ChatRealtimeEvent.class);
        assertThat(dispatchMode).isEqualTo(OutboxDispatchMode.TRANSACTIONAL);
    }

    @Test
    @DisplayName("reply ID 호환 생성자는 나머지 optional realtime 필드를 비운다")
    void createdEventReplyCompatibilityConstructor() {
        ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(
            EVENT_ID,
            OCCURRED_AT,
            11L,
            2L,
            3L,
            MessageContentType.TEXT,
            "reply",
            List.of(),
            10L
        );

        assertThat(event.replyToMessageId()).isEqualTo(10L);
        assertThat(event.clientMessageId()).isNull();
        assertThat(event.mentionedMemberIds()).isEmpty();
        assertThat(event.editedAt()).isNull();
        assertThat(event.deletedAt()).isNull();
    }

    private static Stream<Arguments> realtimeEvents() {
        ChatMessageSnapshot snapshot = new ChatMessageSnapshot(
            11L,
            2L,
            3L,
            MessageContentType.TEXT,
            "message",
            List.of(),
            null,
            null,
            List.of(),
            OCCURRED_AT,
            null,
            null
        );
        return Stream.of(
            Arguments.of(new ChatMessageCreatedEvent(
                EVENT_ID,
                OCCURRED_AT,
                11L,
                2L,
                3L,
                MessageContentType.TEXT,
                "message",
                List.of(),
                null,
                null,
                List.of(),
                null,
                null
            )),
            Arguments.of(new ChatMessageUpdatedEvent(EVENT_ID, OCCURRED_AT, snapshot)),
            Arguments.of(new ChatMessageDeletedEvent(EVENT_ID, OCCURRED_AT, snapshot)),
            Arguments.of(new ChatMessageReactionChangedEvent(
                EVENT_ID,
                OCCURRED_AT,
                2L,
                11L,
                3L,
                "👍",
                true
            )),
            Arguments.of(new ChatReadUpdatedEvent(EVENT_ID, OCCURRED_AT, 2L, 3L, 11L))
        );
    }
}
