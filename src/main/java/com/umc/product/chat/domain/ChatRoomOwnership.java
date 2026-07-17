package com.umc.product.chat.domain;

import org.hibernate.annotations.Immutable;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방과 engine resource ownership binding을 저장하는 immutable registry row다.
 *
 * <p>room id가 곧 primary key라 방당 binding은 하나뿐이다. mapping 필드는 JPA에서도
 * update 불가능하게 선언해 owner transfer를 insert-only 계약으로 제한한다.</p>
 */
@Entity
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "chat_room_ownership",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_chat_room_ownership_owner_tuple",
        columnNames = {"namespace", "owner_resource_key", "slot"}
    )
)
public class ChatRoomOwnership extends BaseEntity {

    @Id
    @Column(name = "room_id", nullable = false, updatable = false)
    private Long roomId;

    @Column(
        name = "namespace",
        nullable = false,
        length = ChatRoomOwnerReference.NAMESPACE_MAX_LENGTH,
        updatable = false
    )
    private String namespace;

    @Column(
        name = "owner_resource_key",
        nullable = false,
        length = ChatRoomOwnerReference.OWNER_RESOURCE_KEY_MAX_LENGTH,
        updatable = false
    )
    private String ownerResourceKey;

    @Column(
        name = "slot",
        nullable = false,
        length = ChatRoomOwnerReference.SLOT_MAX_LENGTH,
        updatable = false
    )
    private String slot;

    private ChatRoomOwnership(ChatRoomOwnerReference reference) {
        this.roomId = reference.roomId();
        this.namespace = reference.namespace();
        this.ownerResourceKey = reference.ownerResourceKey();
        this.slot = reference.slot();
    }

    public static ChatRoomOwnership from(ChatRoomOwnerReference reference) {
        if (reference == null) {
            throw new IllegalArgumentException("ownership reference는 필수입니다.");
        }
        return new ChatRoomOwnership(reference);
    }

    public static ChatRoomOwnership of(
        Long roomId,
        String namespace,
        String ownerResourceKey,
        String slot
    ) {
        return from(ChatRoomOwnerReference.of(roomId, namespace, ownerResourceKey, slot));
    }

    public ChatRoomOwnerReference toReference() {
        return ChatRoomOwnerReference.of(roomId, namespace, ownerResourceKey, slot);
    }

    public boolean sameBinding(ChatRoomOwnership other) {
        return other != null && toReference().sameBinding(other.toReference());
    }

    public boolean matches(ChatRoomOwnerReference reference) {
        return reference != null && toReference().sameBinding(reference);
    }
}
