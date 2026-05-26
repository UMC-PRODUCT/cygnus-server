package com.umc.product.global.websocket.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.umc.product.global.security.MemberPrincipal;
import java.security.Principal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    private static final int MAX_SEND_PER_SECOND = 20;

    private final Cache<String, AtomicInteger> rateLimitCache = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.SECONDS)
        .build();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.SEND.equals(accessor.getCommand())) {
            return message;
        }

        Long memberId = extractMemberId(accessor);
        if (memberId == null) {
            return message;
        }

        long secondBucket = System.currentTimeMillis() / 1000;
        String cacheKey = memberId + ":" + secondBucket;
        AtomicInteger count = rateLimitCache.get(cacheKey, key -> new AtomicInteger(0));
        if (count.incrementAndGet() > MAX_SEND_PER_SECOND) {
            log.warn("WebSocket 메시지 전송 빈도 초과: memberId={}", memberId);
            return null; // 세션 유지, 해당 메시지만 무시
        }

        return message;
    }

    private Long extractMemberId(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof UsernamePasswordAuthenticationToken auth
            && auth.getPrincipal() instanceof MemberPrincipal principal) {
            return principal.getMemberId();
        }
        return null;
    }
}
