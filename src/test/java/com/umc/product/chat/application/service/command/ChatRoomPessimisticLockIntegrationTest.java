package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.umc.product.chat.adapter.out.persistence.ChatMemberJpaRepository;
import com.umc.product.chat.adapter.out.persistence.ChatMessageJpaRepository;
import com.umc.product.chat.application.port.in.command.CreateChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.LeaveChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.global.event.adapter.out.persistence.EventOutboxJpaRepository;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.concurrency.PostgreSqlTransactionRace;
import com.umc.product.support.concurrency.PostgreSqlTransactionRace.TransactionCall;

@DisplayName("ChatRoom PostgreSQL 비관적 잠금")
class ChatRoomPessimisticLockIntegrationTest extends IntegrationTestSupport {

    private static final Long SENDER_ID = 10L;
    private static final long TIMEOUT_SECONDS = 10L;

    @Autowired
    CreateChatRoomUseCase createChatRoomUseCase;
    @Autowired
    CreateChatMessageUseCase createChatMessageUseCase;
    @Autowired
    LeaveChatRoomUseCase leaveChatRoomUseCase;
    @Autowired
    ChatMessageJpaRepository chatMessageRepository;
    @Autowired
    ChatMemberJpaRepository chatMemberRepository;
    @Autowired
    EventOutboxJpaRepository eventOutboxRepository;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private PostgreSqlTransactionRace race;

    @BeforeEach
    void setUpRace() {
        race = new PostgreSqlTransactionRace(transactionManager, jdbcTemplate);
    }

    @AfterEach
    void closeRace() {
        race.close();
    }

    @Test
    @DisplayName("leave commit을 기다린 send는 room lock 획득 후 멤버십을 재검증해 거절된다")
    void sendWaitingForLeaveCommit_revalidatesMembershipAfterRoomLock() throws Exception {
        Long roomId = createChatRoomUseCase.create(CreateChatRoomCommand.from(SENDER_ID)).roomId();
        CountDownLatch leaveApplied = new CountDownLatch(1);
        CountDownLatch allowLeaveCommit = new CountDownLatch(1);
        TransactionCall<Void> leave = race.submit(() -> {
            leaveChatRoomUseCase.leaveChatRoom(LeaveChatRoomCommand.of(roomId, SENDER_ID));
            leaveApplied.countDown();
            race.await(allowLeaveCommit, "leave commit release");
            return null;
        });
        race.await(leaveApplied, "leave mutation");

        TransactionCall<ChatMessageMutationResult> send = race.submit(() ->
            createChatMessageUseCase.create(text(roomId))
        );
        try {
            race.awaitPostgreSqlLockWait(send);
            assertThat(send.future().isDone()).isFalse();
        } finally {
            allowLeaveCommit.countDown();
        }
        leave.future().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        ExecutionException failure = assertThrows(
            ExecutionException.class,
            () -> send.future().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        );
        assertThat(failure.getCause())
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
        assertThat(chatMemberRepository.existsByRoomIdAndMemberId(roomId, SENDER_ID)).isFalse();
        assertThat(chatMessageRepository.count()).isZero();
        assertThat(eventOutboxRepository.count()).isZero();
    }

    private CreateChatMessageCommand text(Long roomId) {
        return new CreateChatMessageCommand(
            roomId,
            SENDER_ID,
            UUID.fromString("6f031139-40c2-4ea6-a8dc-c796b4cb0385"),
            MessageContentType.TEXT,
            "leave 이후 전송",
            List.of(),
            List.of(),
            null
        );
    }
}
