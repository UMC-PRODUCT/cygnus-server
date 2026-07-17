package com.umc.product.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChatRoomOwnerReference")
class ChatRoomOwnerReferenceTest {

    @Test
    @DisplayName("유효한 standalone binding을 불변 참조로 만든다")
    void createsReference() {
        ChatRoomOwnerReference reference = ChatRoomOwnerReference.of(
            10L,
            "chat.standalone",
            "10",
            "default"
        );

        assertThat(reference.roomId()).isEqualTo(10L);
        assertThat(reference.namespace()).isEqualTo("chat.standalone");
        assertThat(reference.ownerResourceKey()).isEqualTo("10");
        assertThat(reference.slot()).isEqualTo("default");
    }

    @Test
    @DisplayName("standalone factory는 creator가 아니라 room ID 문자열을 owner key로 사용한다")
    void createsStandaloneReferenceFromRoomId() {
        ChatRoomOwnerReference reference = ChatRoomOwnerReference.standalone(10L);

        assertThat(reference).isEqualTo(ChatRoomOwnerReference.of(
            10L,
            ChatRoomOwnerReference.STANDALONE_NAMESPACE,
            "10",
            ChatRoomOwnerReference.DEFAULT_SLOT
        ));
    }

    @Test
    @DisplayName("namespace grammar를 벗어난 binding을 거부한다")
    void rejectsInvalidNamespace() {
        assertThatThrownBy(() -> ChatRoomOwnerReference.of(
            10L,
            "Chat.standalone",
            "10",
            "default"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("resource key와 slot grammar를 벗어난 binding을 거부한다")
    void rejectsInvalidOwnerKeyOrSlot() {
        assertThatThrownBy(() -> ChatRoomOwnerReference.of(
            10L,
            "chat.standalone",
            "-10",
            "default"
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> ChatRoomOwnerReference.of(
            10L,
            "chat.standalone",
            "10",
            "Default"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("operation 목록은 Chat이 소유한 operation만 표현한다")
    void exposesChatOperations() {
        assertThat(ChatRoomOperation.values())
            .extracting(Enum::name)
            .containsExactly(
                "CREATE",
                "READ",
                "SEND",
                "JOIN",
                "LEAVE",
                "PIN",
                "UNPIN",
                "DELETE",
                "MEMBERSHIP_MANAGE"
            );
    }
}
