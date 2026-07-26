package com.umc.product.inquiry.adapter.in.web;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.inquiry.adapter.in.web.dto.request.SendInquiryMessageRequest;
import com.umc.product.inquiry.application.port.in.command.SendInquiryMessageUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 문의 채팅방 메시지 STOMP 수신 컨트롤러.
 * <p>
 * 클라이언트는 {@code /app/inquiry/{inquiryId}/message} 로 전송한다. (ADR-011 §2)
 * broadcast는 {@code /topic/inquiry/{inquiryId}} 로 구독한다. (ADR-011 §2)
 * <p>
 * 이 컨트롤러는 저장 및 상태 전환만 위임하고 값을 반환하지 않는다.
 * 실시간 broadcast는 메시지 생성 이벤트를 수신하는
 * {@link com.umc.product.inquiry.adapter.in.event.InquiryChatMessageBroadcastListener}가
 * AFTER_COMMIT에서 처리한다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class InquiryWebSocketController {

    private final SendInquiryMessageUseCase sendInquiryMessageUseCase;

    @MessageMapping("/inquiry/{inquiryId}/message")
    public void send(
        @DestinationVariable Long inquiryId,
        @Valid @Payload SendInquiryMessageRequest request,
        Principal principal
    ) {
        Long senderMemberId = extractMemberId(principal);
        sendInquiryMessageUseCase.send(request.toCommand(inquiryId, senderMemberId));
    }

    // TODO: STOMP handler가 늘어나면 messaging 전용 ArgumentResolver로 분리해
    //  REST의 @CurrentMember와 인증 주입 방식을 통일한다. (chat과 동일 TODO)
    private Long extractMemberId(Principal principal) {
        if (principal instanceof Authentication auth
            && auth.getPrincipal() instanceof MemberPrincipal memberPrincipal) {
            return memberPrincipal.getMemberId();
        }
        throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_JWT);
    }
}
