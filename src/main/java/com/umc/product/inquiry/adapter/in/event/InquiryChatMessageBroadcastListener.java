package com.umc.product.inquiry.adapter.in.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.global.websocket.application.port.out.BroadcastPort;
import com.umc.product.inquiry.adapter.in.web.dto.response.InquiryMessageResponse;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ChatMessageCreatedEvent}를 수신하여 {@code /topic/inquiry/{inquiryId}}로 broadcast하는 리스너.
 *
 * <p><b>트랜잭션 경계 처리:</b><br>
 * {@code AFTER_COMMIT} 단계는 원본 트랜잭션이 이미 커밋된 후이므로 활성 트랜잭션이 없다.
 * {@code loadInquiryPort.getByRoomId()} 조회 시 JPA 영속성 컨텍스트가 필요하므로
 * {@link Propagation#REQUIRES_NEW}로 새 읽기 전용 트랜잭션을 열어 안전하게 조회한다.
 * broadcast 자체는 DB와 무관하므로 같은 트랜잭션 범위 안에서 수행한다.
 *
 * <p><b>이벤트 필터링:</b><br>
 * {@code ChatMessageCreatedEvent.roomId()}가 inquiry의 {@code chatRoomId}와 연결된 경우에만
 * broadcast한다. 해당 roomId에 매핑된 inquiry가 없으면 (inquiry 도메인이 소유하지 않은 채팅방)
 * 조용히 무시한다.
 *
 * @see com.umc.product.global.websocket.application.port.out.BroadcastPort
 * @see <a href="file:../../../../../../../../../../docs/adr/011-inquiry-domain-with-websocket-stomp.md">ADR-011 §2</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InquiryChatMessageBroadcastListener {

    // ADR-011 §2 확정 destination: /topic/inquiry/{inquiryId}
    private static final String DESTINATION_TEMPLATE = "/topic/inquiry/%d";

    private final LoadInquiryPort loadInquiryPort;
    private final BroadcastPort broadcastPort;

    /**
     * 채팅 메시지 생성 이벤트를 수신하여 inquiry topic으로 broadcast한다.
     *
     * <p>AFTER_COMMIT 후 새 읽기 전용 트랜잭션에서 roomId → inquiryId 역매핑을 수행한다.
     * inquiry 도메인이 소유하지 않은 채팅방(매핑 inquiry 없음)은 조용히 skip한다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void on(ChatMessageCreatedEvent event) {
        Inquiry inquiry;
        try {
            inquiry = loadInquiryPort.getByRoomId(event.roomId());
        } catch (InquiryDomainException e) {
            // inquiry 도메인이 소유하지 않은 채팅방 — 다른 소비 도메인의 이벤트이므로 skip
            log.trace("ChatMessageCreatedEvent 수신했으나 매핑된 inquiry 없음, skip: roomId={}", event.roomId());
            return;
        }

        String destination = String.format(DESTINATION_TEMPLATE, inquiry.getId());
        InquiryMessageResponse payload = toResponse(event);

        log.debug("Inquiry broadcast: destination={}, messageId={}", destination, event.messageId());
        broadcastPort.broadcast(destination, payload);
    }

    private InquiryMessageResponse toResponse(ChatMessageCreatedEvent event) {
        return new InquiryMessageResponse(
            event.messageId(),
            event.roomId(),
            event.senderMemberId(),
            event.contentType(),
            event.content(),
            event.fileMetadataIds(),
            event.occurredAt(),
            event.replyToMessageId()
        );
    }
}
