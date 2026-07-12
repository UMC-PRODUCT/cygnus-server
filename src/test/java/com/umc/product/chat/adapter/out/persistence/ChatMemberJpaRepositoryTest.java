package com.umc.product.chat.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.global.config.JpaConfig;
import com.umc.product.support.TestContainersConfig;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, TestContainersConfig.class})
@DisplayName("ChatMemberJpaRepository")
class ChatMemberJpaRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ChatMemberJpaRepository sut;

    private Long roomId;

    @BeforeEach
    void setUp() {
        roomId = em.persist(ChatRoom.create()).getId();
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("insertIfAbsent: 없으면 저장하고 1을 반환한다")
    void insertIfAbsent_inserted() {
        int inserted = sut.insertIfAbsent(roomId, 10L);

        assertThat(inserted).isEqualTo(1);
        assertThat(sut.findByRoomIdAndMemberId(roomId, 10L)).isPresent();
    }

    @Test
    @DisplayName("insertIfAbsent: 이미 있으면 저장하지 않고 0을 반환한다")
    void insertIfAbsent_duplicated() {
        sut.insertIfAbsent(roomId, 10L);
        em.flush();
        em.clear();

        int inserted = sut.insertIfAbsent(roomId, 10L);

        assertThat(inserted).isZero();
        assertThat(sut.findAllByRoomId(roomId)).hasSize(1);
    }
}
