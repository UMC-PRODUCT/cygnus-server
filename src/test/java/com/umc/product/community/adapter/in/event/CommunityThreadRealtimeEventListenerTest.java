package com.umc.product.community.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.EventListener;

import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.community.application.event.CommunityThreadMentionedEvent;
import com.umc.product.community.application.event.CommunityThreadMessageCreatedEvent;
import com.umc.product.community.application.port.in.realtime.RelayCommunityThreadRealtimeEventUseCase;
import com.umc.product.community.domain.event.CommunityThreadDeletedEvent;
import com.umc.product.community.domain.event.CommunityThreadUpdatedEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community thread realtime event listener")
class CommunityThreadRealtimeEventListenerTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    RelayCommunityThreadRealtimeEventUseCase relayUseCase;

    @InjectMocks
    CommunityThreadRealtimeEventListener sut;

    @Test
    @DisplayName("Chat generic event와 Community lifecycle event를 Port In에 동기 위임한다")
    void delegatesRealtimeSourcesToPortIn() {
        ChatMessageCreatedEvent messageEvent = new ChatMessageCreatedEvent(
            UUID.fromString("804da0aa-a470-4b3d-ad3f-c0e18810a4e7"),
            NOW,
            900L,
            101L,
            10L,
            MessageContentType.TEXT,
            "메시지",
            List.of()
        );
        CommunityThreadDeletedEvent deletedEvent = CommunityThreadDeletedEvent.of(
            11L,
            10L,
            List.of(10L, 20L),
            NOW
        );
        CommunityThreadUpdatedEvent updatedEvent = mock(CommunityThreadUpdatedEvent.class);

        sut.onMessageCreated(messageEvent);
        sut.onThreadUpdated(updatedEvent);
        sut.onThreadDeleted(deletedEvent);

        then(relayUseCase).should().relay(messageEvent);
        then(relayUseCase).should().relay(updatedEvent);
        then(relayUseCase).should().relay(deletedEvent);
    }

    @Test
    @DisplayName("alarm용 message와 mention fact는 realtime listener source가 아니다")
    void doesNotConsumeAlarmReadyMessageFacts() {
        Set<Class<?>> listenedTypes = Arrays.stream(CommunityThreadRealtimeEventListener.class.getMethods())
            .filter(method -> method.isAnnotationPresent(EventListener.class))
            .map(Method::getParameterTypes)
            .filter(parameterTypes -> parameterTypes.length == 1)
            .map(parameterTypes -> parameterTypes[0])
            .collect(Collectors.toSet());

        assertThat(listenedTypes)
            .doesNotContain(
                CommunityThreadMessageCreatedEvent.class,
                CommunityThreadMentionedEvent.class
            );
    }
}
