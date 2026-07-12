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

import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.global.config.JpaConfig;
import com.umc.product.global.config.QueryDslConfig;
import com.umc.product.support.TestContainersConfig;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QueryDslConfig.class, TestContainersConfig.class})
@DisplayName("ChatMemberJpaRepository.bumpLastReadMessageId (원자 단조 갱신)")
class ChatMemberJpaRepositoryTest {

    private static final Long MEMBER = 10L;

    @Autowired
    TestEntityManager em;

    @Autowired
    ChatMemberJpaRepository sut;

    private Long roomId;
    private Long memberRowId;

    @BeforeEach
    void setUp() {
        roomId = em.persist(ChatRoom.create()).getId();
        memberRowId = em.persistAndGetId(ChatMember.of(roomId, MEMBER), Long.class);
        em.flush();
    }

    @Test
    @DisplayName("처음(null) 상태에서 candidate로 설정된다")
    void bump_fromNull() {
        int updated = sut.bumpLastReadMessageId(roomId, MEMBER, 42L);

        assertThat(updated).isEqualTo(1);
        assertThat(reloadLastRead()).isEqualTo(42L);
    }

    @Test
    @DisplayName("커밋 순서가 뒤집혀 더 작은 candidate가 나중에 와도(120 후 100) 최댓값 120이 유지된다")
    void bump_monotonic_reversedOrder() {
        sut.bumpLastReadMessageId(roomId, MEMBER, 120L);
        sut.bumpLastReadMessageId(roomId, MEMBER, 100L);

        assertThat(reloadLastRead()).isEqualTo(120L);
    }

    @Test
    @DisplayName("이미 읽은 것보다 작거나 같은 candidate는 무시된다")
    void bump_ignoresLowerOrEqual() {
        sut.bumpLastReadMessageId(roomId, MEMBER, 100L);

        sut.bumpLastReadMessageId(roomId, MEMBER, 50L);
        assertThat(reloadLastRead()).isEqualTo(100L);

        sut.bumpLastReadMessageId(roomId, MEMBER, 100L);
        assertThat(reloadLastRead()).isEqualTo(100L);
    }

    @Test
    @DisplayName("방 멤버가 아니면 0건 갱신되고 아무 것도 바뀌지 않는다")
    void bump_nonMember_noop() {
        int updated = sut.bumpLastReadMessageId(roomId, 999L, 42L);

        assertThat(updated).isZero();
        assertThat(reloadLastRead()).isNull();
    }

    private Long reloadLastRead() {
        em.flush();
        em.clear();
        return em.find(ChatMember.class, memberRowId).getLastReadMessageId();
    }
}
