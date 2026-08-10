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
@Table(name = "chat_room")
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pinned_message_id")
    private Long pinnedMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "read_scope", nullable = false, length = 20)
    private ChatRoomReadScope readScope;

    /**
     * 멤버만 조회할 수 있는 방을 만든다. 조회 범위를 명시하지 않은 소비 도메인은 fail-closed 기본값을 갖는다.
     */
    public static ChatRoom create() {
        return create(ChatRoomReadScope.MEMBER_ONLY);
    }

    public static ChatRoom create(ChatRoomReadScope readScope) {
        return ChatRoom.builder()
            .readScope(readScope == null ? ChatRoomReadScope.MEMBER_ONLY : readScope)
            .build();
    }

    /**
     * 비멤버도 메시지를 조회할 수 있는 방인지 판단한다.
     *
     * <p>조회 인가에만 사용한다. 메시지 생성/수정/삭제, 리액션, 읽음 갱신은 이 값과 무관하게
     * 방 멤버만 수행할 수 있다.</p>
     */
    public boolean isPubliclyReadable() {
        return readScope == ChatRoomReadScope.PUBLIC;
    }

    public void pinMessage(Long messageId) {
        this.pinnedMessageId = messageId;
    }

    public void unpinMessage() {
        this.pinnedMessageId = null;
    }
}
