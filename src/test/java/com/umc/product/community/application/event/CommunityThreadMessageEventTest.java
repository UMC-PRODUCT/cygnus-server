package com.umc.product.community.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.event.domain.DomainEvent;

@DisplayName("커뮤니티 스레드 메시지 도메인 이벤트")
class CommunityThreadMessageEventTest {

    private static final List<String> FORBIDDEN_PAYLOAD_PROPERTIES = List.of(
        "property",
        "text",
        "title",
        "name",
        "token",
        "deeplink",
        "provider",
        "mute",
        "offline",
        "dnd"
    );

    @Test
    @DisplayName("메시지 생성 이벤트는 수신자 ID를 정렬·중복 제거한 불변 스냅샷으로 보관한다")
    void 메시지_생성_이벤트는_수신자_ID를_정렬하고_불변으로_보관한다() {
        List<Long> mutableRecipientIds = new ArrayList<>(List.of(30L, 10L, 30L, 20L));

        CommunityThreadMessageCreatedEvent event =
            CommunityThreadMessageCreatedEvent.of(1L, 2L, 3L, mutableRecipientIds);

        mutableRecipientIds.add(40L);

        assertThat(event.recipientMemberIds()).containsExactly(10L, 20L, 30L);
        assertThatThrownBy(() -> event.recipientMemberIds().add(40L))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(event.eventType()).isEqualTo("community.thread.message.created");
    }

    @Test
    @DisplayName("멘션 이벤트는 멘션 ID를 정렬·중복 제거한 불변 스냅샷으로 보관한다")
    void 멘션_이벤트는_멘션_ID를_정렬하고_불변으로_보관한다() {
        List<Long> mutableMentionedIds = new ArrayList<>(List.of(50L, 40L, 50L, 40L, 60L));

        CommunityThreadMentionedEvent event =
            CommunityThreadMentionedEvent.of(1L, 2L, 3L, mutableMentionedIds);

        mutableMentionedIds.clear();

        assertThat(event.mentionedMemberIds()).containsExactly(40L, 50L, 60L);
        assertThatThrownBy(() -> event.mentionedMemberIds().remove(40L))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(event.eventType()).isEqualTo("community.thread.message.mentioned");
    }

    @Test
    @DisplayName("두 이벤트의 기본 팩토리는 UUID와 발생 시각을 채운다")
    void 기본_팩토리는_UUID와_발생시각을_채운다() {
        CommunityThreadMessageCreatedEvent createdEvent =
            CommunityThreadMessageCreatedEvent.of(1L, 2L, 3L, List.of());
        CommunityThreadMentionedEvent mentionedEvent =
            CommunityThreadMentionedEvent.of(1L, 2L, 3L, List.of());

        assertThat(createdEvent.eventId()).isNotNull();
        assertThat(createdEvent.occurredAt()).isNotNull();
        assertThat(mentionedEvent.eventId()).isNotNull();
        assertThat(mentionedEvent.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("이벤트는 도메인 이벤트의 공통 메타데이터와 ID 전용 페이로드만 노출한다")
    void 이벤트_페이로드는_ID와_공통_메타데이터만_노출한다() {
        assertThat(CommunityThreadMessageCreatedEvent.class).isRecord();
        assertThat(CommunityThreadMentionedEvent.class).isRecord();
        assertThat(DomainEvent.class.isAssignableFrom(CommunityThreadMessageCreatedEvent.class)).isTrue();
        assertThat(DomainEvent.class.isAssignableFrom(CommunityThreadMentionedEvent.class)).isTrue();

        assertThat(recordComponentNames(CommunityThreadMessageCreatedEvent.class))
            .containsExactly(
                "eventId",
                "occurredAt",
                "threadId",
                "messageId",
                "senderMemberId",
                "recipientMemberIds"
            )
            .doesNotContainAnyElementsOf(FORBIDDEN_PAYLOAD_PROPERTIES);
        assertThat(recordComponentNames(CommunityThreadMentionedEvent.class))
            .containsExactly(
                "eventId",
                "occurredAt",
                "threadId",
                "messageId",
                "senderMemberId",
                "mentionedMemberIds"
            )
            .doesNotContainAnyElementsOf(FORBIDDEN_PAYLOAD_PROPERTIES);
    }

    @Test
    @DisplayName("명시한 이벤트 식별자와 발생 시각은 기본 생성 시 덮어쓰지 않는다")
    void 명시한_이벤트_메타데이터를_보존한다() {
        UUID eventId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant occurredAt = Instant.parse("2026-07-18T00:00:00Z");

        CommunityThreadMessageCreatedEvent createdEvent = new CommunityThreadMessageCreatedEvent(
            eventId,
            occurredAt,
            1L,
            2L,
            3L,
            List.of(30L, 10L, 30L)
        );
        CommunityThreadMentionedEvent mentionedEvent = new CommunityThreadMentionedEvent(
            eventId,
            occurredAt,
            1L,
            2L,
            3L,
            List.of(50L, 40L, 50L)
        );

        assertThat(createdEvent.eventId()).isEqualTo(eventId);
        assertThat(createdEvent.occurredAt()).isEqualTo(occurredAt);
        assertThat(createdEvent.recipientMemberIds()).containsExactly(10L, 30L);
        assertThat(mentionedEvent.eventId()).isEqualTo(eventId);
        assertThat(mentionedEvent.occurredAt()).isEqualTo(occurredAt);
        assertThat(mentionedEvent.mentionedMemberIds()).containsExactly(40L, 50L);
    }

    private static List<String> recordComponentNames(Class<?> eventType) {
        return Arrays.stream(eventType.getRecordComponents())
            .map(component -> component.getName())
            .toList();
    }
}
