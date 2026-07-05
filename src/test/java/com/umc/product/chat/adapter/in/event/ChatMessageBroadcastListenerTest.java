package com.umc.product.chat.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.umc.product.chat.application.port.in.command.BroadcastChatMessageUseCase;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link ChatMessageBroadcastListener} 단위 테스트.
 *
 * <h3>예외 삼킴과 outbox retry의 관계</h3>
 *
 * <p><b>outbox off (현재 prod 기본값):</b><br>
 * {@code ChatMessageCommandService.send()} 트랜잭션 커밋 후 이 리스너가 동기로 실행된다.
 * broadcast 예외를 삼키므로 저장 결과에 영향 없다.
 *
 * <p><b>outbox on 전환 시:</b><br>
 * {@code EventOutboxRelayService.publish()}가 {@code PROPAGATION_REQUIRES_NEW} 트랜잭션 안에서
 * {@code ApplicationEventPublisher.publishEvent()}를 호출한다. 이 리스너는
 * {@code @TransactionalEventListener(AFTER_COMMIT)}이므로 relay 트랜잭션이
 * 커밋된 직후에 실행된다.
 *
 * <p>즉, 리스너 실행 시점에 {@code relayOne()}의 try-catch 범위는 이미 종료된 상태다.
 * broadcast 예외가 outbox retry를 유발하지 않는 이유는 <b>예외 삼킴</b> 때문이 아니라,
 * {@code AFTER_COMMIT}으로 인해 relay 트랜잭션 종료 후에 실행되어 {@code relayOne()}의
 * catch 블록 자체에 도달하지 않기 때문이다.
 *
 * <p>따라서 이 리스너에서 예외를 삼키지 않더라도 outbox retry 조건에는 영향이 없다.
 * 예외 삼킴의 실제 목적은 브로커 장애가 HTTP 응답(혹은 스케줄러 실행) 흐름을 중단시키지
 * 않도록 격리하는 데 있다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageBroadcastListener")
class ChatMessageBroadcastListenerTest {

    @Mock
    BroadcastChatMessageUseCase broadcastChatMessageUseCase;

    @InjectMocks
    ChatMessageBroadcastListener sut;

    @Test
    @DisplayName("정상 이벤트 수신 시 BroadcastChatMessageUseCase에 위임한다")
    void handle_정상_이벤트_수신_시_UseCase에_위임한다() {
        // given
        ChatMessageCreatedEvent event = sampleEvent();

        // when
        sut.handle(event);

        // then
        then(broadcastChatMessageUseCase).should().broadcast(event);
    }

    @Test
    @DisplayName("브로드캐스트 UseCase에서 예외가 발생해도 예외를 삼키고 상위로 전파하지 않는다")
    void handle_UseCase_예외_발생_시_예외를_삼키고_상위로_전파하지_않는다() {
        // given
        ChatMessageCreatedEvent event = sampleEvent();
        willThrow(new RuntimeException("STOMP 브로커 연결 실패"))
            .given(broadcastChatMessageUseCase).broadcast(event);

        // when & then
        // broadcast 실패는 이미 커밋된 저장에 영향을 주지 않아야 한다.
        // AFTER_COMMIT 실행 구조상 이 예외는 relay 트랜잭션의 retry 조건과도 무관하다.
        assertThatNoException().isThrownBy(() -> sut.handle(event));
    }

    private ChatMessageCreatedEvent sampleEvent() {
        return new ChatMessageCreatedEvent(
            UUID.randomUUID(),
            Instant.parse("2026-07-03T07:00:00Z"),
            100L,
            10L,
            5L,
            MessageContentType.TEXT,
            "안녕하세요",
            List.of()
        );
    }
}
