package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.ParsedAccessToken;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@DisplayName("StompPrincipalInterceptor")
@ExtendWith(MockitoExtension.class)
class StompPrincipalInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private StompPrincipalInterceptor sut;

    @Test
    @DisplayName("유효한 JWT 토큰으로 CONNECT 시 Principal이 설정된다")
    void connect_with_valid_token_sets_principal() {
        when(jwtTokenProvider.parseAndValidateAccessToken("valid-token"))
            .thenReturn(new ParsedAccessToken(1L, List.of("USER"), ClientType.ANDROID));

        Message<?> result = sut.preSend(connectMessage("Bearer valid-token"), channel);

        UsernamePasswordAuthenticationToken auth =
            (UsernamePasswordAuthenticationToken) StompHeaderAccessor.wrap(result).getUser();
        MemberPrincipal principal = (MemberPrincipal) auth.getPrincipal();
        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getClientType()).isEqualTo(ClientType.ANDROID);
    }

    @Test
    @DisplayName("roles가 여러 개일 때 ROLE_ 접두사가 붙은 권한으로 변환된다")
    void connect_with_multiple_roles_prefixes_role() {
        when(jwtTokenProvider.parseAndValidateAccessToken("multi-role-token"))
            .thenReturn(new ParsedAccessToken(2L, List.of("USER", "ADMIN"), ClientType.WEB));

        Message<?> result = sut.preSend(connectMessage("Bearer multi-role-token"), channel);

        UsernamePasswordAuthenticationToken auth =
            (UsernamePasswordAuthenticationToken) StompHeaderAccessor.wrap(result).getUser();
        assertThat(auth.getAuthorities())
            .extracting(Object::toString)
            .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Authorization 헤더 없이 CONNECT 시 INVALID_JWT 예외가 발생한다")
    void connect_without_authorization_header_throws() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> sut.preSend(message, channel))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("Bearer 접두사 없는 헤더로 CONNECT 시 INVALID_JWT 예외가 발생한다")
    void connect_without_bearer_prefix_throws() {
        assertThatThrownBy(() -> sut.preSend(connectMessage("invalid-format-token"), channel))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("JWT 검증 실패 시 예외가 그대로 전파된다")
    void connect_with_invalid_token_propagates_exception() {
        when(jwtTokenProvider.parseAndValidateAccessToken(anyString()))
            .thenThrow(new AuthenticationDomainException(AuthenticationErrorCode.EXPIRED_JWT_TOKEN));

        assertThatThrownBy(() -> sut.preSend(connectMessage("Bearer expired-token"), channel))
            .isInstanceOf(AuthenticationDomainException.class);
    }

    @Test
    @DisplayName("CONNECT 외 명령어는 인증 처리 없이 그대로 통과된다")
    void non_connect_command_passes_through() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/topic/test");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThat(sut.preSend(message, channel)).isSameAs(message);
    }

    private Message<byte[]> connectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", authorizationHeader);
        accessor.setLeaveMutable(true); // 인터셉터의 setUser() 호출을 허용하기 위해 mutable 유지
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
