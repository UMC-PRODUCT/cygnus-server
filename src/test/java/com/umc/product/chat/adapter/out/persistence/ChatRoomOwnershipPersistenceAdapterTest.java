package com.umc.product.chat.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.global.config.JpaConfig;
import com.umc.product.global.config.QueryDslConfig;
import com.umc.product.support.TestContainersConfig;

import jakarta.persistence.PersistenceException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QueryDslConfig.class, TestContainersConfig.class, ChatRoomOwnershipPersistenceAdapter.class})
@DisplayName("ChatRoomOwnershipPersistenceAdapter")
class ChatRoomOwnershipPersistenceAdapterTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ChatRoomOwnershipPersistenceAdapter sut;

    private Long roomId;

    @BeforeEach
    void setUp() {
        roomId = em.persist(ChatRoom.create()).getId();
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("binding을 저장하고 room id로 lock 조회한다")
    void savesAndLocksBinding() {
        ChatRoomOwnerReference reference = ChatRoomOwnerReference.standalone(roomId);

        sut.save(reference);
        em.flush();
        em.clear();

        assertThat(sut.findByRoomId(roomId)).contains(reference);
        assertThat(sut.findByRoomIdForUpdate(roomId))
            .contains(reference);
    }

    @Test
    @DisplayName("같은 binding을 다시 저장해도 한 row로 idempotent하다")
    void sameBindingIsIdempotent() {
        ChatRoomOwnerReference reference = reference(roomId, "10");

        sut.save(reference);
        sut.save(reference);
        em.flush();
        em.clear();

        assertThat(sut.findByRoomId(roomId)).contains(reference);
        assertThat(sut.count()).isOne();
    }

    @Test
    @DisplayName("같은 room의 owner transfer는 거부한다")
    void rejectsOwnerTransfer() {
        sut.save(reference(roomId, "10"));

        assertThatThrownBy(() -> sut.save(reference(roomId, "11")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("owner tuple은 두 room에서 중복될 수 없다")
    void rejectsDuplicateOwnerTuple() {
        Long secondRoomId = em.persist(ChatRoom.create()).getId();
        em.flush();

        sut.save(reference(roomId, "shared"));

        assertThatThrownBy(() -> sut.save(reference(secondRoomId, "shared")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("chat room 삭제 시 ownership row도 cascade 삭제된다")
    void cascadesWithRoomDelete() {
        sut.save(reference(roomId, "10"));
        em.flush();
        ChatRoom room = em.find(ChatRoom.class, roomId);
        em.remove(room);
        em.flush();
        em.clear();

        assertThat(sut.findByRoomId(roomId)).isEmpty();
    }

    @Test
    @DisplayName("저장된 namespace를 중복 없이 조회한다")
    void listsDistinctNamespaces() {
        Long secondRoomId = em.persist(ChatRoom.create()).getId();
        em.flush();

        sut.save(reference(roomId, "10"));
        sut.save(reference(secondRoomId, "11").withNamespace("chat.future"));

        assertThat(sut.listDistinctNamespaces())
            .containsExactly("chat.future", "chat.standalone");
    }

    @Test
    @DisplayName("잘못된 grammar는 DB CHECK에서 거부한다")
    void rejectsInvalidGrammarAtDatabase() {
        assertThatThrownBy(() -> em.getEntityManager()
            .createNativeQuery("""
                insert into chat_room_ownership(room_id, namespace, owner_resource_key, slot, created_at, updated_at)
                values (:roomId, 'Chat.standalone', '10', 'default', now(), now())
                """)
            .setParameter("roomId", roomId)
            .executeUpdate())
            .isInstanceOf(PersistenceException.class);
    }

    private ChatRoomOwnerReference reference(Long id, String ownerKey) {
        return ChatRoomOwnerReference.of(id, "chat.standalone", ownerKey, "default");
    }
}
