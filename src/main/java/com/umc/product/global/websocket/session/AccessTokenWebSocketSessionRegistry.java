package com.umc.product.global.websocket.session;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.terms.reconsent", name = "enabled", havingValue = "true")
public class AccessTokenWebSocketSessionRegistry {

    private static final CloseStatus ACCESS_TOKEN_EXPIRED =
        new CloseStatus(CloseStatus.POLICY_VIOLATION.getCode(), "Access token expired");

    private final TaskScheduler taskScheduler;
    private final Object stateMonitor = new Object();
    private final Map<String, SessionExpiryState> states = new HashMap<>();
    private long generationSequence;

    public AccessTokenWebSocketSessionRegistry(
        @Qualifier("webSocketSessionExpiryScheduler") TaskScheduler taskScheduler
    ) {
        this.taskScheduler = taskScheduler;
    }

    public WebSocketHandlerDecoratorFactory decoratorFactory() {
        return handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                register(session);
                try {
                    super.afterConnectionEstablished(session);
                } catch (Exception exception) {
                    unregister(session.getId());
                    throw exception;
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                unregister(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    public void scheduleExpiry(String sessionId, Instant expiresAt) {
        if (sessionId == null || expiresAt == null) {
            return;
        }
        ExpiryRegistration registration;
        synchronized (stateMonitor) {
            SessionExpiryState current = states.get(sessionId);
            WebSocketSession session = current == null ? null : current.session();
            registration = replaceState(sessionId, session, expiresAt);
        }
        schedule(registration);
    }

    private void register(WebSocketSession session) {
        String sessionId = session.getId();
        ExpiryRegistration registration;
        synchronized (stateMonitor) {
            SessionExpiryState current = states.get(sessionId);
            Instant expiresAt = current == null ? null : current.expiresAt();
            registration = replaceState(sessionId, session, expiresAt);
        }
        schedule(registration);
    }

    private void expire(String sessionId, WebSocketSession session, long generation) {
        synchronized (stateMonitor) {
            SessionExpiryState current = states.get(sessionId);
            if (current == null || current.session() != session || current.generation() != generation) {
                return;
            }
            states.remove(sessionId);
        }
        if (!session.isOpen()) {
            return;
        }

        try {
            session.close(ACCESS_TOKEN_EXPIRED);
        } catch (IOException exception) {
            log.warn("AccessToken 만료 WebSocket 세션 종료 실패: sessionId={}", sessionId, exception);
        }
    }

    private void unregister(String sessionId) {
        synchronized (stateMonitor) {
            SessionExpiryState state = states.remove(sessionId);
            if (state != null) {
                cancel(state.expiryTask());
            }
        }
    }

    private ExpiryRegistration replaceState(
        String sessionId,
        WebSocketSession session,
        Instant expiresAt
    ) {
        SessionExpiryState previous = states.get(sessionId);
        if (previous != null) {
            cancel(previous.expiryTask());
        }
        long generation = ++generationSequence;
        states.put(sessionId, new SessionExpiryState(session, expiresAt, generation, null));
        return new ExpiryRegistration(sessionId, session, expiresAt, generation);
    }

    private void schedule(ExpiryRegistration registration) {
        if (registration.session() == null || registration.expiresAt() == null) {
            return;
        }
        ScheduledFuture<?> expiryTask = taskScheduler.schedule(
            () -> expire(registration.sessionId(), registration.session(), registration.generation()),
            registration.expiresAt()
        );
        if (expiryTask == null) {
            return;
        }

        synchronized (stateMonitor) {
            SessionExpiryState current = states.get(registration.sessionId());
            if (current == null || current.generation() != registration.generation()) {
                cancel(expiryTask);
                return;
            }
            states.put(
                registration.sessionId(),
                new SessionExpiryState(
                    current.session(),
                    current.expiresAt(),
                    current.generation(),
                    expiryTask
                )
            );
        }
    }

    private void cancel(ScheduledFuture<?> task) {
        if (task != null) {
            task.cancel(false);
        }
    }

    private record SessionExpiryState(
        WebSocketSession session,
        Instant expiresAt,
        long generation,
        ScheduledFuture<?> expiryTask
    ) {
    }

    private record ExpiryRegistration(
        String sessionId,
        WebSocketSession session,
        Instant expiresAt,
        long generation
    ) {
    }
}
