package com.umc.product.community.application.service.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.chat.domain.event.ChatMessageDeletedEvent;
import com.umc.product.chat.domain.event.ChatMessageReactionChangedEvent;
import com.umc.product.chat.domain.event.ChatMessageSnapshot;
import com.umc.product.chat.domain.event.ChatMessageUpdatedEvent;
import com.umc.product.chat.domain.event.ChatReadUpdatedEvent;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.message.GetCommunityThreadMessageForRecipientsUseCase;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageRecipientsQuery;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageStatus;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionInfo;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEvent;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEventType;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimePayload;
import com.umc.product.community.application.port.out.realtime.CommunityThreadRealtimeBroadcastPort;
import com.umc.product.community.application.port.out.thread.CommunityThreadQueryPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Operation;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics.Outcome;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadCategory;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community thread Chat realtime relay")
class CommunityThreadChatRealtimeRelayTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("2ec04088-5063-4214-bacd-63339553a389");
    private static final UUID CLIENT_MESSAGE_ID =
        UUID.fromString("bdaaf6cb-c753-4e86-a05f-c733c53447b0");

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
    CommunityThreadRealtimeMetrics metrics;

    @Captor
    ArgumentCaptor<CommunityThreadRealtimeEvent<?>> eventCaptor;

    CommunityThreadChatRealtimeRelay sut;

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
        sut = new CommunityThreadChatRealtimeRelay(delivery);
    }

    @Test
    @DisplayName("message.created는 delivery-time ACTIVE 멤버별 payload로 변환하고 source eventId를 보존한다")
    void messageCreatedUsesDeliveryTimeAudienceAndStableEventId() {
        CommunityThread thread = thread();
        CommunityThreadMessageInfo ownerMessage = message(true);
        CommunityThreadMessageInfo memberMessage = message(false);
        given(loadThreadPort.findByChatRoomId(101L)).willReturn(Optional.of(thread));
        given(threadQueryPort.listActiveMemberIdsByThreadId(11L, 101))
            .willReturn(List.of(10L, 20L));
        given(getMessageForRecipientsUseCase.getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(11L, 900L, List.of(10L, 20L))
        )).willReturn(Map.of(10L, ownerMessage, 20L, memberMessage));
        ChatMessageCreatedEvent event = new ChatMessageCreatedEvent(
            EVENT_ID,
            NOW,
            900L,
            101L,
            10L,
            MessageContentType.TEXT,
            "메시지",
            List.of(),
            null,
            CLIENT_MESSAGE_ID,
            List.of(),
            null,
            null
        );

        sut.relay(event);

        ArgumentCaptor<Long> memberCaptor = ArgumentCaptor.forClass(Long.class);
        then(broadcastPort).should(times(2)).broadcastToMember(
            memberCaptor.capture(),
            eventCaptor.capture()
        );
        assertThat(memberCaptor.getAllValues()).containsExactly(10L, 20L);
        assertThat(eventCaptor.getAllValues())
            .extracting(CommunityThreadRealtimeEvent::eventId)
            .containsOnly(EVENT_ID);
        assertThat(eventCaptor.getAllValues())
            .extracting(CommunityThreadRealtimeEvent::threadId)
            .containsOnly("11");
        assertThat(eventCaptor.getAllValues())
            .extracting(CommunityThreadRealtimeEvent::type)
            .containsOnly(CommunityThreadRealtimeEventType.MESSAGE_CREATED);
        CommunityThreadRealtimePayload.MessageCreated ownerPayload =
            (CommunityThreadRealtimePayload.MessageCreated) eventCaptor.getAllValues().get(0).payload();
        CommunityThreadRealtimePayload.MessageCreated memberPayload =
            (CommunityThreadRealtimePayload.MessageCreated) eventCaptor.getAllValues().get(1).payload();
        assertThat(ownerPayload.message()).isSameAs(ownerMessage);
        assertThat(ownerPayload.message().reactions().get(0).reactedByMe()).isTrue();
        assertThat(memberPayload.message()).isSameAs(memberMessage);
        assertThat(memberPayload.message().reactions().get(0).reactedByMe()).isFalse();
        assertThat(ownerPayload.clientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);
        then(getMessageForRecipientsUseCase).should().getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(11L, 900L, List.of(10L, 20L))
        );
        then(metrics).should().recordFanOut(Operation.MESSAGE_CREATED, Outcome.SUCCESS, 2);
    }

    @Test
    @DisplayName("reaction.changed도 수신자 message snapshot을 한 번만 준비하고 개인화 reaction을 전송한다")
    void reactionChangedPreparesPersonalizedMessagesOnce() {
        given(loadThreadPort.findByChatRoomId(101L)).willReturn(Optional.of(thread()));
        given(threadQueryPort.listActiveMemberIdsByThreadId(11L, 101))
            .willReturn(List.of(10L, 20L));
        given(getMessageForRecipientsUseCase.getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(11L, 900L, List.of(10L, 20L))
        )).willReturn(Map.of(10L, message(true), 20L, message(false)));
        ChatMessageReactionChangedEvent event = new ChatMessageReactionChangedEvent(
            EVENT_ID,
            NOW,
            101L,
            900L,
            10L,
            "👍",
            true
        );

        sut.relay(event);

        then(getMessageForRecipientsUseCase).should().getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(11L, 900L, List.of(10L, 20L))
        );
        then(broadcastPort).should(times(2)).broadcastToMember(
            any(Long.class),
            eventCaptor.capture()
        );
        CommunityThreadRealtimePayload.ReactionChanged ownerPayload =
            (CommunityThreadRealtimePayload.ReactionChanged) eventCaptor.getAllValues().get(0).payload();
        CommunityThreadRealtimePayload.ReactionChanged memberPayload =
            (CommunityThreadRealtimePayload.ReactionChanged) eventCaptor.getAllValues().get(1).payload();
        assertThat(ownerPayload.reactions().get(0).reactedByMe()).isTrue();
        assertThat(memberPayload.reactions().get(0).reactedByMe()).isFalse();
        assertThat(eventCaptor.getAllValues())
            .extracting(CommunityThreadRealtimeEvent::eventId)
            .containsOnly(EVENT_ID);
    }

    @Test
    @DisplayName("message.updated와 message.deleted는 수신자별 최신 snapshot을 대응 payload로 전송한다")
    void messageUpdatedAndDeletedUsePersonalizedSnapshots() {
        CommunityThread thread = thread();
        CommunityThreadMessageInfo ownerMessage = message(true);
        CommunityThreadMessageInfo memberMessage = message(false);
        Map<Long, CommunityThreadMessageInfo> messages = Map.of(
            10L, ownerMessage,
            20L, memberMessage
        );
        given(loadThreadPort.findByChatRoomId(101L)).willReturn(Optional.of(thread));
        given(threadQueryPort.listActiveMemberIdsByThreadId(11L, 101))
            .willReturn(List.of(10L, 20L));
        given(getMessageForRecipientsUseCase.getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(11L, 900L, List.of(10L, 20L))
        )).willReturn(messages);
        ChatMessageSnapshot snapshot = snapshot();

        sut.relay(new ChatMessageUpdatedEvent(EVENT_ID, NOW, snapshot));
        sut.relay(new ChatMessageDeletedEvent(EVENT_ID, NOW, snapshot));

        then(broadcastPort).should(times(4)).broadcastToMember(
            any(Long.class),
            eventCaptor.capture()
        );
        assertThat(eventCaptor.getAllValues())
            .extracting(CommunityThreadRealtimeEvent::type)
            .containsExactly(
                CommunityThreadRealtimeEventType.MESSAGE_UPDATED,
                CommunityThreadRealtimeEventType.MESSAGE_UPDATED,
                CommunityThreadRealtimeEventType.MESSAGE_DELETED,
                CommunityThreadRealtimeEventType.MESSAGE_DELETED
            );
        CommunityThreadRealtimePayload.MessageUpdated updated =
            (CommunityThreadRealtimePayload.MessageUpdated) eventCaptor.getAllValues().get(0).payload();
        CommunityThreadRealtimePayload.MessageDeleted deleted =
            (CommunityThreadRealtimePayload.MessageDeleted) eventCaptor.getAllValues().get(3).payload();
        assertThat(updated.message()).isSameAs(ownerMessage);
        assertThat(deleted.message()).isSameAs(memberMessage);
        then(metrics).should().recordFanOut(Operation.MESSAGE_UPDATED, Outcome.SUCCESS, 2);
        then(metrics).should().recordFanOut(Operation.MESSAGE_DELETED, Outcome.SUCCESS, 2);
    }

    @Test
    @DisplayName("read.updated는 한 번 만든 동일 event를 delivery-time ACTIVE audience에 전송한다")
    void readUpdatedUsesOneStableEnvelope() {
        given(loadThreadPort.findByChatRoomId(101L)).willReturn(Optional.of(thread()));
        given(threadQueryPort.listActiveMemberIdsByThreadId(11L, 101))
            .willReturn(List.of(10L, 20L));

        sut.relay(new ChatReadUpdatedEvent(EVENT_ID, NOW, 101L, 10L, 900L));

        then(broadcastPort).should(times(2)).broadcastToMember(
            any(Long.class),
            eventCaptor.capture()
        );
        assertThat(eventCaptor.getAllValues()).containsOnly(eventCaptor.getValue());
        CommunityThreadRealtimePayload.ReadUpdated payload =
            (CommunityThreadRealtimePayload.ReadUpdated) eventCaptor.getValue().payload();
        assertThat(payload.memberId()).isEqualTo(10L);
        assertThat(payload.lastReadMessageId()).isEqualTo(900L);
        then(metrics).should().recordFanOut(Operation.READ_UPDATED, Outcome.SUCCESS, 2);
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
        return thread;
    }

    private CommunityThreadMessageInfo message(boolean reactedByMe) {
        return new CommunityThreadMessageInfo(
            900L,
            11L,
            10L,
            "작성자",
            "메시지",
            CommunityThreadMessageType.TEXT,
            CommunityThreadMessageStatus.SENT,
            List.of(),
            List.of(),
            null,
            List.of(new CommunityThreadReactionInfo("👍", 1L, reactedByMe)),
            CLIENT_MESSAGE_ID,
            NOW,
            null,
            null
        );
    }

    private ChatMessageSnapshot snapshot() {
        return new ChatMessageSnapshot(
            900L,
            101L,
            10L,
            MessageContentType.TEXT,
            "메시지",
            List.of(),
            null,
            CLIENT_MESSAGE_ID,
            List.of(),
            NOW,
            NOW,
            null
        );
    }
}
