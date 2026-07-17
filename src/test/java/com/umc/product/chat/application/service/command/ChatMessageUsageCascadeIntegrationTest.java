package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.chat.application.port.in.command.DeleteChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.SendChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.application.port.out.ChatRoomOwnerPolicy;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatRoomOwnershipPort;
import com.umc.product.chat.application.port.out.SaveChatRoomPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.storage.application.port.out.SaveFileMetadataPort;
import com.umc.product.storage.domain.FileMetadata;
import com.umc.product.storage.domain.enums.FileCategory;
import com.umc.product.storage.domain.enums.StorageProvider;
import com.umc.product.storage.domain.exception.StorageErrorCode;
import com.umc.product.storage.domain.exception.StorageException;
import com.umc.product.support.IntegrationTestSupport;

@Import(ChatMessageUsageCascadeIntegrationTest.PolicyConfig.class)
@DisplayName("ChatMessage usage room cascade")
class ChatMessageUsageCascadeIntegrationTest extends IntegrationTestSupport {

    private static final String NAMESPACE = "test.chat-room";
    private static final Long MEMBER_ID = 10L;
    private static final String FILE_ID = "shared-chat-file";

    @Autowired
    SaveChatRoomPort saveChatRoomPort;
    @Autowired
    SaveChatRoomOwnershipPort saveChatRoomOwnershipPort;
    @Autowired
    SaveChatMemberPort saveChatMemberPort;
    @Autowired
    SendChatMessageUseCase sendChatMessageUseCase;
    @Autowired
    DeleteChatRoomUseCase deleteChatRoomUseCase;
    @Autowired
    SaveFileMetadataPort saveFileMetadataPort;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("shared file은 첫 room 삭제 후 유지되고 마지막 room 삭제에서 detach된다")
    void roomDeleteDetachesSharedFileUsages() {
        saveFile(false);
        ChatRoomOwnerReference first = createRoom();
        ChatRoomOwnerReference second = createRoom();
        sendFile(first);
        sendFile(second);
        assertThat(usageCount()).isEqualTo(2);

        deleteChatRoomUseCase.delete(first, ChatRoomActorContext.actor(MEMBER_ID));

        assertThat(usageCount()).isOne();
        assertThat(unreferencedAt()).isNull();

        deleteChatRoomUseCase.delete(second, ChatRoomActorContext.actor(MEMBER_ID));

        assertThat(usageCount()).isZero();
        assertThat(unreferencedAt()).isNotNull();
    }

    @Test
    @DisplayName("cleanup claim된 file의 usage 거부는 message 저장을 rollback한다")
    void cleanupClaimedFileRollsBackMessage() {
        saveFile(true);
        ChatRoomOwnerReference owner = createRoom();

        assertThatThrownBy(() -> sendFile(owner))
            .isInstanceOf(StorageException.class)
            .extracting(error -> ((StorageException)error).getBaseCode())
            .isEqualTo(StorageErrorCode.FILE_CLEANUP_IN_PROGRESS);

        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from chat_message", Long.class)).isZero();
        assertThat(usageCount()).isZero();
    }

    private ChatRoomOwnerReference createRoom() {
        ChatRoom room = saveChatRoomPort.save(ChatRoom.create());
        ChatRoomOwnerReference owner = ChatRoomOwnerReference.of(
            room.getId(), NAMESPACE, "room-" + room.getId(), "default");
        saveChatRoomOwnershipPort.save(owner);
        saveChatMemberPort.save(ChatMember.of(room.getId(), MEMBER_ID));
        return owner;
    }

    private void sendFile(ChatRoomOwnerReference owner) {
        sendChatMessageUseCase.send(new SendChatMessageCommand(
            owner,
            ChatRoomActorContext.actor(MEMBER_ID),
            MessageContentType.IMAGE,
            null,
            List.of(FILE_ID)
        ));
    }

    private void saveFile(boolean cleanupClaimed) {
        FileMetadata metadata = FileMetadata.builder()
            .fileId(FILE_ID)
            .originalFileName("shared.jpg")
            .category(FileCategory.POST_IMAGE)
            .contentType("image/jpeg")
            .fileSize(1024L)
            .storageProvider(StorageProvider.AWS_S3)
            .storageKey("test/shared-chat-file")
            .uploadedMemberId(MEMBER_ID)
            .build();
        metadata.markAsUploaded(Instant.parse("2026-07-18T00:00:00Z"));
        if (cleanupClaimed) {
            metadata.claimCleanup(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                Instant.parse("2026-07-18T00:01:00Z")
            );
        }
        saveFileMetadataPort.save(metadata);
    }

    private long usageCount() {
        return jdbcTemplate.queryForObject(
            "select count(*) from file_usage where file_id = ?", Long.class, FILE_ID);
    }

    private Instant unreferencedAt() {
        return jdbcTemplate.queryForObject(
            "select unreferenced_at from file_metadata where id = ?", Instant.class, FILE_ID);
    }

    @TestConfiguration
    static class PolicyConfig {

        @Bean
        ChatRoomOwnerPolicy testChatRoomOwnerPolicy() {
            return new ChatRoomOwnerPolicy() {
                @Override
                public String namespace() {
                    return NAMESPACE;
                }

                @Override
                public boolean allows(
                    ChatRoomOwnerReference ownerReference,
                    ChatRoomOperation operation,
                    ChatRoomActorContext actorContext
                ) {
                    return ownerReference != null
                        && NAMESPACE.equals(ownerReference.namespace())
                        && actorContext != null
                        && MEMBER_ID.equals(actorContext.actorMemberId());
                }
            };
        }
    }
}
