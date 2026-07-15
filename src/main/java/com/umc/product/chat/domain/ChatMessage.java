package com.umc.product.chat.domain;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;
import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지.
 * <p>
 * 순수 채팅 도메인 엔티티로, 발신자 역할(운영진/문의자)이나 문의 상태 같은 inquiry 개념을 알지 못한다.
 * "누가({@code senderMemberId}) 어느 방({@code roomId})에 무엇을({@code content}/{@code contentType})
 * 언제({@code createdAt}) 보냈고, 어떤 메시지에 답장했는가({@code replyToMessageId})"만 표현한다.
 */
@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "chat_message")
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    // 발신자. SYSTEM 메시지(입장/퇴장 등)는 발신자가 없으므로 nullable.
    @Column(name = "sender_member_id")
    private Long senderMemberId;

    @Column(name = "content_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MessageContentType contentType;

    // IMAGE/FILE은 캡션(없을 수 있음), SYSTEM은 안내 문구, TEXT는 본문.
    @Column(columnDefinition = "TEXT")
    private String content;

    // IMAGE/FILE 첨부 시 storage 도메인의 파일 메타데이터 참조 목록. 없으면 빈 배열.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "file_metadata_ids", columnDefinition = "text[]", nullable = false)
    private List<String> fileMetadataIds;

    // 답장 대상 메시지 id. 일반 메시지는 null.
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    /**
     * 일반 메시지(TEXT/IMAGE/FILE)를 생성한다.
     */
    public static ChatMessage create(
        Long roomId,
        Long senderMemberId,
        MessageContentType contentType,
        String content,
        List<String> fileMetadataIds
    ) {
        return create(roomId, senderMemberId, contentType, content, fileMetadataIds, null);
    }

    /**
     * 답장 대상이 있는 일반 메시지(TEXT/IMAGE/FILE)를 생성한다.
     * <p>
     * 콘텐츠 타입과 페이로드(content/fileMetadataIds)의 정합성은 이 팩토리가 보장한다.
     * 정합성에 위배되면 엔티티가 생성되지 않으므로, 무효한 상태의 {@link ChatMessage}는 존재할 수 없다.
     */
    public static ChatMessage create(
        Long roomId,
        Long senderMemberId,
        MessageContentType contentType,
        String content,
        List<String> fileMetadataIds,
        Long replyToMessageId
    ) {
        List<String> files = fileMetadataIds != null ? List.copyOf(fileMetadataIds) : List.of();
        validateContentConsistency(contentType, content, files);

        return ChatMessage.builder()
            .roomId(roomId)
            .senderMemberId(senderMemberId)
            .contentType(contentType)
            .content(content)
            .fileMetadataIds(files)
            .replyToMessageId(replyToMessageId)
            .build();
    }

    /**
     * 콘텐츠 타입별 페이로드 정합성 불변식을 검증한다.
     * <ul>
     *     <li>{@code TEXT} — 본문(content)이 반드시 있어야 한다.</li>
     *     <li>{@code IMAGE}/{@code FILE} — 첨부(fileMetadataIds)가 반드시 있어야 한다. content는 캡션이라 없어도 된다.</li>
     *     <li>{@code SYSTEM} — 이 팩토리로 생성할 수 없다({@link #createSystem(Long, String)} 전용).</li>
     * </ul>
     * switch 표현식이라 {@link MessageContentType}에 상수가 추가되면 컴파일 에러로 이 검증의 갱신을 강제한다.
     */
    private static void validateContentConsistency(
        MessageContentType contentType,
        String content,
        List<String> files
    ) {
        boolean hasContent = content != null && !content.isBlank();
        boolean hasFiles = !files.isEmpty();

        ChatErrorCode violation = switch (contentType) {
            case TEXT -> hasContent ? null : ChatErrorCode.CHAT_MESSAGE_EMPTY;
            case IMAGE, FILE -> hasFiles ? null : ChatErrorCode.CHAT_MESSAGE_ATTACHMENT_REQUIRED;
            case SYSTEM -> ChatErrorCode.CHAT_MESSAGE_INVALID_CONTENT_TYPE;
        };

        if (violation != null) {
            throw new ChatDomainException(violation);
        }
    }

    /**
     * 시스템 메시지(입장/퇴장 등)를 생성한다. 발신자가 없다.
     */
    public static ChatMessage createSystem(Long roomId, String content) {
        return ChatMessage.builder()
            .roomId(roomId)
            .senderMemberId(null)
            .contentType(MessageContentType.SYSTEM)
            .content(content)
            .fileMetadataIds(List.of())
            .replyToMessageId(null)
            .build();
    }
}
