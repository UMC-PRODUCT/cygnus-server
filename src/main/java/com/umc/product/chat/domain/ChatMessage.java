package com.umc.product.chat.domain;

import com.umc.product.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 채팅 메시지.
 * <p>
 * 순수 채팅 도메인 엔티티로, 발신자 역할(운영진/문의자)이나 문의 상태 같은 inquiry 개념을 알지 못한다.
 * "누가({@code senderMemberId}) 어느 방({@code roomId})에 무엇을({@code content}/{@code contentType})
 * 언제({@code createdAt}) 보냈는가"만 표현한다.
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
        return ChatMessage.builder()
            .roomId(roomId)
            .senderMemberId(senderMemberId)
            .contentType(contentType)
            .content(content)
            .fileMetadataIds(fileMetadataIds != null ? List.copyOf(fileMetadataIds) : List.of())
            .build();
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
            .build();
    }
}
