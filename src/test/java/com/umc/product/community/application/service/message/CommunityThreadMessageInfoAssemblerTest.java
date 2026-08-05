package com.umc.product.community.application.service.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageReplyInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageStatus;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;
import com.umc.product.storage.domain.enums.FileCategory;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadMessageInfoAssembler")
class CommunityThreadMessageInfoAssemblerTest {

    private static final Long THREAD_ID = 11L;
    private static final Long ROOM_ID = 101L;
    private static final Instant CREATED_AT = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetFileUseCase getFileUseCase;

    @Test
    @DisplayName(
        "한 페이지의 sender·mention·reply 이름은 단 한 번의 Member batch 조회로 매핑되고 "
            + "roomId는 노출하지 않는다"
    )
    void assembleBatchNamesOnceWithoutRawRoomId() {
        CommunityThreadMessageInfoAssembler sut =
            new CommunityThreadMessageInfoAssembler(getMemberUseCase, getFileUseCase);
        ChatMessageInfo first = chatMessage(
            900L,
            10L,
            "첫 메시지",
            List.of(20L, 30L),
            new ChatMessageReplyInfo(700L, 30L, "답글 원문")
        );
        ChatMessageInfo second = chatMessage(901L, 20L, "두 번째", List.of(10L), null);
        MemberInfo sender = mockMember(10L, "보낸이");
        MemberInfo mention = mockMember(20L, "멘션이");
        MemberInfo replySender = mockMember(30L, "답글이");
        given(getMemberUseCase.findAllByIds(Set.of(10L, 20L, 30L)))
            .willReturn(Map.of(10L, sender, 20L, mention, 30L, replySender));

        List<CommunityThreadMessageInfo> result = sut.assemble(THREAD_ID, List.of(first, second));

        assertThat(result).hasSize(2);
        CommunityThreadMessageInfo firstInfo = result.get(0);
        assertThat(firstInfo.threadId()).isEqualTo(THREAD_ID);
        assertThat(firstInfo.messageId()).isEqualTo(900L);
        assertThat(firstInfo.senderId()).isEqualTo(10L);
        assertThat(firstInfo.senderName()).isEqualTo("보낸이");
        assertThat(firstInfo.type()).isEqualTo(CommunityThreadMessageType.TEXT);
        assertThat(firstInfo.status()).isEqualTo(CommunityThreadMessageStatus.SENT);
        assertThat(firstInfo.mentions()).extracting("memberId", "name")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(20L, "멘션이"),
                org.assertj.core.groups.Tuple.tuple(30L, "답글이")
        );
        assertThat(firstInfo.replyTo().senderName()).isEqualTo("답글이");
        assertThat(firstInfo.reactions()).extracting("emoji", "count", "reactedByMe")
            .containsExactly(org.assertj.core.groups.Tuple.tuple("👍", 2L, true));
        assertThat(firstInfo.clientMessageId()).isNotNull();
        assertThat(firstInfo.createdAt()).isEqualTo(CREATED_AT);
        assertThat(recordComponentNames(CommunityThreadMessageInfo.class))
            .doesNotContain("roomId", "chatRoomId");
        then(getMemberUseCase).should(times(1)).findAllByIds(Set.of(10L, 20L, 30L));
    }

    @Test
    @DisplayName("IMAGE와 SYSTEM도 Community type으로 변환하고 서버 status는 항상 SENT로 고정한다")
    void assembleMapsImageAndSystemToSent() {
        CommunityThreadMessageInfoAssembler sut =
            new CommunityThreadMessageInfoAssembler(getMemberUseCase, getFileUseCase);
        ChatMessageInfo image = new ChatMessageInfo(
            900L,
            ROOM_ID,
            10L,
            MessageContentType.IMAGE,
            "캡션",
            List.of("file-1"),
            CREATED_AT,
            null,
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            null,
            null,
            List.of(),
            null,
            List.of()
        );
        MemberInfo sender = mockMember(10L, "보낸이");
        given(getMemberUseCase.findAllByIds(Set.of(10L))).willReturn(Map.of(10L, sender));
        given(getFileUseCase.findAllByIds(List.of("file-1")))
            .willReturn(Map.of("file-1", fileInfo("file-1", "screenshot.png", 1_024L)));

        ChatMessageInfo system = new ChatMessageInfo(
            901L,
            ROOM_ID,
            10L,
            MessageContentType.SYSTEM,
            "메시지가 삭제되었어요.",
            List.of(),
            CREATED_AT,
            null,
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            null,
            CREATED_AT,
            List.of(),
            null,
            List.of()
        );

        CommunityThreadMessageInfo imageInfo = sut.assemble(THREAD_ID, image);
        CommunityThreadMessageInfo systemInfo = sut.assemble(THREAD_ID, system);

        assertThat(imageInfo.type()).isEqualTo(CommunityThreadMessageType.IMAGE);
        assertThat(imageInfo.status()).isEqualTo(CommunityThreadMessageStatus.SENT);
        assertThat(imageInfo.files()).extracting("fileId", "fileName", "fileSize", "fileUrl")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("file-1", "screenshot.png", 1_024L, "https://cdn/file-1")
        );
        assertThat(systemInfo.type()).isEqualTo(CommunityThreadMessageType.SYSTEM);
        assertThat(systemInfo.status()).isEqualTo(CommunityThreadMessageStatus.SENT);
        then(getMemberUseCase).should(times(2)).findAllByIds(Set.of(10L));
    }

    private ChatMessageInfo chatMessage(
        Long messageId,
        Long senderId,
        String content,
        List<Long> mentions,
        ChatMessageReplyInfo reply
    ) {
        return new ChatMessageInfo(
            messageId,
            ROOM_ID,
            senderId,
            MessageContentType.TEXT,
            content,
            List.of(),
            CREATED_AT,
            reply == null ? null : reply.messageId(),
            UUID.fromString("00000000-0000-0000-0000-00000000000" + (messageId - 899L)),
            null,
            null,
            mentions,
            reply,
            List.of(new ChatReactionInfo("👍", 2L, true))
        );
    }

    private FileInfo fileInfo(String fileId, String fileName, Long fileSize) {
        return new FileInfo(
            fileId,
            fileName,
            FileCategory.POST_IMAGE,
            "image/png",
            fileSize,
            "https://cdn/" + fileId,
            true,
            10L,
            CREATED_AT
        );
    }

    private MemberInfo mockMember(Long id, String name) {
        return MemberInfo.builder()
            .id(id)
            .name(name)
            .build();
    }

    private List<String> recordComponentNames(Class<?> type) {
        return java.util.Arrays.stream(type.getRecordComponents())
            .map(RecordComponent::getName)
            .toList();
    }
}
