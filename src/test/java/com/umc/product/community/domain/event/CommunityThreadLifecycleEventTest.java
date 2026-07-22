package com.umc.product.community.domain.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.OutboxDispatchMode;

@DisplayName("Community thread lifecycle event")
class CommunityThreadLifecycleEventTest {

    @Test
    @DisplayName("초대 ID snapshot은 정렬·중복 제거된 불변 값으로 보관한다")
    void invitedEvent_keepsImmutableSortedDistinctIds() {
        // given & when
        CommunityThreadInvitedEvent event = CommunityThreadInvitedEvent.of(
            1L,
            10L,
            List.of(30L, 20L, 30L),
            Instant.parse("2026-07-18T00:00:00Z")
        );

        // then
        assertThat(event.invitedMemberIds()).containsExactly(20L, 30L);
        assertThat(event.eventType()).isEqualTo("community.thread.invited");
        assertThat(event.outboxDispatchMode()).isEqualTo(OutboxDispatchMode.NON_TRANSACTIONAL);
        assertThatThrownBy(() -> event.invitedMemberIds().add(40L))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("member.left outbox payload는 탈퇴 membership epoch를 직렬화하고 복원한다")
    void memberLeftEvent_roundTripsMembershipEpoch() throws Exception {
        // given
        Instant membershipJoinedAt = Instant.parse("2026-07-17T23:59:00Z");
        CommunityThreadMemberLeftEvent event = CommunityThreadMemberLeftEvent.of(
            1L,
            10L,
            List.of(10L, 20L),
            membershipJoinedAt,
            Instant.parse("2026-07-18T00:00:00Z")
        );
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        EventPayloadDeserializer deserializer = new EventPayloadDeserializer(objectMapper);

        // when
        String payload = serializer.serialize(event);
        EventOutbox outbox = EventOutbox.record(event, payload);
        CommunityThreadMemberLeftEvent restored =
            (CommunityThreadMemberLeftEvent) deserializer.deserialize(outbox);

        // then
        assertThat(objectMapper.readTree(payload).hasNonNull("membershipJoinedAt")).isTrue();
        assertThat(restored.membershipJoinedAt()).isEqualTo(membershipJoinedAt);
        assertThat(restored).isEqualTo(event);
    }

    @Test
    @DisplayName("delete·kick·update lifecycle event는 타입과 정렬된 recipient snapshot을 보존한다")
    void exposesLifecycleEventTypes() {
        Instant occurredAt = Instant.parse("2026-07-18T00:00:00Z");
        CommunityThreadDeletedEvent deleted = CommunityThreadDeletedEvent.of(
            1L, 2L, List.of(4L, 3L, 4L), occurredAt);
        CommunityThreadMemberKickedEvent kicked = CommunityThreadMemberKickedEvent.of(
            1L, 2L, 3L, List.of(4L), occurredAt);
        CommunityThreadUpdatedEvent updated = CommunityThreadUpdatedEvent.of(1L, 2L, occurredAt);

        assertThat(deleted.eventType()).isEqualTo("community.thread.deleted");
        assertThat(deleted.activeMemberIds()).containsExactly(3L, 4L);
        assertThat(kicked.eventType()).isEqualTo("community.thread.member.kicked");
        assertThat(updated.eventType()).isEqualTo("community.thread.updated");
    }

    @Test
    @DisplayName("lifecycle event는 null snapshot과 non-positive ID를 거부한다")
    void rejectsInvalidLifecycleEventIdentity() {
        Instant occurredAt = Instant.parse("2026-07-18T00:00:00Z");
        assertThatThrownBy(() -> new CommunityThreadDeletedEvent(
            java.util.UUID.randomUUID(), occurredAt, 0L, 2L, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CommunityThreadDeletedEvent(
            java.util.UUID.randomUUID(), occurredAt, 1L, 2L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
