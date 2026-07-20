package com.umc.product.authorization.adapter.out.persistence;

import static com.umc.product.authorization.domain.QChallengerRole.challengerRole;
import static com.umc.product.support.fixture.AuthorizationFixture.중앙_역할;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@DisplayName("ChallengerRoleQueryRepository")
class ChallengerRoleQueryRepositoryTest {

    JPAQueryFactory queryFactory;
    JPAQuery<ChallengerRole> query;
    ChallengerRoleQueryRepository sut;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        queryFactory = mock(JPAQueryFactory.class);
        query = mock(JPAQuery.class, RETURNS_SELF);
        given(queryFactory.selectFrom(challengerRole)).willReturn(query);
        sut = new ChallengerRoleQueryRepository(queryFactory);
    }

    @Test
    @DisplayName("회원 ID로 join된 역할 목록을 조회한다")
    void find_by_member_id() {
        List<ChallengerRole> roles = List.of(
            중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, 9L));
        given(query.fetch()).willReturn(roles);

        assertThat(sut.findByMemberId(1L)).isSameAs(roles);
    }

    @Test
    @DisplayName("회원 ID와 기수 ID로 join된 역할 목록을 조회한다")
    void find_by_member_id_and_gisu_id() {
        List<ChallengerRole> roles = List.of(
            중앙_역할(ChallengerRoleType.CENTRAL_PRESIDENT, 9L));
        given(query.fetch()).willReturn(roles);

        assertThat(sut.findByMemberIdAndGisuId(1L, 9L)).isSameAs(roles);
    }
}
