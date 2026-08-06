package com.umc.product.member.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.QChallenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.member.application.dto.MemberSearchAccessScope;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.out.dto.MemberInvitationCandidate;
import com.umc.product.member.application.port.out.dto.MemberInvitationCandidatePage;
import com.umc.product.member.application.port.out.dto.SearchMemberInvitationCondition;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.QMember;
import com.umc.product.organization.domain.QChapterSchool;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Long countAll() {
        return queryFactory
            .selectFrom(QMember.member)
            .stream().count();
    }

    public Optional<Member> findByIdWithPessimisticLock(Long id) {
        return Optional.ofNullable(queryFactory
            .selectFrom(QMember.member)
            .where(QMember.member.id.eq(id))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .fetchOne()
        );
    }

    public Optional<Member> findByNickname(String nickname) {
        return Optional.ofNullable(queryFactory
            .selectFrom(QMember.member)
            .where(QMember.member.nickname.eq(nickname))
            .fetchFirst()
        );
    }

    /**
     * 검색 로직 (v1) — 챌린저 단위 row.
     */
    public Page<Challenger> searchBy(SearchMemberQuery query, Pageable pageable) {
        QChallenger challenger = QChallenger.challenger;
        QMember member = QMember.member;

        BooleanBuilder condition = buildSearchCondition(query, challenger, member);

        List<Challenger> content = queryFactory
            .selectFrom(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .where(condition)
            .orderBy(challenger.gisuId.desc(), member.schoolId.asc(), member.name.asc(), challenger.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(challenger.count())
            .from(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .where(condition)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 검색 로직 (v2) — 회원 단위 distinct memberId.
     * <p>
     * 같은 회원이 여러 챌린저 이력을 가져도 1개 row로만 반환됩니다.
     * gisuId/part 같은 챌린저 필터는 "회원이 한 번이라도 그 조건을 만족하는 챌린저를 가졌는가"로 매칭됩니다.
     */
    public Page<Long> searchMemberIdsBy(SearchMemberQuery query, Pageable pageable) {
        QChallenger challenger = QChallenger.challenger;
        QMember member = QMember.member;

        BooleanBuilder condition = buildSearchCondition(query, challenger, member);

        // PostgreSQL은 SELECT DISTINCT 사용 시 ORDER BY 표현식이 SELECT 목록에 포함되어 있어야 합니다.
        // member 테이블의 컬럼은 같은 member.id 행에서 항상 동일한 값을 가지므로,
        // (id, schoolId, name) 묶음에 distinct를 걸어도 결과는 member.id 단위 distinct와 동일하면서
        // 표준 SQL 규칙도 만족합니다.
        List<Tuple> rows = queryFactory
            .select(member.id, member.schoolId, member.name)
            .distinct()
            .from(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .where(condition)
            .orderBy(member.schoolId.asc(), member.name.asc(), member.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<Long> content = rows.stream()
            .map(t -> t.get(member.id))
            .toList();

        Long total = queryFactory
            .select(member.id.countDistinct())
            .from(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .where(condition)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    public Page<Long> searchMemberIdsBy(
        SearchMemberQuery query,
        MemberSearchAccessScope scope,
        Pageable pageable
    ) {
        if (scope.denied()) {
            return Page.empty(pageable);
        }
        if (scope.unrestricted()) {
            return searchMemberIdsBy(query, pageable);
        }

        QChallenger challenger = QChallenger.challenger;
        QMember member = QMember.member;
        BooleanBuilder condition = buildSearchCondition(query, challenger, member);
        condition.and(buildAccessCondition(scope, challenger, member));

        List<Tuple> rows = queryFactory
            .select(member.id, member.schoolId, member.name)
            .distinct()
            .from(member)
            .leftJoin(challenger).on(challenger.memberId.eq(member.id))
            .where(condition)
            .orderBy(member.schoolId.asc(), member.name.asc(), member.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<Long> content = rows.stream()
            .map(tuple -> tuple.get(member.id))
            .toList();

        Long total = queryFactory
            .select(member.id.countDistinct())
            .from(member)
            .leftJoin(challenger).on(challenger.memberId.eq(member.id))
            .where(condition)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    public MemberInvitationCandidatePage searchInvitationCandidates(
        SearchMemberInvitationCondition condition
    ) {
        QMember member = QMember.member;
        BooleanBuilder predicate = new BooleanBuilder()
            .and(member.status.eq(MemberStatus.ACTIVE))
            .and(memberNameContains(condition.keyword(), member))
            .and(memberIdNotIn(condition.excludedMemberIds(), member));

        List<MemberInvitationCandidate> items = queryFactory
            .select(member.id, member.name)
            .from(member)
            .where(predicate)
            .orderBy(member.name.asc(), member.id.asc())
            .offset(condition.offset())
            .limit(condition.limit())
            .fetch()
            .stream()
            .map(row -> new MemberInvitationCandidate(
                row.get(member.id),
                row.get(member.name)
            ))
            .toList();

        Long total = queryFactory
            .select(member.count())
            .from(member)
            .where(predicate)
            .fetchOne();

        return new MemberInvitationCandidatePage(items, total != null ? total : 0L);
    }

    public Set<Long> findActiveMemberIds(Set<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Set.of();
        }

        QMember member = QMember.member;
        return Set.copyOf(queryFactory
            .select(member.id)
            .from(member)
            .where(
                member.id.in(memberIds),
                member.status.eq(MemberStatus.ACTIVE)
            )
            .fetch());
    }

    // ========= PRIVATE ============

    // 필터링
    private BooleanBuilder buildSearchCondition(SearchMemberQuery query, QChallenger challenger, QMember member) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(keywordContains(query.keyword(), member));
        builder.and(schoolIdEq(query.schoolId(), member));
        builder.and(chapterIdExists(query.chapterId(), member));
        builder.and(gisuIdEq(query.gisuId(), challenger));
        builder.and(partEq(query.part(), challenger));

        return builder;
    }

    private BooleanExpression buildAccessCondition(
        MemberSearchAccessScope scope,
        QChallenger challenger,
        QMember member
    ) {
        BooleanExpression schoolCondition = scope.allowedSchoolIds().isEmpty()
            ? null
            : member.schoolId.in(scope.allowedSchoolIds());
        BooleanExpression gisuCondition = scope.allowedGisuIds().isEmpty()
            ? null
            : challenger.gisuId.in(scope.allowedGisuIds());

        if (schoolCondition == null) {
            return gisuCondition != null ? gisuCondition : member.id.isNull();
        }
        return gisuCondition == null ? schoolCondition : schoolCondition.or(gisuCondition);
    }

    // 검색 관련 조건
    private BooleanExpression keywordContains(String keyword, QMember member) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return member.name.containsIgnoreCase(keyword)
            .or(member.nickname.containsIgnoreCase(keyword))
            .or(member.email.containsIgnoreCase(keyword));
    }

    private BooleanExpression memberNameContains(String keyword, QMember member) {
        return keyword == null || keyword.isBlank()
            ? null
            : member.name.containsIgnoreCase(keyword);
    }

    private BooleanExpression memberIdNotIn(Set<Long> excludedMemberIds, QMember member) {
        return excludedMemberIds == null || excludedMemberIds.isEmpty()
            ? null
            : member.id.notIn(excludedMemberIds);
    }

    private BooleanExpression chapterIdExists(Long chapterId, QMember member) {
        if (chapterId == null) {
            return null;
        }

        QChapterSchool chapterSchool = QChapterSchool.chapterSchool;

        return JPAExpressions
            .selectOne()
            .from(chapterSchool)
            .where(
                chapterSchool.school.id.eq(member.schoolId),
                chapterSchool.chapter.id.eq(chapterId)
            )
            .exists();
    }

    private BooleanExpression gisuIdEq(Long gisuId, QChallenger challenger) {
        return gisuId != null ? challenger.gisuId.eq(gisuId) : null;
    }

    private BooleanExpression partEq(ChallengerPart part, QChallenger challenger) {
        return part != null ? challenger.part.eq(part) : null;
    }

    private BooleanExpression schoolIdEq(Long schoolId, QMember member) {
        return schoolId != null ? member.schoolId.eq(schoolId) : null;
    }

}
