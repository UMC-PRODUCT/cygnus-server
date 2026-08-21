package com.umc.product.test.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.MemberSystemRoleType;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.seed.enabled=true")
@Import(MemberSystemRoleSeedPersistenceAdapter.class)
class MemberSystemRoleSeedPersistenceAdapterTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private MemberSystemRoleSeedPersistenceAdapter sut;

    @Test
    @DisplayName("같은 회원의 SUPER_ADMIN 역할은 한 행만 생성한다")
    void assignSuperAdminOnlyOnce() {
        // given
        Member member = Member.create("테스트 관리자", "테스트관리자", "seed-admin@test.com", null, null);
        entityManager.persistAndFlush(member);

        // when
        boolean firstCreated = sut.assignIfAbsent(member.getId(), MemberSystemRoleType.SUPER_ADMIN);
        boolean secondCreated = sut.assignIfAbsent(member.getId(), MemberSystemRoleType.SUPER_ADMIN);
        entityManager.flush();
        entityManager.clear();

        // then
        Long count = entityManager.getEntityManager()
            .createQuery("""
                SELECT count(role)
                FROM MemberSystemRole role
                WHERE role.memberId = :memberId
                  AND role.roleType = :roleType
                """, Long.class)
            .setParameter("memberId", member.getId())
            .setParameter("roleType", MemberSystemRoleType.SUPER_ADMIN)
            .getSingleResult();

        assertThat(firstCreated).isTrue();
        assertThat(secondCreated).isFalse();
        assertThat(count).isOne();
    }
}
