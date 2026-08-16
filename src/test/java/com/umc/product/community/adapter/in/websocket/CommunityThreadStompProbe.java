package com.umc.product.community.adapter.in.websocket;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class CommunityThreadStompProbe implements AutoCloseable {

    private static final AtomicInteger CLIENT_SEQUENCE = new AtomicInteger();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);

    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskScheduler scheduler;
    private final WebSocketStompClient client;
    private final BlockingQueue<StompFrame> terminalFrames = new LinkedBlockingQueue<>();
    private final BlockingQueue<Throwable> transportErrors = new LinkedBlockingQueue<>();
    private final AtomicBoolean remoteClosed = new AtomicBoolean();
    private final StompSession session;

    private CommunityThreadStompProbe(int port, String accessToken, ObjectMapper objectMapper) throws Exception {
        this.objectMapper = objectMapper;
        this.scheduler = scheduler();
        this.client = stompClient(scheduler);
        StompSession connected;
        try {
            connected = connect(port, accessToken);
        } catch (Exception exception) {
            client.stop();
            scheduler.shutdown();
            throw exception;
        }
        this.session = connected;
        this.session.setAutoReceipt(true);
    }

    static CommunityThreadStompProbe connect(int port, String accessToken, ObjectMapper objectMapper)
        throws Exception {
        return new CommunityThreadStompProbe(port, accessToken, objectMapper);
    }

    BlockingQueue<StompFrame> subscribe(String destination, Duration receiptTimeout)
        throws InterruptedException {
        BlockingQueue<StompFrame> frames = new LinkedBlockingQueue<>();
        CountDownLatch receipt = new CountDownLatch(1);
        StompSession.Subscription subscription = session.subscribe(destination, frameHandler(frames));
        subscription.addReceiptTask(receipt::countDown);
        if (!receipt.await(receiptTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new AssertionError("STOMP SUBSCRIBE receipt를 받지 못했습니다: " + destination);
        }
        return frames;
    }

    void subscribeExpectingTerminalError(String destination) {
        session.subscribe(destination, frameHandler(new LinkedBlockingQueue<>()));
    }

    void send(String destination, UUID commandId, Object body) throws JsonProcessingException {
        StompHeaders headers = new StompHeaders();
        headers.setDestination(destination);
        headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
        headers.add("x-command-id", commandId.toString());
        session.send(headers, objectMapper.writeValueAsBytes(body));
    }

    StompFrame awaitTerminalError(Duration timeout) throws InterruptedException {
        StompFrame frame = terminalFrames.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (frame != null) {
            return frame;
        }
        Throwable transportError = transportErrors.poll();
        if (transportError != null) {
            throw new AssertionError("ERROR frame 전에 STOMP transport가 종료됐습니다.", transportError);
        }
        throw new AssertionError("STOMP terminal ERROR frame을 받지 못했습니다.");
    }

    @Override
    public void close() {
        try {
            if (!remoteClosed.get() && session.isConnected()) {
                try {
                    session.disconnect();
                } catch (MessageDeliveryException ignored) {
                }
            }
        } finally {
            try {
                client.stop();
            } finally {
                scheduler.shutdown();
            }
        }
    }

    private StompSession connect(int port, String accessToken) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + accessToken);
        return client.connectAsync(
            "http://localhost:%d/ws".formatted(port),
            new WebSocketHttpHeaders(),
            connectHeaders,
            new SessionHandler()
        ).get(CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }

    private StompFrameHandler frameHandler(BlockingQueue<StompFrame> frames) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                frames.offer(frame(headers, payload));
            }
        };
    }

    private StompFrame frame(StompHeaders headers, Object payload) {
        byte[] bytes = (byte[]) payload;
        return new StompFrame(headers, new String(bytes, StandardCharsets.UTF_8));
    }

    private ThreadPoolTaskScheduler scheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("community-stomp-e2e-" + CLIENT_SEQUENCE.incrementAndGet() + "-");
        taskScheduler.initialize();
        return taskScheduler;
    }

    private WebSocketStompClient stompClient(ThreadPoolTaskScheduler taskScheduler) {
        SockJsClient sockJsClient = new SockJsClient(
            List.of(new WebSocketTransport(new StandardWebSocketClient()))
        );
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setTaskScheduler(taskScheduler);
        stompClient.setReceiptTimeLimit(Duration.ofSeconds(10).toMillis());
        return stompClient;
    }

    record StompFrame(StompHeaders headers, String payload) {
    }

    private final class SessionHandler extends StompSessionHandlerAdapter {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            remoteClosed.set(true);
            terminalFrames.offer(frame(headers, payload));
        }

        @Override
        public void handleTransportError(StompSession stompSession, Throwable exception) {
            remoteClosed.set(true);
            transportErrors.offer(exception);
        }

        @Override
        public void handleException(
            StompSession stompSession,
            org.springframework.messaging.simp.stomp.StompCommand command,
            StompHeaders headers,
            byte[] payload,
            Throwable exception
        ) {
            remoteClosed.set(true);
            transportErrors.offer(exception);
        }
    }
}
