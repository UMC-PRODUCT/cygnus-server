package com.umc.product.community.application.service.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageForRecipientsUseCase;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEvent;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEventType;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimePayload;
import com.umc.product.community.application.port.out.realtime.CommunityThreadRealtimeBroadcastPort;
import com.umc.product.community.application.port.out.thread.CommunityThreadQueryPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Reason;
import com.umc.product.community.domain.CommunityThreadProperties;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community thread realtime delivery")
class CommunityThreadRealtimeDeliveryTest {

    private static final UUID EVENT_ID = UUID.fromString("12ce4b92-2912-49cf-b66e-48ce5d541428");

    @Mock
    LoadCommunityThreadPort loadThreadPort;
    @Mock
    CommunityThreadQueryPort threadQueryPort;
    @Mock
    GetCommunityThreadMessageForRecipientsUseCase getMessageForRecipientsUseCase;
    @Mock
    GetCommunityThreadDetailUseCase getThreadDetailUseCase;
    @Mock
    CommunityThreadRealtimeBroadcastPort broadcastPort;
    @Mock
    CommunityThreadProperties properties;
    @Mock
    CommunityThreadRealtimeMetrics metrics;

    @Captor
    ArgumentCaptor<CommunityThreadRealtimeEvent<?>> eventCaptor;

    @InjectMocks
    CommunityThreadRealtimeDelivery sut;

    @Test
    @DisplayName("일부 broker 전송이 실패해도 모든 recipient를 시도한 뒤 실패를 집계해 던진다")
    void partialFailureAttemptsEveryRecipientBeforeRethrow() {
        CommunityThreadRealtimeEvent<?> event = event();
        willAnswer(invocation -> {
            Long memberId = invocation.getArgument(1);
            if (Long.valueOf(20L).equals(memberId)) {
                throw new IllegalStateException("broker unavailable");
            }
            return null;
        }).given(broadcastPort).broadcastToThreadMember(eq(11L), any(Long.class), eq(event));

        assertThatThrownBy(() -> sut.fanOutThreadMembers(
            11L,
            List.of(10L, 20L, 30L),
            Operation.MESSAGE_CREATED,
            ignored -> event
        ))
            .isInstanceOf(IllegalStateException.class)
            .satisfies(exception -> assertThat(exception.getSuppressed()).hasSize(1));

        then(broadcastPort).should().broadcastToThreadMember(11L, 10L, event);
        then(broadcastPort).should().broadcastToThreadMember(11L, 20L, event);
        then(broadcastPort).should().broadcastToThreadMember(11L, 30L, event);
        then(metrics).should().recordBroadcastFailure(
            Operation.MESSAGE_CREATED,
            Reason.BROKER_UNAVAILABLE
        );
        then(metrics).should().recordFanOut(Operation.MESSAGE_CREATED, Outcome.FAILURE, 3);
    }

    @Test
    @DisplayName("모든 recipient는 retry dedup에 사용하는 동일한 stable eventId를 받는다")
    void allRecipientsReceiveTheSameStableEventId() {
        CommunityThreadRealtimeEvent<?> event = event();

        sut.fanOutThreadMembers(
            11L,
            List.of(10L, 20L),
            Operation.READ_UPDATED,
            ignored -> event
        );

        then(broadcastPort).should(times(2)).broadcastToThreadMember(
            eq(11L),
            any(Long.class),
            eventCaptor.capture()
        );
        assertThat(eventCaptor.getAllValues())
            .extracting(CommunityThreadRealtimeEvent::eventId)
            .containsOnly(EVENT_ID);
        then(metrics).should().recordFanOut(Operation.READ_UPDATED, Outcome.SUCCESS, 2);
    }

    private CommunityThreadRealtimeEvent<?> event() {
        return CommunityThreadRealtimeEvent.of(
            EVENT_ID,
            CommunityThreadRealtimeEventType.READ_UPDATED,
            11L,
            Instant.parse("2026-07-18T00:00:00Z"),
            new CommunityThreadRealtimePayload.ReadUpdated(10L, 900L)
        );
    }
}
