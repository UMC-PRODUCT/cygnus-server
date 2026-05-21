package com.umc.product.global.security;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
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
public class StompChannelInterceptor implements ChannelInterceptor {

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
        jwtTokenProvider.validateAccessToken(token);

        Long memberId = jwtTokenProvider.parseAccessToken(token);
        List<String> roles = jwtTokenProvider.getRolesFromAccessToken(token);

        MemberPrincipal principal = new MemberPrincipal(memberId);
        List<SimpleGrantedAuthority> authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, authorities);

        // WebSocket 세션에 인증 정보 바인딩 - 이후 메시지에서 Principal로 접근 가능
        accessor.setUser(authentication);

        log.info("WebSocket CONNECT 인증 성공: memberId={}", memberId);

        return message;
    }
}
