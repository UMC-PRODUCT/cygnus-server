package com.umc.product.global.websocket.interceptor;

import java.security.Principal;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.application.port.in.WebSocketRateLimitRejectionObserver;
import com.umc.product.global.websocket.application.service.StompClientMessageIdResolverRegistry;
import com.umc.product.global.websocket.handler.WebSocketErrorEvent;
import com.umc.product.global.websocket.support.StompCommandIdParser;

@Component
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    private static final int MAX_SEND_PER_SECOND = 20;
    private static final ApplicationEventPublisher NO_OP_EVENT_PUBLISHER = event -> {
    };
    private static final StompClientMessageIdResolverRegistry NO_OP_CLIENT_MESSAGE_ID_RESOLVER =
        new StompClientMessageIdResolverRegistry(List.of());

    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;
    private final List<WebSocketRateLimitRejectionObserver> rejectionObservers;
    private final StompClientMessageIdResolverRegistry clientMessageIdResolverRegistry;
    private final Cache<RateLimitBucket, AtomicInteger> rateLimitCache = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.SECONDS)
        .build();

    public WebSocketRateLimitInterceptor() {
        this(Clock.systemUTC(), NO_OP_EVENT_PUBLISHER, List.of(), NO_OP_CLIENT_MESSAGE_ID_RESOLVER);
    }

    public WebSocketRateLimitInterceptor(
        ObjectProvider<Clock> clockProvider,
        ApplicationEventPublisher eventPublisher
    ) {
        this(
            clockProvider.getIfAvailable(Clock::systemUTC),
            eventPublisher,
            List.of(),
            NO_OP_CLIENT_MESSAGE_ID_RESOLVER
        );
    }

    @Autowired
    public WebSocketRateLimitInterceptor(
        ObjectProvider<Clock> clockProvider,
        ApplicationEventPublisher eventPublisher,
        ObjectProvider<WebSocketRateLimitRejectionObserver> observerProvider,
        StompClientMessageIdResolverRegistry clientMessageIdResolverRegistry
    ) {
        this(
            clockProvider.getIfAvailable(Clock::systemUTC),
            eventPublisher,
            observerProvider.orderedStream().toList(),
            clientMessageIdResolverRegistry
        );
    }

    WebSocketRateLimitInterceptor(Clock clock, ApplicationEventPublisher eventPublisher) {
        this(clock, eventPublisher, List.of(), NO_OP_CLIENT_MESSAGE_ID_RESOLVER);
    }

    WebSocketRateLimitInterceptor(
        Clock clock,
        ApplicationEventPublisher eventPublisher,
        List<WebSocketRateLimitRejectionObserver> rejectionObservers
    ) {
        this(clock, eventPublisher, rejectionObservers, NO_OP_CLIENT_MESSAGE_ID_RESOLVER);
    }

    WebSocketRateLimitInterceptor(
        Clock clock,
        ApplicationEventPublisher eventPublisher,
        List<WebSocketRateLimitRejectionObserver> rejectionObservers,
        StompClientMessageIdResolverRegistry clientMessageIdResolverRegistry
    ) {
        this.clock = clock;
        this.eventPublisher = eventPublisher;
        this.rejectionObservers = List.copyOf(rejectionObservers);
        this.clientMessageIdResolverRegistry = clientMessageIdResolverRegistry;
    }

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

        RateLimitBucket bucket = new RateLimitBucket(memberId, clock.instant().getEpochSecond());
        AtomicInteger count = rateLimitCache.get(bucket, key -> new AtomicInteger(0));
        if (count.incrementAndGet() > MAX_SEND_PER_SECOND) {
            publishRateLimitError(message, accessor);
            notifyRejectionObservers(accessor.getDestination());
            return null;
        }

        return message;
    }

    private void notifyRejectionObservers(String destination) {
        for (WebSocketRateLimitRejectionObserver observer : rejectionObservers) {
            try {
                observer.observe(destination);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private Long extractMemberId(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof UsernamePasswordAuthenticationToken auth
            && auth.getPrincipal() instanceof MemberPrincipal principal) {
            return principal.getMemberId();
        }
        return null;
    }

    private void publishRateLimitError(Message<?> message, StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        eventPublisher.publishEvent(WebSocketErrorEvent.from(
            user.getName(),
            StompCommandIdParser.parse(accessor),
            clientMessageIdResolverRegistry
                .resolve(accessor.getDestination(), message.getPayload())
                .orElse(null),
            CommonErrorCode.TOO_MANY_REQUESTS,
            true
        ));
    }

    private record RateLimitBucket(Long memberId, long epochSecond) {
    }
}
