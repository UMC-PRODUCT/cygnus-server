package com.umc.product.chat.adapter.in.event;

import com.umc.product.chat.application.port.in.command.BroadcastChatMessageUseCase;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 채팅 메시지 생성 이벤트를 수신하여 구독자에게 실시간 broadcast 한다.
 * <p>
 * <b>실행 시점</b><br>
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 을 사용하므로, 이벤트를 발행한 트랜잭션이 커밋된
 * 직후에 동기로 실행된다. 이는 아래 두 가지 문제를 방지한다.
 * <ul>
 *     <li>유령 broadcast 방지: 커밋 전 broadcast 시 트랜잭션 롤백이 발생하면 DB 에 없는 메시지를
 *         클라이언트가 수신하게 된다. AFTER_COMMIT 은 커밋이 확정된 이후에만 broadcast 를 수행한다.</li>
 *     <li>지속성·브로커 결합 방지: broadcast 예외가 메시지 저장 트랜잭션을 롤백시키지 않는다.
 *         이미 커밋된 이후이므로 broadcast 실패는 저장에 영향을 주지 않으며, 예외는 삼키고 로그만 남긴다.</li>
 * </ul>
 * <p>
 * <b>트랜잭션 컨텍스트</b><br>
 * {@code @TransactionalEventListener} 는 활성 트랜잭션이 없으면 기본적으로 실행되지 않아 이벤트가
 * 조용히 유실된다. 현재 이 이벤트는 {@code ChatMessageCommandService.send()} 에서만 발행되며,
 * 해당 메서드는 클래스 레벨 {@code @Transactional} 로 항상 활성 트랜잭션 컨텍스트에서 실행되므로
 * 유실 위험이 없다.
 * <p>
 * <b>outbox on 전환 시 재검토 필요</b><br>
 * 현재 prod 설정은 {@code EVENT_OUTBOX_ENABLED=false} (outbox off) 기준이다.
 * outbox on 으로 전환할 경우, {@code EventOutboxRelayService} 가 {@code REQUIRES_NEW} 트랜잭션
 * 안에서 이벤트를 재발행하므로 이 리스너는 relay 트랜잭션 커밋 후 실행된다. 해당 흐름에서
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 이 의도대로 동작하는지 재검토할 것.
 * <p>
 * TODO: 처리량 부하가 확인되면 {@code @Async} 비동기 처리를 검토한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageBroadcastListener {

    private final BroadcastChatMessageUseCase broadcastChatMessageUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageCreatedEvent event) {
        try {
            broadcastChatMessageUseCase.broadcast(event);
        } catch (Exception e) {
            // broadcast 실패는 이미 커밋된 저장에 영향을 주지 않는다.
            // 예외를 삼키고 로그만 남겨 broker 가용성이 영속성에 결합되지 않도록 한다.
            log.error("채팅 메시지 broadcast 실패: messageId={}, roomId={}",
                event.messageId(), event.roomId(), e);
        }
    }
}
