package com.umc.product.community.application.service.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import com.umc.product.community.application.port.in.command.thread.message.dto.UpdateCommunityThreadReadCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReadMutationInfo;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.support.concurrency.PostgreSqlTransactionRace.TransactionCall;

@DisplayName("Community thread send/read 실제 transaction 동시성")
class CommunityThreadSendReadConcurrencyIntegrationTest
    extends CommunityThreadConcurrencyIntegrationSupport {

    @Test
    @DisplayName("send와 exact read는 ChatRoom lock으로 직렬화되고 watermark 이후 unread를 보존한다")
    void sendAndExactRead_keepWatermarkDerivedCommunityUnreadConsistent() throws Exception {
        ThreadContext thread = createThread();
        Long watermark = createThreadMessageUseCase.create(communityText(
            thread,
            OWNER_ID,
            UUID.fromString("36967e84-30f3-475a-b5ea-77d65bf0b552")
        )).message().messageId();
        createChatMessageUseCase.create(chatText(
            thread,
            UUID.fromString("ae4d37ee-34ff-4bff-9e55-4acc353f4adb")
        ));
        assertThat(threadMember(thread.threadId(), MEMBER_ID).getUnreadCount()).isEqualTo(1L);
        CountDownLatch sendApplied = new CountDownLatch(1);
        CountDownLatch allowSendCommit = new CountDownLatch(1);
        TransactionCall<ChatMessageMutationResult> send = race.submit(() -> {
            ChatMessageMutationResult result = createChatMessageUseCase.create(chatText(
                thread,
                UUID.fromString("6cde6397-d664-40a0-826b-05b1b82a23ea")
            ));
            sendApplied.countDown();
            race.await(allowSendCommit, "send commit release");
            return result;
        });
        race.await(sendApplied, "send mutation");

        TransactionCall<CommunityThreadReadMutationInfo> read = race.submit(() ->
            updateThreadReadUseCase.update(new UpdateCommunityThreadReadCommand(
                thread.threadId(),
                MEMBER_ID,
                watermark
            ))
        );
        try {
            race.awaitPostgreSqlLockWait(read);
            assertThat(read.future().isDone()).isFalse();
        } finally {
            allowSendCommit.countDown();
        }
        ChatMessageMutationResult secondMessage = send.future().get(
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        );
        CommunityThreadReadMutationInfo readResult = read.future().get(
            TIMEOUT_SECONDS,
            TimeUnit.SECONDS
        );

        ChatRoomSummaryInfo summary = listChatRoomSummariesUseCase.listRoomSummaries(
            MEMBER_ID,
            List.of(thread.roomId())
        ).get(0);
        CommunityThreadMember projected = threadMember(thread.threadId(), MEMBER_ID);
        assertThat(readResult.lastReadMessageId()).isEqualTo(watermark);
        assertThat(loadChatMemberPort.getByRoomIdAndMemberId(
            thread.roomId(),
            MEMBER_ID
        ).getLastReadMessageId()).isEqualTo(watermark);
        assertThat(summary.lastMessage().messageId())
            .isEqualTo(secondMessage.message().messageId());
        assertThat(summary.unreadCount()).isEqualTo(2L);
        assertThat(projected.getUnreadCount()).isEqualTo(summary.unreadCount());
    }
}
