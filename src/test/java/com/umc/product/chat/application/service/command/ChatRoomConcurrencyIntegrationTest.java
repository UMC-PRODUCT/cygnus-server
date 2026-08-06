package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.umc.product.chat.adapter.out.persistence.ChatMessageJpaRepository;
import com.umc.product.chat.adapter.out.persistence.ChatMessageReactionJpaRepository;
import com.umc.product.chat.application.port.in.command.CreateChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.ManageChatMessageReactionUseCase;
import com.umc.product.chat.application.port.in.command.dto.ChangeChatMessageReactionCommand;
import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.ChatReactionMutationResult;
import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("ChatRoom lock concurrency")
class ChatRoomConcurrencyIntegrationTest extends IntegrationTestSupport {

    private static final Long OWNER = 10L;

    @Autowired
    CreateChatRoomUseCase createChatRoomUseCase;
    @Autowired
    CreateChatMessageUseCase createChatMessageUseCase;
    @Autowired
    ManageChatMessageReactionUseCase manageChatMessageReactionUseCase;
    @Autowired
    ChatMessageJpaRepository chatMessageRepository;
    @Autowired
    ChatMessageReactionJpaRepository reactionRepository;

    @Test
    @DisplayName("동일 clientMessageId 동시 retry는 fresh 한 건과 deduplicated 한 건만 반환한다")
    void concurrentRetry() throws Exception {
        Long roomId = createRoom();
        CreateChatMessageCommand command = text(
            roomId,
            OWNER,
            UUID.fromString("542f64b6-bac8-4aad-bfdb-683645f4dc24"),
            "retry"
        );

        List<ChatMessageMutationResult> results = race(
            () -> createChatMessageUseCase.create(command),
            () -> createChatMessageUseCase.create(command)
        );

        assertThat(results).extracting(ChatMessageMutationResult::deduplicated)
            .containsExactlyInAnyOrder(false, true);
        assertThat(results).extracting(result -> result.message().messageId())
            .containsOnly(results.get(0).message().messageId());
        assertThat(chatMessageRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("동일 reaction 동시 추가는 상태 변경 한 건과 idempotent no-op 한 건이다")
    void concurrentDuplicateReaction() throws Exception {
        Long roomId = createRoom();
        Long messageId = createChatMessageUseCase.create(text(
            roomId,
            OWNER,
            UUID.fromString("e07147f6-8524-43c9-9b55-6045ad0d69cb"),
            "reaction"
        )).message().messageId();
        ChangeChatMessageReactionCommand command =
            new ChangeChatMessageReactionCommand(roomId, messageId, OWNER, "👍");

        List<ChatReactionMutationResult> results = race(
            () -> manageChatMessageReactionUseCase.add(command),
            () -> manageChatMessageReactionUseCase.add(command)
        );

        assertThat(results).extracting(ChatReactionMutationResult::deduplicated)
            .containsExactlyInAnyOrder(false, true);
        assertThat(reactionRepository.count()).isEqualTo(1L);
    }

    private Long createRoom() {
        ChatRoomInfo room = createChatRoomUseCase.create(CreateChatRoomCommand.from(OWNER));
        return room.roomId();
    }

    private CreateChatMessageCommand text(Long roomId, Long senderId, UUID clientId, String content) {
        return new CreateChatMessageCommand(
            roomId,
            senderId,
            clientId,
            MessageContentType.TEXT,
            content,
            List.of(),
            List.of(),
            null
        );
    }

    private <T> List<T> race(Callable<T> firstAction, Callable<T> secondAction) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<T> first = executor.submit(() -> runAfterSignal(firstAction, ready, start));
            Future<T> second = executor.submit(() -> runAfterSignal(secondAction, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private <T> T runAfterSignal(
        Callable<T> action,
        CountDownLatch ready,
        CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("chat concurrency start timed out");
        }
        return action.call();
    }
}
