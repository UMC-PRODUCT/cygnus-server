package com.umc.product.authorization.adapter.out.persistence;

import static com.umc.product.authorization.domain.QChallengerRole.challengerRole;
import static com.umc.product.challenger.domain.QChallenger.challenger;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerRoleType;

import lombok.RequiredArgsConstructor;

/**
 * ChallengerRole QueryDSL Repository
 */
@Repository
@RequiredArgsConstructor
public class ChallengerRoleQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * memberId로 모든 Role 조회
     *
     * @param memberId 사용자 ID
     * @return Role 리스트
     */
    public List<ChallengerRole> findByMemberId(Long memberId) {
        return queryFactory
            .selectFrom(challengerRole)
            .join(challenger).on(challengerRole.challengerId.eq(challenger.id))
            .where(challenger.memberId.eq(memberId))
            .fetch();
    }

    /**
     * memberId와 gisuId로 Role 조회
     *
     * @param memberId 사용자 ID
     * @param gisuId   기수 ID
     * @return Role 리스트
     */
    public List<ChallengerRole> findByMemberIdAndGisuId(Long memberId, Long gisuId) {
        return queryFactory
            .selectFrom(challengerRole)
            .join(challenger).on(challengerRole.challengerId.eq(challenger.id))
            .where(
                challenger.memberId.eq(memberId),
                challengerRole.gisuId.eq(gisuId)
            )
            .fetch();
    }

    /**
     * 특정 챌린저가 특정 기수·조직에서 동일 역할을 이미 보유하고 있는지 여부.
     * <p>
     * organizationId 는 CENTRAL 역할의 경우 null 이므로, null 이면 IS NULL 로 비교한다.
     *
     * @param challengerId   챌린저 ID
     * @param roleType       역할 타입
     * @param organizationId 조직 ID (CENTRAL 이면 null)
     * @param gisuId         기수 ID
     * @return 동일 역할이 이미 있으면 true
     */
    public boolean existsByChallengerRole(
        Long challengerId, ChallengerRoleType roleType, Long organizationId, Long gisuId
    ) {
        BooleanBuilder condition = new BooleanBuilder();
        condition.and(challengerRole.challengerId.eq(challengerId));
        condition.and(challengerRole.challengerRoleType.eq(roleType));
        condition.and(challengerRole.gisuId.eq(gisuId));
        if (organizationId == null) {
            condition.and(challengerRole.organizationId.isNull());
        } else {
            condition.and(challengerRole.organizationId.eq(organizationId));
        }

        Integer found = queryFactory
            .selectOne()
            .from(challengerRole)
            .where(condition)
            .fetchFirst();

        return found != null;
    }
}
