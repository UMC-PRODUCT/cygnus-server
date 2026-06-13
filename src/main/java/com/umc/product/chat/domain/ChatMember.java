package com.umc.product.chat.domain;

import com.umc.product.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "chat_member")
public class ChatMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 이 멤버가 마지막으로 읽은 메시지 id. 안 읽은 메시지 수 계산 기준. 아직 읽은 메시지가 없으면 null.
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    public static ChatMember of(Long roomId, Long memberId) {
        return ChatMember.builder()
            .roomId(roomId)
            .memberId(memberId)
            .build();
    }

    /**
     * 읽음 위치를 갱신한다. 이미 더 최신 메시지를 읽은 상태면 무시한다(뒤로 되돌아가지 않음).
     */
    public void markRead(Long messageId) {
        if (messageId == null) {
            return;
        }
        if (this.lastReadMessageId == null || messageId > this.lastReadMessageId) {
            this.lastReadMessageId = messageId;
        }
    }
}
