package com.umc.product.global.websocket.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

class AccessTokenWebSocketSessionRegistryTest {

    @Test
    @DisplayName("AccessToken 만료 시 연결된 WebSocket 세션을 policy violation으로 종료한다")
    void closeSessionAtAccessTokenExpiry() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        WebSocketSession session = mock(WebSocketSession.class);
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        Instant expiresAt = Instant.parse("2026-07-23T01:00:00Z");
        given(session.getId()).willReturn("session-1");
        given(session.isOpen()).willReturn(true);
        doReturn(scheduledFuture).when(scheduler).schedule(any(Runnable.class), eq(expiresAt));
        AccessTokenWebSocketSessionRegistry sut = new AccessTokenWebSocketSessionRegistry(scheduler);
        WebSocketHandler decorated = sut.decoratorFactory().decorate(delegate);
        decorated.afterConnectionEstablished(session);

        sut.scheduleExpiry("session-1", expiresAt);
        ArgumentCaptor<Runnable> expiryTask = ArgumentCaptor.forClass(Runnable.class);
        then(scheduler).should().schedule(expiryTask.capture(), eq(expiresAt));

        expiryTask.getValue().run();

        ArgumentCaptor<CloseStatus> closeStatus = ArgumentCaptor.forClass(CloseStatus.class);
        then(session).should().close(closeStatus.capture());
        assertThat(closeStatus.getValue().getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
        assertThat(closeStatus.getValue().getReason()).isEqualTo("Access token expired");
    }

    @Test
    @DisplayName("만료 예약 갱신과 연결 종료는 기존 예약 작업을 취소한다")
    void cancelExpiryTaskOnRescheduleAndDisconnect() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> firstTask = mock(ScheduledFuture.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> secondTask = mock(ScheduledFuture.class);
        WebSocketSession session = mock(WebSocketSession.class);
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        Instant firstExpiry = Instant.parse("2026-07-23T01:00:00Z");
        Instant secondExpiry = Instant.parse("2026-07-23T02:00:00Z");
        given(session.getId()).willReturn("session-1");
        doReturn(firstTask).when(scheduler).schedule(any(Runnable.class), eq(firstExpiry));
        doReturn(secondTask).when(scheduler).schedule(any(Runnable.class), eq(secondExpiry));
        AccessTokenWebSocketSessionRegistry sut = new AccessTokenWebSocketSessionRegistry(scheduler);
        WebSocketHandler decorated = sut.decoratorFactory().decorate(delegate);
        decorated.afterConnectionEstablished(session);

        sut.scheduleExpiry("session-1", firstExpiry);
        sut.scheduleExpiry("session-1", secondExpiry);
        decorated.afterConnectionClosed(session, CloseStatus.NORMAL);

        then(firstTask).should().cancel(false);
        then(secondTask).should().cancel(false);
    }

    @Test
    @DisplayName("취소와 엇갈려 실행된 이전 만료 task는 새 만료 상태를 종료하지 않는다")
    void ignoreStaleExpiryGeneration() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> firstTask = mock(ScheduledFuture.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> secondTask = mock(ScheduledFuture.class);
        WebSocketSession session = mock(WebSocketSession.class);
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        Instant firstExpiry = Instant.parse("2026-07-23T01:00:00Z");
        Instant secondExpiry = Instant.parse("2026-07-23T02:00:00Z");
        given(session.getId()).willReturn("session-1");
        given(session.isOpen()).willReturn(true);
        doReturn(firstTask).when(scheduler).schedule(any(Runnable.class), eq(firstExpiry));
        doReturn(secondTask).when(scheduler).schedule(any(Runnable.class), eq(secondExpiry));
        AccessTokenWebSocketSessionRegistry sut = new AccessTokenWebSocketSessionRegistry(scheduler);
        WebSocketHandler decorated = sut.decoratorFactory().decorate(delegate);
        decorated.afterConnectionEstablished(session);

        sut.scheduleExpiry("session-1", firstExpiry);
        ArgumentCaptor<Runnable> firstExpiryTask = ArgumentCaptor.forClass(Runnable.class);
        then(scheduler).should().schedule(firstExpiryTask.capture(), eq(firstExpiry));
        sut.scheduleExpiry("session-1", secondExpiry);
        ArgumentCaptor<Runnable> secondExpiryTask = ArgumentCaptor.forClass(Runnable.class);
        then(scheduler).should().schedule(secondExpiryTask.capture(), eq(secondExpiry));

        firstExpiryTask.getValue().run();
        then(session).should(never()).close(any(CloseStatus.class));

        secondExpiryTask.getValue().run();
        then(session).should().close(any(CloseStatus.class));
    }

    @Test
    @DisplayName("만료 task가 예약 즉시 실행되어도 게시된 상태를 확인하고 세션을 종료한다")
    void closeWhenExpiryTaskRunsImmediately() throws Exception {
        TaskScheduler scheduler = mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        WebSocketSession session = mock(WebSocketSession.class);
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        Instant expiresAt = Instant.parse("2026-07-23T01:00:00Z");
        given(session.getId()).willReturn("session-1");
        given(session.isOpen()).willReturn(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return scheduledFuture;
        }).when(scheduler).schedule(any(Runnable.class), eq(expiresAt));
        AccessTokenWebSocketSessionRegistry sut = new AccessTokenWebSocketSessionRegistry(scheduler);
        WebSocketHandler decorated = sut.decoratorFactory().decorate(delegate);
        decorated.afterConnectionEstablished(session);

        sut.scheduleExpiry("session-1", expiresAt);

        then(session).should().close(any(CloseStatus.class));
        then(scheduledFuture).should().cancel(false);
    }
}
