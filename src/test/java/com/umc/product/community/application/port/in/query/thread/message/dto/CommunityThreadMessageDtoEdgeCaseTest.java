package com.umc.product.community.application.port.in.query.thread.message.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.application.port.in.command.thread.message.dto.ChangeCommunityThreadMessageReactionCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.CreateCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.EditCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.TombstoneCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.UpdateCommunityThreadReadCommand;

@DisplayName("Community thread message DTO 경계")
class CommunityThreadMessageDtoEdgeCaseTest {

    private static final UUID CLIENT_MESSAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("create command는 null collection을 비우고 mention을 정렬·중복 제거한다")
    void normalizesCreateCommandCollections() {
        CreateCommunityThreadMessageCommand empty = create(1L, 2L, CLIENT_MESSAGE_ID,
            CommunityThreadMessageType.TEXT, null, null, null, null);
        CreateCommunityThreadMessageCommand mentions = create(1L, 2L, CLIENT_MESSAGE_ID,
            CommunityThreadMessageType.TEXT, "본문", List.of("file"), List.of(3L, 1L, 3L), 4L);

        assertThat(empty.fileMetadataIds()).isEmpty();
        assertThat(empty.mentionedMemberIds()).isEmpty();
        assertThat(mentions.mentionedMemberIds()).containsExactly(1L, 3L);
    }

    @Test
    @DisplayName("create command는 ID·client ID·type·mention·reply 경계를 거부한다")
    void rejectsInvalidCreateCommand() {
        assertInvalid(() -> create(0L, 2L, CLIENT_MESSAGE_ID, CommunityThreadMessageType.TEXT,
            null, null, null, null));
        assertInvalid(() -> create(1L, 0L, CLIENT_MESSAGE_ID, CommunityThreadMessageType.TEXT,
            null, null, null, null));
        assertInvalid(() -> create(1L, 2L, null, CommunityThreadMessageType.TEXT,
            null, null, null, null));
        assertInvalid(() -> create(1L, 2L, CLIENT_MESSAGE_ID, null,
            null, null, null, null));
        assertInvalid(() -> create(1L, 2L, CLIENT_MESSAGE_ID, CommunityThreadMessageType.TEXT,
            null, null, List.of(0L), null));
        assertInvalid(() -> create(1L, 2L, CLIENT_MESSAGE_ID, CommunityThreadMessageType.TEXT,
            null, null, Arrays.asList(1L, null), null));
        assertInvalid(() -> create(1L, 2L, CLIENT_MESSAGE_ID, CommunityThreadMessageType.TEXT,
            null, null, null, 0L));
    }

    @Test
    @DisplayName("edit·delete·reaction·read command는 모든 resource ID를 양수로 강제한다")
    void validatesMutationCommandIds() {
        assertThat(new EditCommunityThreadMessageCommand(1L, 2L, 3L, null).content()).isNull();
        assertThat(new TombstoneCommunityThreadMessageCommand(1L, 2L, 3L).messageId()).isEqualTo(2L);
        assertThat(new ChangeCommunityThreadMessageReactionCommand(1L, 2L, 3L, "👍").emoji())
            .isEqualTo("👍");
        assertThat(new UpdateCommunityThreadReadCommand(1L, 2L, 3L).lastReadMessageId())
            .isEqualTo(3L);

        assertInvalid(() -> new EditCommunityThreadMessageCommand(0L, 2L, 3L, "수정"));
        assertInvalid(() -> new TombstoneCommunityThreadMessageCommand(1L, 0L, 3L));
        assertInvalid(() -> new ChangeCommunityThreadMessageReactionCommand(1L, 2L, 0L, "👍"));
        assertInvalid(() -> new UpdateCommunityThreadReadCommand(1L, 2L, 0L));
    }

