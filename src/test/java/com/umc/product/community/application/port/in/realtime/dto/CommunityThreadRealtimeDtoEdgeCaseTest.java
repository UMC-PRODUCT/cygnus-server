package com.umc.product.community.application.port.in.realtime.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.adapter.in.websocket.CommunityStompCommandType;
import com.umc.product.community.adapter.in.websocket.dto.event.CommunityCommandAcknowledgement;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadSummaryInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageStatus;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionInfo;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;

@DisplayName("Community thread realtime DTO 경계")
class CommunityThreadRealtimeDtoEdgeCaseTest {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID COMMAND_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    @DisplayName("모든 realtime command enum은 외부 wire value를 그대로 노출한다")
    void exposesEveryCommandWireValue() {
        assertThat(CommunityThreadRealtimeCommandType.values())
            .extracting(CommunityThreadRealtimeCommandType::value)
            .containsExactly(
                "MESSAGE_CREATE",
                "MESSAGE_EDIT",
                "MESSAGE_DELETE",
                "REACTION_ADD",
                "REACTION_REMOVE",
                "READ_UPDATE"
            );
        assertThat(CommunityThreadRealtimeEventType.values())
            .extracting(CommunityThreadRealtimeEventType::value)
            .contains("thread.deleted", "message.created", "command.acknowledged");
    }

