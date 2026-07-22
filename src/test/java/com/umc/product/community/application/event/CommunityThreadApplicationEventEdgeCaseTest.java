package com.umc.product.community.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Community thread application event 경계")
class CommunityThreadApplicationEventEdgeCaseTest {

    @Test
    @DisplayName("created·mentioned event는 null recipient를 비우고 ID를 정렬·중복 제거한다")
    void normalizesRecipientSnapshots() {
        CommunityThreadMessageCreatedEvent created =
            CommunityThreadMessageCreatedEvent.of(1L, 2L, 3L, null);
        CommunityThreadMentionedEvent mentioned =
            CommunityThreadMentionedEvent.of(1L, 2L, 3L, List.of(5L, 4L, 5L));
        CommunityThreadMessageCreatedEvent explicitCreated = new CommunityThreadMessageCreatedEvent(
            UUID.randomUUID(), Instant.EPOCH, 1L, 2L, 3L, List.of(5L, 4L, 5L));
        CommunityThreadMentionedEvent emptyMentioned = new CommunityThreadMentionedEvent(
            UUID.randomUUID(), Instant.EPOCH, 1L, 2L, 3L, null);

        assertThat(created.recipientMemberIds()).isEmpty();
        assertThat(created.eventType()).isEqualTo("community.thread.message.created");
        assertThat(explicitCreated.recipientMemberIds()).containsExactly(4L, 5L);
        assertThat(mentioned.mentionedMemberIds()).containsExactly(4L, 5L);
        assertThat(emptyMentioned.mentionedMemberIds()).isEmpty();
        assertThat(mentioned.eventType()).isEqualTo("community.thread.message.mentioned");
    }

    @Test
    @DisplayName("created·mentioned event는 aggregate·message·sender·recipient ID를 양수로 강제한다")
    void rejectsInvalidEventIds() {
        assertInvalid(() -> CommunityThreadMessageCreatedEvent.of(0L, 2L, 3L, List.of()));
        assertInvalid(() -> CommunityThreadMessageCreatedEvent.of(1L, 0L, 3L, List.of()));
        assertInvalid(() -> CommunityThreadMessageCreatedEvent.of(1L, 2L, 0L, List.of()));
        assertInvalid(() -> CommunityThreadMessageCreatedEvent.of(
            1L, 2L, 3L, Arrays.asList(4L, null)));
        assertInvalid(() -> CommunityThreadMentionedEvent.of(0L, 2L, 3L, List.of()));
        assertInvalid(() -> CommunityThreadMentionedEvent.of(1L, 0L, 3L, List.of()));
        assertInvalid(() -> CommunityThreadMentionedEvent.of(1L, 2L, 0L, List.of()));
        assertInvalid(() -> CommunityThreadMentionedEvent.of(1L, 2L, 3L, List.of(0L)));
    }

    private void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(IllegalArgumentException.class);
    }
}
