package com.umc.product.global.websocket.interceptor;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.ParsedAccessToken;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompPrincipalInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_JWT);
        }

        String token = authHeader.substring(7);

        // TODO: 인증 실패 시 STOMP ERROR 프레임을 ApiResponse 형식으로 포맷팅하는 StompSubProtocolErrorHandler 구현 필요
        ParsedAccessToken parsed = jwtTokenProvider.parseAndValidateAccessToken(token);

        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(parsed.memberId())
            .clientType(parsed.clientType())
            .build();
        List<SimpleGrantedAuthority> authorities = parsed.roles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, authorities);

        accessor.setUser(authentication);

        log.debug("WebSocket CONNECT 인증 성공: memberId={}", parsed.memberId());

        return message;
    }
}