    @Test
    @DisplayName("ACK와 thread deleted payload는 필수 상관관계·삭제 시각을 검증한다")
    void validatesPreviouslyUnexecutedPayloads() {
        CommunityThreadRealtimePayload.CommandAcknowledged acknowledged =
            new CommunityThreadRealtimePayload.CommandAcknowledged(
                COMMAND_ID,
                CommunityThreadRealtimeCommandType.MESSAGE_CREATE,
                3L,
                null,
                false
            );
        CommunityThreadRealtimePayload.ThreadDeleted deleted =
            new CommunityThreadRealtimePayload.ThreadDeleted("1", Instant.EPOCH);

        assertThat(acknowledged.commandId()).isEqualTo(COMMAND_ID);
        assertThat(deleted.threadId()).isEqualTo("1");
        assertThatThrownBy(() -> new CommunityThreadRealtimePayload.CommandAcknowledged(
            null, CommunityThreadRealtimeCommandType.MESSAGE_CREATE, null, null, false
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CommunityThreadRealtimePayload.CommandAcknowledged(
            COMMAND_ID, null, null, null, false
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CommunityThreadRealtimePayload.ThreadDeleted(null, Instant.EPOCH))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CommunityThreadRealtimePayload.ThreadDeleted("1", null))
            .isInstanceOf(NullPointerException.class);

        CommunityCommandAcknowledgement acknowledgement = new CommunityCommandAcknowledgement(
            COMMAND_ID,
            CommunityStompCommandType.MESSAGE_CREATE,
            1L,
            null,
            false
        );
        assertThat(acknowledgement.messageId()).isEqualTo(1L);
        assertInvalid(() -> new CommunityCommandAcknowledgement(
            COMMAND_ID,
            CommunityStompCommandType.MESSAGE_CREATE,
            0L,
            null,
            false
        ));
    }

    @Test
    @DisplayName("realtime event는 양수 thread ID 문자열과 필수 envelope 필드를 강제한다")
    void validatesRealtimeEnvelope() {
        CommunityThreadRealtimePayload.ThreadDeleted payload =
            new CommunityThreadRealtimePayload.ThreadDeleted("1", Instant.EPOCH);
        CommunityThreadRealtimeEvent<CommunityThreadRealtimePayload.ThreadDeleted> event =
            CommunityThreadRealtimeEvent.of(
                EVENT_ID,
                CommunityThreadRealtimeEventType.THREAD_DELETED,
                1L,
                Instant.EPOCH,
                payload
            );
        assertThat(event.threadId()).isEqualTo("1");

        assertInvalid(() -> CommunityThreadRealtimeEvent.of(
            EVENT_ID, CommunityThreadRealtimeEventType.THREAD_DELETED, 0L, Instant.EPOCH, payload));
        assertInvalid(() -> new CommunityThreadRealtimeEvent<>(
            EVENT_ID, CommunityThreadRealtimeEventType.THREAD_DELETED, " ", Instant.EPOCH, payload));
        assertInvalid(() -> new CommunityThreadRealtimeEvent<>(
            EVENT_ID, CommunityThreadRealtimeEventType.THREAD_DELETED, "0", Instant.EPOCH, payload));
        assertInvalid(() -> new CommunityThreadRealtimeEvent<>(
            EVENT_ID, CommunityThreadRealtimeEventType.THREAD_DELETED, "not-number", Instant.EPOCH, payload));
        assertThatThrownBy(() -> new CommunityThreadRealtimeEvent<>(
            null, CommunityThreadRealtimeEventType.THREAD_DELETED, "1", Instant.EPOCH, payload
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CommunityThreadRealtimeEvent<>(
            EVENT_ID, null, "1", Instant.EPOCH, payload
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CommunityThreadRealtimeEvent<>(
            EVENT_ID, CommunityThreadRealtimeEventType.THREAD_DELETED, "1", null, payload
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CommunityThreadRealtimeEvent<>(
            EVENT_ID, CommunityThreadRealtimeEventType.THREAD_DELETED, "1", Instant.EPOCH, null
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("message·reaction·read·lifecycle payload는 null과 non-positive 경계를 거부한다")
    void validatesRemainingPayloads() {
        CommunityThreadMessageInfo message = message();
        ThreadSummaryInfo thread = thread();
        assertThat(new CommunityThreadRealtimePayload.MessageCreated(message, null).message())
            .isSameAs(message);
        assertThat(new CommunityThreadRealtimePayload.MessageUpdated(message).message()).isSameAs(message);
        assertThat(new CommunityThreadRealtimePayload.MessageDeleted(message).message()).isSameAs(message);
        assertThat(new CommunityThreadRealtimePayload.ReactionChanged(
            1L, List.of(new CommunityThreadReactionInfo("👍", 1, true))).messageId()).isEqualTo(1L);
        assertThat(new CommunityThreadRealtimePayload.ReadUpdated(1L, 2L).lastReadMessageId())
            .isEqualTo(2L);
        assertThat(new CommunityThreadRealtimePayload.ThreadInvited(thread).thread()).isSameAs(thread);
        assertThat(new CommunityThreadRealtimePayload.ThreadUpdated(
            "1", "제목", null, CommunityThreadCategory.FREE, "💬", 1, 10,
            Instant.EPOCH, Instant.EPOCH
        ).threadId()).isEqualTo("1");
        assertThat(new CommunityThreadRealtimePayload.MemberKicked(1L, 0).memberId()).isEqualTo(1L);
        assertThat(new CommunityThreadRealtimePayload.MemberLeft(1L, 0).memberId()).isEqualTo(1L);

        assertThatThrownBy(() -> new CommunityThreadRealtimePayload.MessageCreated(null, null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CommunityThreadRealtimePayload.MessageUpdated(null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CommunityThreadRealtimePayload.MessageDeleted(null))
            .isInstanceOf(NullPointerException.class);
        assertInvalid(() -> new CommunityThreadRealtimePayload.ReactionChanged(0L, List.of()));
        assertThatThrownBy(() -> new CommunityThreadRealtimePayload.ReactionChanged(1L, null))
            .isInstanceOf(NullPointerException.class);
        assertInvalid(() -> new CommunityThreadRealtimePayload.ReadUpdated(0L, 1L));
        assertInvalid(() -> new CommunityThreadRealtimePayload.ReadUpdated(1L, 0L));
        assertThatThrownBy(() -> new CommunityThreadRealtimePayload.ThreadInvited(null))
            .isInstanceOf(NullPointerException.class);
        assertInvalid(() -> new CommunityThreadRealtimePayload.MemberKicked(0L, 0));
        assertInvalid(() -> new CommunityThreadRealtimePayload.MemberLeft(0L, 0));
    }

    private CommunityThreadMessageInfo message() {
        return new CommunityThreadMessageInfo(
            1L, 1L, 1L, "작성자", "본문", CommunityThreadMessageType.TEXT,
            CommunityThreadMessageStatus.SENT, List.of(), List.of(), null, List.of(),
            COMMAND_ID, Instant.EPOCH, null, null
        );
    }

    private ThreadSummaryInfo thread() {
        return new ThreadSummaryInfo(
            1L, "제목", null, CommunityThreadCategory.FREE, "💬", 1, 0, 10,
            false, false, CommunityThreadMemberRole.OWNER, null, 1L, Instant.EPOCH, Instant.EPOCH
        );
    }

    private void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(IllegalArgumentException.class);
    }
}
