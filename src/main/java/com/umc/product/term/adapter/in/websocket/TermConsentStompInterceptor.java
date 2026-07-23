package com.umc.product.term.adapter.in.websocket;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.session.AccessTokenWebSocketSessionRegistry;
import com.umc.product.term.config.TermConsentEnforcementProperties;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

@Component
@ConditionalOnProperty(prefix = "app.terms.reconsent", name = "enabled", havingValue = "true")
public class TermConsentStompInterceptor implements ChannelInterceptor {

    private static final String METRIC_DOMAIN = "terms";
    private static final String METRIC_OPERATION = "reconsent_enforcement";
    private static final Set<StompCommand> ENFORCED_COMMANDS = Set.of(
        StompCommand.CONNECT,
        StompCommand.STOMP,
        StompCommand.SEND,
        StompCommand.SUBSCRIBE
    );

    private final TermConsentEnforcementProperties properties;
    private final Clock clock;
    private final OperationalMetrics operationalMetrics;
    private final AccessTokenWebSocketSessionRegistry sessionRegistry;

    public TermConsentStompInterceptor(
        TermConsentEnforcementProperties properties,
        Clock clock,
        OperationalMetrics operationalMetrics,
        AccessTokenWebSocketSessionRegistry sessionRegistry
    ) {
        this.properties = properties;
        this.clock = clock;
        this.operationalMetrics = operationalMetrics;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (!properties.enabled() || accessor == null || !ENFORCED_COMMANDS.contains(accessor.getCommand())) {
            return message;
        }

        MemberPrincipal principal = resolveMemberPrincipal(accessor.getUser());
        if (principal == null) {
            return message;
        }

        Instant expiresAt = principal.getAccessTokenExpiresAt();
        if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
        }

        if (!principal.isRequiredTermsAgreed()) {
            operationalMetrics.recordSecurityEvent(METRIC_DOMAIN, METRIC_OPERATION, "blocked_stomp");
            throw new TermDomainException(TermErrorCode.TERMS_RECONSENT_REQUIRED);
        }

        if (isConnectionCommand(accessor.getCommand())) {
            sessionRegistry.scheduleExpiry(accessor.getSessionId(), expiresAt);
        }
        return message;
    }

    private MemberPrincipal resolveMemberPrincipal(Principal principal) {
        if (principal instanceof Authentication authentication
            && authentication.getPrincipal() instanceof MemberPrincipal memberPrincipal) {
            return memberPrincipal;
        }
        return null;
    }

    private boolean isConnectionCommand(StompCommand command) {
        return StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command);
    }
}
