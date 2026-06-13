package com.umc.product.chat.adapter.in.web;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.chat.adapter.in.web.dto.request.SendChatMessageRequest;
import com.umc.product.chat.application.port.in.command.SendChatMessageUseCase;
import com.umc.product.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/**
 * 채팅 메시지 STOMP 수신 컨트롤러.
 * <p>
 * 클라이언트는 {@code /app/chat/rooms/{roomId}/messages}로 전송하고, broadcast는
 * {@code /topic/chat/rooms/{roomId}/messages}로 구독한다. 이 컨트롤러는 저장만 위임하고 값을 반환하지 않는다.
 * 실시간 broadcast는 메시지 생성 이벤트를 수신하는 별도 컴포넌트(BroadcastPort)가 처리하므로,
 * 여기서 {@code SimpMessagingTemplate}을 직접 호출하지 않는다(브로커 교체 대비 디커플링).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageStompController {

    private final SendChatMessageUseCase sendChatMessageUseCase;

    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void send(
        @DestinationVariable Long roomId,
        @Valid @Payload SendChatMessageRequest request,
        Principal principal
    ) {
        Long senderMemberId = extractMemberId(principal);
        sendChatMessageUseCase.send(request.toCommand(roomId, senderMemberId));
    }

    // TODO: STOMP handler가 늘어나면 messaging 전용 ArgumentResolver(addArgumentResolvers 등록,
    //  accessor.getUser()에서 추출)로 분리해 REST의 @CurrentMember와 인증 주입 방식을 통일한다.
    private Long extractMemberId(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof MemberPrincipal memberPrincipal) {
            return memberPrincipal.getMemberId();
        }
        throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_JWT);
    }
}