    @Test
    @DisplayName("message query는 cursor·limit·recipient의 양수 및 null 계약을 검증한다")
    void validatesMessageQueries() {
        assertThat(new CommunityThreadMessageHistoryQuery(1L, 2L, null, 100).beforeMessageId())
            .isNull();
        assertThat(new CommunityThreadMessageRecoveryQuery(1L, 2L, null, 1).beforeMessageId())
            .isNull();
        assertThat(new CommunityThreadMessageRecipientsQuery(1L, 2L, List.of(3L, 3L)).recipientMemberIds())
            .containsExactly(3L);

        assertInvalid(() -> new CommunityThreadMessageHistoryQuery(1L, 2L, 0L, 10));
        assertInvalid(() -> new CommunityThreadMessageHistoryQuery(1L, 2L, null, 0));
        assertInvalid(() -> new CommunityThreadMessageRecoveryQuery(0L, 2L, null, 10));
        assertInvalid(() -> new CommunityThreadMessageRecoveryQuery(1L, 2L, null, 101));
        assertInvalid(() -> new CommunityThreadMessageQuery(1L, 2L, 0L));
        assertInvalid(() -> new CommunityThreadMessageQuery(0L, 2L, 3L));
        assertInvalid(() -> new CommunityThreadMessageQuery(1L, 0L, 3L));
        assertThatThrownBy(() -> new CommunityThreadMessageRecipientsQuery(1L, 2L, null))
            .isInstanceOf(NullPointerException.class);
        assertInvalid(() -> new CommunityThreadMessageRecipientsQuery(1L, 2L, List.of(0L)));
    }

    @Test
    @DisplayName("message read model은 null collection을 비우고 음수 count·cursor·ID를 거부한다")
    void validatesMessageReadModels() {
        CommunityThreadMessageInfo info = new CommunityThreadMessageInfo(
            1L, 2L, 3L, "작성자", "본문", CommunityThreadMessageType.TEXT,
            CommunityThreadMessageStatus.SENT, null, null, null, null,
            CLIENT_MESSAGE_ID, Instant.EPOCH, null, null
        );
        assertThat(info.fileMetadataIds()).isEmpty();
        assertThat(info.mentions()).isEmpty();
        assertThat(info.reactions()).isEmpty();
        assertThat(new CommunityThreadMessagePageInfo(null, false, null).messages()).isEmpty();
        assertThat(new CommunityThreadMessagePageInfo(List.of(info), true, 2L).nextBefore()).isEqualTo(2L);
        assertThat(new CommunityThreadReactionMutationInfo(1L, null, false).reactions()).isEmpty();
        assertThat(new CommunityThreadMessageMentionInfo(1L, "회원").memberId()).isEqualTo(1L);
        assertThat(new CommunityThreadMessageReplyInfo(1L, "회원", "본문").messageId()).isEqualTo(1L);
        assertThat(new CommunityThreadMessageQuery(1L, 2L, 3L).messageId()).isEqualTo(3L);

        assertInvalid(() -> new CommunityThreadMessageInfo(
            0L, 2L, 3L, "작성자", "본문", CommunityThreadMessageType.TEXT,
            CommunityThreadMessageStatus.SENT, null, null, null, null,
            CLIENT_MESSAGE_ID, Instant.EPOCH, null, null
        ));
        assertInvalid(() -> new CommunityThreadMessageMentionInfo(0L, "회원"));
        assertInvalid(() -> new CommunityThreadMessageReplyInfo(0L, "회원", "본문"));
        assertInvalid(() -> new CommunityThreadReactionInfo("👍", -1, false));
        assertInvalid(() -> new CommunityThreadMessagePageInfo(List.of(info), true, 0L));
        assertInvalid(() -> new CommunityThreadReactionMutationInfo(0L, List.of(), false));
        assertInvalid(() -> new CommunityThreadReadMutationInfo(1L, 2L, 0L, false));
    }

    private CreateCommunityThreadMessageCommand create(
        Long threadId,
        Long senderMemberId,
        UUID clientMessageId,
        CommunityThreadMessageType type,
        String content,
        List<String> fileMetadataIds,
        List<Long> mentionedMemberIds,
        Long replyToMessageId
    ) {
        return new CreateCommunityThreadMessageCommand(
            threadId,
            senderMemberId,
            clientMessageId,
            type,
            content,
            fileMetadataIds,
            mentionedMemberIds,
            replyToMessageId
        );
    }

    private void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOf(IllegalArgumentException.class);
    }
}
