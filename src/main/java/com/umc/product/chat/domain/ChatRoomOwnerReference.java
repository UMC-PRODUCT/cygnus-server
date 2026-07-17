package com.umc.product.chat.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 채팅방 ownership binding의 값 객체다.
 *
 * <p>참조에는 consumer entity가 아니라 namespace와 resource key만 들어간다. 따라서
 * Chat core가 Project/Notice 등 소비 도메인을 import하거나 owner를 추론하지 않는다.</p>
 */
public record ChatRoomOwnerReference(
    Long roomId,
    String namespace,
    String ownerResourceKey,
    String slot
) {

    public static final String STANDALONE_NAMESPACE = "chat.standalone";
    public static final String DEFAULT_SLOT = "default";

    public static final int NAMESPACE_MAX_LENGTH = 100;
    public static final int OWNER_RESOURCE_KEY_MAX_LENGTH = 128;
    public static final int SLOT_MAX_LENGTH = 50;

    private static final Pattern NAMESPACE_PATTERN = Pattern.compile(
        "^[a-z][a-z0-9-]{0,31}(\\.[a-z][a-z0-9-]{0,31})*$"
    );
    private static final Pattern OWNER_RESOURCE_KEY_PATTERN = Pattern.compile(
        "^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$"
    );
    private static final Pattern SLOT_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{0,49}$");

    public ChatRoomOwnerReference {
        requireRoomId(roomId);
        requireNamespace(namespace);
        requireOwnerResourceKey(ownerResourceKey);
        requireSlot(slot);
    }

    public static ChatRoomOwnerReference of(
        Long roomId,
        String namespace,
        String ownerResourceKey,
        String slot
    ) {
        return new ChatRoomOwnerReference(roomId, namespace, ownerResourceKey, slot);
    }

    /** 새 engine-native room의 canonical server-generated ownership binding을 만든다. */
    public static ChatRoomOwnerReference standalone(Long roomId) {
        requireRoomId(roomId);
        return of(roomId, STANDALONE_NAMESPACE, roomId.toString(), DEFAULT_SLOT);
    }

    public static ChatRoomOwnerReference from(ChatRoomOwnership ownership) {
        Objects.requireNonNull(ownership, "ownership은 필수입니다.");
        return of(
            ownership.getRoomId(),
            ownership.getNamespace(),
            ownership.getOwnerResourceKey(),
            ownership.getSlot()
        );
    }

    public ChatRoomOwnership toOwnership() {
        return ChatRoomOwnership.from(this);
    }

    public ChatRoomOwnerReference withNamespace(String replacementNamespace) {
        return of(roomId, replacementNamespace, ownerResourceKey, slot);
    }

    public boolean sameBinding(ChatRoomOwnerReference other) {
        return other != null
            && roomId.equals(other.roomId)
            && namespace.equals(other.namespace)
            && ownerResourceKey.equals(other.ownerResourceKey)
            && slot.equals(other.slot);
    }

    private static void requireRoomId(Long roomId) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("roomId는 양수여야 합니다.");
        }
    }

    private static void requireNamespace(String namespace) {
        if (namespace == null
            || namespace.length() > NAMESPACE_MAX_LENGTH
            || !NAMESPACE_PATTERN.matcher(namespace).matches()) {
            throw new IllegalArgumentException("namespace grammar가 올바르지 않습니다.");
        }
    }

    private static void requireOwnerResourceKey(String ownerResourceKey) {
        if (ownerResourceKey == null
            || ownerResourceKey.length() > OWNER_RESOURCE_KEY_MAX_LENGTH
            || !OWNER_RESOURCE_KEY_PATTERN.matcher(ownerResourceKey).matches()) {
            throw new IllegalArgumentException("ownerResourceKey grammar가 올바르지 않습니다.");
        }
    }

    private static void requireSlot(String slot) {
        if (slot == null || slot.length() > SLOT_MAX_LENGTH || !SLOT_PATTERN.matcher(slot).matches()) {
            throw new IllegalArgumentException("slot grammar가 올바르지 않습니다.");
        }
    }
}
