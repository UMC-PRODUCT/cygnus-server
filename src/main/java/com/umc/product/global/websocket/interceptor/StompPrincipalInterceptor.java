package com.umc.product.global.websocket.interceptor;

import java.util.List;

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

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.ParsedAccessToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompPrincipalInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !isConnectionCommand(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_JWT);
        }

        String token = authHeader.substring(7);

        ParsedAccessToken parsed = jwtTokenProvider.parseAndValidateAccessToken(token);

        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(parsed.memberId())
            .clientType(parsed.clientType())
            .clientContextClaims(parsed.clientContextClaims())
            .requiredTermsAgreed(parsed.requiredTermsAgreed())
            .accessTokenExpiresAt(parsed.expiresAt())
            .build();
        List<SimpleGrantedAuthority> authorities = parsed.roles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(principal, null, authorities);

        accessor.setUser(authentication);
        accessor.removeNativeHeader("Authorization");

        log.debug("WebSocket CONNECT 인증 확인 memberId={}", parsed.memberId());

        return message;
    }

    private boolean isConnectionCommand(StompCommand command) {
        return StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command);
    }
}
