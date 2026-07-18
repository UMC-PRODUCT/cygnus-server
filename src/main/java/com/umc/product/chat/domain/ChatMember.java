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
        return of(roomId, memberId, null);
    }

    public static ChatMember of(Long roomId, Long memberId, Long initialLastReadMessageId) {
        ChatMember member = ChatMember.builder()
            .roomId(roomId)
            .memberId(memberId)
            .build();
        member.markRead(initialLastReadMessageId);
        return member;
    }

    /**
     * 읽음 위치를 갱신한다. 이미 더 최신 메시지를 읽은 상태면 무시한다(뒤로 되돌아가지 않음).
     * <p>
     * <b>단일 트랜잭션 내 in-memory 갱신 전용.</b> 트랜잭션 간 단조성(여러 기기 동시 읽음)은 이 비교로 보장되지 않는다.
     * 로드-비교-저장 사이에 다른 트랜잭션이 더 큰 값을 커밋하면 덮어써질 수 있으므로, 영속 갱신은
     * {@link com.umc.product.chat.application.port.out.SaveChatMemberPort#bumpLastReadMessageId} 원자 갱신을 사용한다.
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
