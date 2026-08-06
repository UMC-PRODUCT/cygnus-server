package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.policy.ChatAttachmentPolicy;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.in.command.UpdateChatReadUseCase;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.UpdateChatReadCommand;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatMessagePort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoom lock 재검증")
class ChatRoomLockRevalidationTest {

    private static final Long ROOM_ID = 1L;
    private static final Long SENDER_ID = 10L;
    private static final Long READER_ID = 20L;
    private static final Long MESSAGE_ID = 100L;
    private static final long TIMEOUT_SECONDS = 5L;

    @Mock
    SaveChatMessagePort saveChatMessagePort;
    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatRoomPort loadChatRoomPort;
    @Mock
    LoadChatMemberPort loadChatMemberPort;
    @Mock
    SaveChatMemberPort saveChatMemberPort;
    @Mock
    GetFileUseCase getFileUseCase;
    @Mock
    ChatAttachmentPolicy chatAttachmentPolicy;
    @Mock
    UpdateChatReadUseCase updateChatReadUseCase;
    @Mock
    DomainEventPublisher domainEventPublisher;

    private ExecutorService executor;
    private ChatMessageCommandService sendService;
    private ChatReadCommandService readService;
    private ChatMemberCommandService memberService;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        ChatRoomAccessPolicy accessPolicy = new ChatRoomAccessPolicy(loadChatMemberPort);
        sendService = new ChatMessageCommandService(
            saveChatMessagePort,
            loadChatMessagePort,
            loadChatRoomPort,
            saveChatMemberPort,
            getFileUseCase,
            chatAttachmentPolicy,
            accessPolicy,
            updateChatReadUseCase,
            domainEventPublisher
        );
        readService = new ChatReadCommandService(
            loadChatRoomPort,
            loadChatMessagePort,
            loadChatMemberPort,
            saveChatMemberPort,
            accessPolicy,
            domainEventPublisher
        );
        memberService = new ChatMemberCommandService(
            loadChatRoomPort,
            loadChatMemberPort,
            loadChatMessagePort,
            saveChatMemberPort
        );
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("send가 lock을 기다리는 사이 leave가 끝나면 잠금 후 멤버십을 다시 검증한다")
    void sendAndLeave_revalidatesMembershipAfterLock() throws Exception {
        AtomicBoolean membership = new AtomicBoolean(true);
        AtomicInteger lockAttempt = new AtomicInteger();
        CountDownLatch sendWaitingForLock = new CountDownLatch(1);
        CountDownLatch leaveCompleted = new CountDownLatch(1);
        given(loadChatMemberPort.existsByRoomIdAndMemberId(ROOM_ID, SENDER_ID))
            .willAnswer(invocation -> membership.get());
        willAnswer(invocation -> {
            if (lockAttempt.incrementAndGet() == 1) {
                sendWaitingForLock.countDown();
                await(leaveCompleted, "leave completion");
            }
            return ChatRoom.create();
        }).given(loadChatRoomPort).getByIdForUpdate(ROOM_ID);
        willAnswer(invocation -> {
            membership.set(false);
            leaveCompleted.countDown();
            return null;
        }).given(saveChatMemberPort).delete(ROOM_ID, SENDER_ID);
        Future<?> send = executor.submit(() -> sendService.send(text("leave race")));
        await(sendWaitingForLock, "send lock attempt");
        Future<?> leave = executor.submit(() ->
            memberService.leaveChatRoom(LeaveChatRoomCommand.of(ROOM_ID, SENDER_ID))
        );

        leave.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        ExecutionException failure = assertThrows(
            ExecutionException.class,
            () -> send.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        );

        assertThat(failure.getCause())
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        assertThat(membership).isFalse();
        then(saveChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("send lock 동안 exact read는 멤버십 검증보다 room lock을 먼저 기다린다")
    void sendAndRead_readWaitsAtRoomLockBeforeValidation() throws Exception {
        AtomicInteger lockAttempt = new AtomicInteger();
        AtomicReference<ChatMessage> storedMessage = new AtomicReference<>();
        CountDownLatch sendAtMembership = new CountDownLatch(1);
        CountDownLatch allowSend = new CountDownLatch(1);
        CountDownLatch sendCompleted = new CountDownLatch(1);
        CompletableFuture<ReadSeam> observedReadSeam = new CompletableFuture<>();
        given(loadChatMemberPort.existsByRoomIdAndMemberId(any(Long.class), any(Long.class)))
            .willAnswer(invocation -> {
                Long memberId = invocation.getArgument(1);
                if (SENDER_ID.equals(memberId)) {
                    sendAtMembership.countDown();
                    await(allowSend, "send membership release");
                } else if (sendCompleted.getCount() > 0L) {
                    observedReadSeam.complete(ReadSeam.MEMBERSHIP_VALIDATION);
                }
                return true;
            });
        willAnswer(invocation -> {
            if (lockAttempt.incrementAndGet() > 1) {
                observedReadSeam.complete(ReadSeam.ROOM_LOCK);
                await(sendCompleted, "send completion");
            }
            return ChatRoom.create();
        }).given(loadChatRoomPort).getByIdForUpdate(ROOM_ID);
        given(saveChatMessagePort.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", MESSAGE_ID);
            ReflectionTestUtils.setField(message, "createdAt", Instant.parse("2026-07-18T00:00:00Z"));
            storedMessage.set(message);
            return message;
        });
        given(loadChatMessagePort.getByIdAndRoomId(MESSAGE_ID, ROOM_ID)).willAnswer(invocation -> {
            ChatMessage message = storedMessage.get();
            if (message == null) {
                observedReadSeam.complete(ReadSeam.MESSAGE_VALIDATION);
                throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND);
            }
            return message;
        });
        given(loadChatMemberPort.getByRoomIdAndMemberId(ROOM_ID, READER_ID))
            .willReturn(ChatMember.of(ROOM_ID, READER_ID));

        Future<?> send = executor.submit(() -> {
            try {
                return sendService.send(text("read race"));
            } finally {
                sendCompleted.countDown();
            }
        });
        await(sendAtMembership, "send membership validation");
        Future<?> read = executor.submit(() -> readService.update(
            new UpdateChatReadCommand(ROOM_ID, READER_ID, MESSAGE_ID)
        ));
        ReadSeam firstReadSeam = observedReadSeam.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        allowSend.countDown();

        assertThat(firstReadSeam).isEqualTo(ReadSeam.ROOM_LOCK);
        send.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        read.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(lockAttempt).hasValue(2);
        then(saveChatMemberPort).should().bumpLastReadMessageId(ROOM_ID, READER_ID, MESSAGE_ID);
    }

    private SendChatMessageCommand text(String content) {
        return new SendChatMessageCommand(ROOM_ID, SENDER_ID, MessageContentType.TEXT, content, List.of());
    }

    private void await(CountDownLatch latch, String seam) throws InterruptedException {
        if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new IllegalStateException(seam + " timed out");
        }
    }

    private enum ReadSeam {
        ROOM_LOCK,
        MEMBERSHIP_VALIDATION,
        MESSAGE_VALIDATION
    }
}
