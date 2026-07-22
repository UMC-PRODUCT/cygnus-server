package com.umc.product.community.application.service.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadDetailInfo;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageForRecipientsUseCase;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEvent;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEventType;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimePayload;
import com.umc.product.community.application.port.out.realtime.CommunityThreadRealtimeBroadcastPort;
import com.umc.product.community.application.port.out.thread.CommunityThreadQueryPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.event.CommunityThreadDeletedEvent;
import com.umc.product.community.domain.event.CommunityThreadInvitedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberKickedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberLeftEvent;
import com.umc.product.community.domain.event.CommunityThreadUpdatedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community thread lifecycle realtime relay")
class CommunityThreadLifecycleRealtimeRelayTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    LoadCommunityThreadPort loadThreadPort;
    @Mock
    LoadCommunityThreadMemberPort loadMemberPort;
    @Mock
    CommunityThreadQueryPort threadQueryPort;
    @Mock
    GetCommunityThreadMessageForRecipientsUseCase getMessageForRecipientsUseCase;
    @Mock
    GetCommunityThreadDetailUseCase getThreadDetailUseCase;
    @Mock
    CommunityThreadRealtimeBroadcastPort broadcastPort;
    @Mock
    CommunityThreadRealtimeMetrics metrics;

    @Captor
    ArgumentCaptor<CommunityThreadRealtimeEvent<?>> eventCaptor;

    CommunityThreadLifecycleRealtimeRelay sut;

    @BeforeEach
    void setUp() {
        CommunityThreadRealtimeDelivery delivery = new CommunityThreadRealtimeDelivery(
            loadThreadPort,
            threadQueryPort,
            getMessageForRecipientsUseCase,
            getThreadDetailUseCase,
            broadcastPort,
            new CommunityThreadProperties(100),
            metrics
        );
        sut = new CommunityThreadLifecycleRealtimeRelay(delivery, loadMemberPort);
    }

    @Test
    @DisplayName("thread.updated는 event snapshot이 아닌 delivery-time ACTIVE audience에만 fan-out한다")
    void threadUpdatedUsesDeliveryTimeActiveAudience() {
        CommunityThread thread = thread();
        given(loadThreadPort.findById(11L)).willReturn(Optional.of(thread));
        given(threadQueryPort.listActiveMemberIdsByThreadId(11L, 101))
            .willReturn(List.of(10L, 30L));
        CommunityThreadUpdatedEvent event = CommunityThreadUpdatedEvent.of(
            11L,
            10L,
            NOW
        );

        sut.relay(event);

        ArgumentCaptor<Long> memberCaptor = ArgumentCaptor.forClass(Long.class);
        then(broadcastPort).should(times(2)).broadcastToMember(
            memberCaptor.capture(),
            eventCaptor.capture()
        );
        assertThat(memberCaptor.getAllValues()).containsExactly(10L, 30L);
        assertThat(eventCaptor.getAllValues())
            .extracting(CommunityThreadRealtimeEvent::eventId)
            .containsOnly(event.eventId());
        CommunityThreadRealtimePayload.ThreadUpdated payload =
            (CommunityThreadRealtimePayload.ThreadUpdated) eventCaptor.getValue().payload();
        assertThat(payload.memberCount()).isEqualTo(2L);
        then(metrics).should().recordFanOut(Operation.THREAD_UPDATED, Outcome.SUCCESS, 2);
    }

    @Test
    @DisplayName("member.kicked는 전환 전 snapshot으로 대상 멤버에게도 terminal event를 한 번 보낸다")
    void memberKickedUsesPreTransitionAudienceIncludingAffectedMember() {
        CommunityThreadMemberKickedEvent event = CommunityThreadMemberKickedEvent.of(
            11L,
            10L,
            20L,
            List.of(10L, 20L, 30L),
            NOW
        );

        sut.relay(event);

        ArgumentCaptor<Long> memberCaptor = ArgumentCaptor.forClass(Long.class);
        then(broadcastPort).should(times(3)).broadcastToMember(
            memberCaptor.capture(),
            eventCaptor.capture()
        );
        assertThat(memberCaptor.getAllValues()).containsExactly(10L, 20L, 30L);
        assertThat(memberCaptor.getAllValues()).containsOnlyOnce(20L);
        assertThat(eventCaptor.getAllValues())
            .extracting(CommunityThreadRealtimeEvent::type)
            .containsOnly(CommunityThreadRealtimeEventType.MEMBER_KICKED);
        CommunityThreadRealtimePayload.MemberKicked payload =
            (CommunityThreadRealtimePayload.MemberKicked) eventCaptor.getValue().payload();
        assertThat(payload.memberId()).isEqualTo(20L);
        assertThat(payload.memberCount()).isEqualTo(2L);
        then(threadQueryPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("재가입한 epoch가 다시 LEFT여도 지연된 과거 member.left 전체를 건너뛴다")
    void delayedMemberLeftAfterRejoinAndSecondLeaveIsSkipped() {
        CommunityThreadMember currentMembership =
            CommunityThreadMember.createMember(11L, 20L, NOW.minusSeconds(60));
        Instant previousMembershipJoinedAt = currentMembership.getJoinedAt();
        currentMembership.leave(NOW);
        currentMembership.rejoin(NOW);
        currentMembership.leave(NOW.plusSeconds(60));
        given(loadMemberPort.findByThreadIdAndMemberIdForUpdate(11L, 20L))
            .willReturn(Optional.of(currentMembership));
        CommunityThreadMemberLeftEvent delayedEvent = memberLeftEvent(
            previousMembershipJoinedAt,
            NOW
        );

        sut.relay(delayedEvent);

        then(broadcastPort).shouldHaveNoInteractions();
        then(metrics).should().recordFanOut(Operation.MEMBER_LEFT, Outcome.SKIPPED, 0);
    }

    @Test
    @DisplayName("현재 membership을 확인할 수 없으면 member.left를 fail-safe로 건너뛴다")
    void memberLeftWithoutCurrentMembershipIsSkipped() {
        given(loadMemberPort.findByThreadIdAndMemberIdForUpdate(11L, 20L)).willReturn(Optional.empty());

        sut.relay(memberLeftEvent(NOW.minusSeconds(60), NOW));

        then(broadcastPort).shouldHaveNoInteractions();
        then(metrics).should().recordFanOut(Operation.MEMBER_LEFT, Outcome.SKIPPED, 0);
    }

    @Test
    @DisplayName("가입과 탈퇴 시각이 같아도 현재 epoch의 member.left를 전환 전 snapshot 전체에 전달한다")
    void currentEpochMemberLeftWithEqualTimestampsUsesPreTransitionAudience() {
        CommunityThreadMember currentMembership =
            CommunityThreadMember.createMember(11L, 20L, NOW);
        currentMembership.leave(NOW);
        given(loadMemberPort.findByThreadIdAndMemberIdForUpdate(11L, 20L))
            .willReturn(Optional.of(currentMembership));

        sut.relay(memberLeftEvent(NOW, NOW));

        then(loadMemberPort).should().findByThreadIdAndMemberIdForUpdate(11L, 20L);
        then(loadMemberPort).shouldHaveNoMoreInteractions();
        then(broadcastPort).should(times(3))
            .broadcastToMember(any(Long.class), any());
        then(metrics).should().recordFanOut(Operation.MEMBER_LEFT, Outcome.SUCCESS, 3);
    }

    @Test
    @DisplayName("thread.invited는 초대 대상마다 user destination과 viewer별 summary를 사용한다")
    void threadInvitedUsesUserDestinations() {
        CommunityThread thread = thread();
        given(loadThreadPort.findById(11L)).willReturn(Optional.of(thread));
        given(threadQueryPort.listActiveMemberIdsByThreadId(11L, 101))
            .willReturn(List.of(10L, 20L, 30L));
        given(getThreadDetailUseCase.getThread(any())).willReturn(threadDetail());
        CommunityThreadInvitedEvent event = CommunityThreadInvitedEvent.of(
            11L,
            10L,
            List.of(20L, 30L),
            NOW
        );

        sut.relay(event);

        then(broadcastPort).should().broadcastToMember(eq(20L), any());
        then(broadcastPort).should().broadcastToMember(eq(30L), any());
        then(broadcastPort).shouldHaveNoMoreInteractions();
        then(metrics).should().recordFanOut(Operation.THREAD_INVITED, Outcome.SUCCESS, 2);
    }

    @Test
    @DisplayName("지연된 thread.invited는 현재 ACTIVE가 아닌 과거 초대 대상을 건너뛴다")
    void delayedThreadInvitedSkipsStaleInviteeAndDeliversToActiveInvitee() {
        CommunityThread thread = thread();
        given(loadThreadPort.findById(11L)).willReturn(Optional.of(thread));
        given(threadQueryPort.listActiveMemberIdsByThreadId(11L, 101))
            .willReturn(List.of(10L, 20L));
        given(getThreadDetailUseCase.getThread(any())).willReturn(threadDetail());
        CommunityThreadInvitedEvent event = CommunityThreadInvitedEvent.of(
            11L,
            10L,
            List.of(20L, 30L),
            NOW
        );

        sut.relay(event);

        then(broadcastPort).should().broadcastToMember(eq(20L), any());
        then(broadcastPort).shouldHaveNoMoreInteractions();
        then(metrics).should().recordFanOut(Operation.THREAD_INVITED, Outcome.SUCCESS, 1);
    }

    @Test
    @DisplayName("thread.deleted는 삭제 전 ACTIVE snapshot 전체에 terminal event를 전송한다")
    void threadDeletedUsesPreDeleteAudience() {
        CommunityThreadDeletedEvent event = CommunityThreadDeletedEvent.of(
            11L,
            10L,
            List.of(10L, 20L, 30L),
            NOW
        );

        sut.relay(event);

        ArgumentCaptor<Long> memberCaptor = ArgumentCaptor.forClass(Long.class);
        then(broadcastPort).should(times(3)).broadcastToMember(
            memberCaptor.capture(),
            eventCaptor.capture()
        );
        assertThat(memberCaptor.getAllValues()).containsExactly(10L, 20L, 30L);
        assertThat(eventCaptor.getAllValues())
            .extracting(CommunityThreadRealtimeEvent::eventId)
            .containsOnly(event.eventId());
        CommunityThreadRealtimePayload.ThreadDeleted payload =
            (CommunityThreadRealtimePayload.ThreadDeleted) eventCaptor.getValue().payload();
        assertThat(payload.threadId()).isEqualTo("11");
        assertThat(payload.deletedAt()).isEqualTo(NOW);
        then(metrics).should().recordFanOut(Operation.THREAD_DELETED, Outcome.SUCCESS, 3);
    }

    private CommunityThread thread() {
        CommunityThread thread = CommunityThread.create(
            101L,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "chat",
            10L,
            NOW
        );
        ReflectionTestUtils.setField(thread, "id", 11L);
        ReflectionTestUtils.setField(thread, "createdAt", NOW);
        ReflectionTestUtils.setField(thread, "updatedAt", NOW);
        return thread;
    }

    private CommunityThreadMemberLeftEvent memberLeftEvent(
        Instant membershipJoinedAt,
        Instant occurredAt
    ) {
        return CommunityThreadMemberLeftEvent.of(
            11L,
            20L,
            List.of(10L, 20L, 30L),
            membershipJoinedAt,
            occurredAt
        );
    }

    private ThreadDetailInfo threadDetail() {
        return new ThreadDetailInfo(
            11L,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "chat",
            3L,
            0L,
            100,
            false,
            false,
            CommunityThreadMemberRole.MEMBER,
            null,
            10L,
            NOW,
            NOW,
            "/api/v1/community/threads/11",
            null
        );
    }
}
