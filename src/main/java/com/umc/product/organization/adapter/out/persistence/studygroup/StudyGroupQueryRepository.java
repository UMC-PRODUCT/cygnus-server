package com.umc.product.organization.adapter.out.persistence.studygroup;

import static com.umc.product.organization.domain.QStudyGroup.studyGroup;
import static com.umc.product.organization.domain.QStudyGroupMember.studyGroupMember;
import static com.umc.product.organization.domain.QStudyGroupMentor.studyGroupMentor;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.query.dto.OrganizationRoleScope;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupHeaderInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupMemberPageInfo;
import com.umc.product.organization.application.port.in.query.dto.studygroup.StudyGroupNameInfo;
import com.umc.product.organization.domain.QStudyGroupMember;
import com.umc.product.organization.domain.QStudyGroupMentor;
import com.umc.product.organization.domain.StudyGroup;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudyGroupQueryRepository {

    private final JPAQueryFactory queryFactory;

    // ============================================================================
    // Aggregate root 조회 (자식 컬렉션 포함)
    // ============================================================================

    /**
     * Aggregate root {@link StudyGroup} 을 자식 collection (members / mentors) 까지 함께 로드한다.
     *
     * <h3>왜 fetch join 을 두 쿼리로 나누는가</h3>
     * 두 컬렉션 모두 {@code @OneToMany List<>} 이고, JPA 는 이를 "bag"(순서 없음 + 중복 허용) 으로 취급한다. 한 쿼리에 두 bag 을 동시에 fetch join 하면
     * Hibernate 가 {@code MultipleBagFetchException} 을 던진다 — bag 들의 cartesian product 가 결과 row 를 부풀려 컬렉션 hydration 이
     * 모호해지기 때문이다.
     * <p>
     * 회피 옵션 두 가지 중 도메인 엔티티의 컬렉션 타입(List)을 유지하기 위해 *두 쿼리 분리* 를 택했다 (List → Set 변경 회피).
     * cartesian product 도 회피되어 큰 그룹에서 row 폭증 위험이 없다.
     *
     * <h3>왜 두 번째 쿼리의 반환값을 받지 않는가</h3>
     * Hibernate 의 영속성 컨텍스트(1차 캐시)는 한 트랜잭션 안에서 같은 (Entity Class, PK) 조합에 대해 *동일 자바 인스턴스* 를 보장한다. 두 번째 쿼리의 결과는
     * 첫 쿼리에서 들고있는 그 인스턴스의 mentors 컬렉션을 부수효과로 초기화한다.
     */
    public Optional<StudyGroup> findEntityById(Long id) {
        StudyGroup group = queryFactory
            .selectFrom(studyGroup)
            .leftJoin(studyGroup.members).fetchJoin()
            .where(studyGroup.id.eq(id))
            .fetchOne();
        if (group == null) {
            return Optional.empty();
        }

        // 1차 캐시의 동일 인스턴스(group)의 mentors 컬렉션을 채우는 부수효과만 노린 쿼리.
        queryFactory
            .selectFrom(studyGroup)
            .leftJoin(studyGroup.mentors).fetchJoin()
            .where(studyGroup.id.eq(id))
            .fetchOne();

        return Optional.of(group);
    }

    public Optional<StudyGroup> findEntityByMemberIdAndGisuIdAndPart(
        Long memberId,
        Long gisuId,
        ChallengerPart part
    ) {
        Long groupId = queryFactory
            .select(studyGroup.id)
            .from(studyGroupMember)
            .join(studyGroupMember.studyGroup, studyGroup)
            .where(
                studyGroupMember.memberId.eq(memberId),
                studyGroup.gisuId.eq(gisuId),
                studyGroup.part.eq(part)
            )
            .fetchFirst();
        return groupId == null ? Optional.empty() : findEntityById(groupId);
    }

    public Optional<StudyGroup> findEntityByMemberIdAndGisuIdAndTrack(
        Long memberId,
        Long gisuId,
        ChallengerTrack track
    ) {
        Long groupId = queryFactory
            .select(studyGroup.id)
            .from(studyGroupMember)
            .join(studyGroupMember.studyGroup, studyGroup)
            .where(
                studyGroupMember.memberId.eq(memberId),
                studyGroup.gisuId.eq(gisuId),
                studyGroup.track.eq(track)
            )
            .fetchFirst();
        return groupId == null ? Optional.empty() : findEntityById(groupId);
    }

    // ============================================================================
    // Scope 기반 페이징 조회 — 헤더만 반환. 멤버/멘토 ID 는 별도 batch 메서드로 분리.
    // ============================================================================

    /**
     * 역할 Scope 기반 스터디 그룹 헤더 목록 (커서 페이지네이션).
     * <p>
     * 헤더만 반환하고 각 그룹의 멤버/멘토 ID 는 {@link #findMemberIdsByStudyGroupIds} / {@link #findMentorIdsByStudyGroupIds} 가 batch 로
     * 채워준다. Service 는 그 결과 + Member 도메인 UseCase 로 이름/학교/프로필 합성. cross-domain JOIN 회피.
     * <p>
     * 멤버/멘토 JOIN 을 안 하는 이유: 멤버가 포함되면 한 그룹이 멤버 수만큼 중복 row 로 나와 {@code LIMIT size} 가 "멤버 row 기준 size" 로 해석되어
     * 페이지 의미가 붕괴된다. 그룹 단위로 size 를 정확히 자르기 위해 분리.
     *
     * @return scope 가 모두 비어 predicate 가 null 이면 빈 리스트 (풀스캔 방지)
     */
    public List<StudyGroupHeaderInfo> findStudyGroupHeaders(
        List<OrganizationRoleScope> scopes, Long gisuId,
        Long cursor, int size
    ) {
        BooleanExpression scopePredicate = buildScopePredicate(scopes);
        if (scopePredicate == null) {
            return List.of();
        }

        return queryFactory
            .select(Projections.constructor(
                StudyGroupHeaderInfo.class,
                studyGroup.id,
                studyGroup.name,
                studyGroup.gisuId,
                studyGroup.part,
                studyGroup.createdAt,
                studyGroup.track
            ))
            .from(studyGroup)
            .where(
                studyGroup.gisuId.eq(gisuId),
                scopePredicate,
                descCursorCondition(cursor)
            )
            .orderBy(studyGroup.id.desc())
            .limit(size)
            .fetch();
    }

    /**
     * 여러 스터디 그룹의 memberId 들을 한 번에 batch 조회. study_group_member 테이블만 본다 (cross-domain JOIN 없음).
     * <p>
     * 결과를 {@code groupId → List<memberId>} 맵으로 묶어 반환하므로 Service 에서 그룹별 멤버를 즉시 찾을 수 있다. 멤버가 없는 그룹은 키 자체가 맵에 없다.
     */
    public Map<Long, List<Long>> findMemberIdsByStudyGroupIds(Collection<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        return queryFactory
            .select(studyGroupMember.studyGroup.id, studyGroupMember.memberId)
            .from(studyGroupMember)
            .where(studyGroupMember.studyGroup.id.in(groupIds))
            .fetch().stream()
            .collect(Collectors.groupingBy(
                tuple -> tuple.get(studyGroupMember.studyGroup.id),
                LinkedHashMap::new,
                Collectors.mapping(
                    tuple -> tuple.get(studyGroupMember.memberId),
                    Collectors.toList()
                )
            ));
    }

    /**
     * 여러 스터디 그룹에 속한 스터디원을 커서 페이지네이션으로 조회한다. 반환 순서는 {@code study_group_member.id} 오름차순.
     * <p>
     * {@link #findMemberIdsByStudyGroupIds} 는 그룹별로 전량을 반환하므로 목록 화면처럼 인원이 많은 경우에 쓸 수 없어 분리했다.
     * <p>
     * 커서를 {@code study_group_member.id} 로 잡은 이유: 이 테이블은 재등록 시 새 행이 생기고 기존 행은 삭제되므로 id 가 단조 증가하며 중복되지 않는다. memberId 를
     * 커서로 쓰면 여러 그룹에 같은 멤버가 있을 때 페이지가 어긋난다.
     *
     * @param groupIds 조회 대상 그룹 ID (호출 측에서 권한 Scope 로 이미 좁혀진 집합)
     * @param cursor   직전 페이지 마지막 studyGroupMemberId (첫 페이지는 null)
     * @param size     조회 건수. hasNext 판별이 필요하면 호출 측에서 +1 하여 전달한다.
     */
    public List<StudyGroupMemberPageInfo> findStudyGroupMemberPage(
        Collection<Long> groupIds, Long cursor, int size
    ) {
        if (groupIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
            .select(Projections.constructor(
                StudyGroupMemberPageInfo.class,
                studyGroupMember.id,
                studyGroup.id,
                studyGroup.name,
                studyGroup.part,
                studyGroupMember.memberId,
                studyGroup.track
            ))
            .from(studyGroupMember)
            .join(studyGroupMember.studyGroup, studyGroup)
            .where(
                studyGroup.id.in(groupIds),
                ascCursorCondition(cursor)
            )
            .orderBy(studyGroupMember.id.asc())
            .limit(size)
            .fetch();
    }

    /**
     * 여러 스터디 그룹의 멘토 memberId 들을 한 번에 batch 조회. study_group_mentor 테이블만 본다 (cross-domain JOIN 없음).
     */
    public Map<Long, List<Long>> findMentorIdsByStudyGroupIds(Collection<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        return queryFactory
            .select(studyGroupMentor.studyGroup.id, studyGroupMentor.memberId)
            .from(studyGroupMentor)
            .where(studyGroupMentor.studyGroup.id.in(groupIds))
            .fetch().stream()
            .collect(Collectors.groupingBy(
                tuple -> tuple.get(studyGroupMentor.studyGroup.id),
                LinkedHashMap::new,
                Collectors.mapping(
                    tuple -> tuple.get(studyGroupMentor.memberId),
                    Collectors.toList()
                )
            ));
    }

    // ============================================================================
    // 이름 목록 조회 (드롭다운 UI 용)
    // ============================================================================

    /**
     * 역할 Scope 기반 (groupId, name) 목록. 페이지네이션 없이 전체 반환.
     * Scope 가 모두 비어 합성 predicate 가 null 이면 빈 리스트.
     */
    public List<StudyGroupNameInfo> findStudyGroupNames(List<OrganizationRoleScope> scopes, Long gisuId) {
        BooleanExpression scopePredicate = buildScopePredicate(scopes);
        if (scopePredicate == null) {
            return List.of();
        }

        return queryFactory
            .select(Projections.constructor(
                StudyGroupNameInfo.class,
                studyGroup.id,
                studyGroup.name
            ))
            .from(studyGroup)
            .where(
                studyGroup.gisuId.eq(gisuId),
                scopePredicate
            )
            .orderBy(studyGroup.name.asc())
            .fetch();
    }

    /**
     * Scope 기반 조회 대상 스터디 그룹 ID 집합. 다른 aggregate (Schedule 등) 가 "사용자에게 보이는 그룹" 만 필요할 때 사용.
     * 합성 predicate 가 null 이면 빈 set (풀스캔 방지).
     */
    public Set<Long> findStudyGroupIds(List<OrganizationRoleScope> scopes, Long gisuId) {
        BooleanExpression scopePredicate = buildScopePredicate(scopes);
        if (scopePredicate == null) {
            return Set.of();
        }

        return new HashSet<>(queryFactory
            .select(studyGroup.id)
            .from(studyGroup)
            .where(
                studyGroup.gisuId.eq(gisuId),
                scopePredicate
            )
            .fetch()
        );
    }

    // ============================================================================
    // Scope predicate 합성
    // ============================================================================

    /**
     * Scope 리스트를 OR 결합한 WHERE 조건으로 변환. 각 Scope 가 null predicate (예: 빈 schoolMemberIds) 를 내면 OR 결합에서 제외.
     * 최종 결과가 null 이면 "조회 가능한 Scope 가 하나도 없음" → 호출 측에서 빈 리스트 반환 (풀스캔 방지).
     */
    private BooleanExpression buildScopePredicate(List<OrganizationRoleScope> scopes) {
        return scopes.stream()
            .map(this::toScopePredicate)
            .filter(Objects::nonNull)
            .reduce(BooleanExpression::or)
            .orElse(null);
    }

    /**
     * sealed interface 의 switch 가 exhaustive → 새 Scope 추가 시 컴파일 에러로 누락 방지.
     */
    private BooleanExpression toScopePredicate(OrganizationRoleScope scope) {
        return switch (scope) {
            case OrganizationRoleScope.AsSchoolCore s -> schoolCoreExists(s.schoolMemberIds());
            case OrganizationRoleScope.AsPartLeader s -> partLeaderExists(s.memberId());
        };
    }

    /**
     * 학교 회장단 Scope: "학교 멤버가 *멤버 혹은 멘토* 로 등록된 스터디 그룹" EXISTS 서브쿼리.
     * <p>
     * 두 case 를 OR 결합하여, 다음 둘 중 하나라도 충족하면 visible:
     * <ul>
     *   <li>study_group_member 에 학교 멤버 존재</li>
     *   <li>study_group_mentor 에 학교 멤버 존재</li>
     * </ul>
     * 빈 ID 셋 → null 반환 (OR 결합에서 제외).
     */
    private BooleanExpression schoolCoreExists(Set<Long> schoolMemberIds) {
        if (schoolMemberIds == null || schoolMemberIds.isEmpty()) {
            return null;
        }
        QStudyGroupMember m = new QStudyGroupMember("m_core");
        QStudyGroupMentor o = new QStudyGroupMentor("o_core");

        BooleanExpression memberMatch = JPAExpressions
            .selectOne()
            .from(m)
            .where(m.studyGroup.eq(studyGroup).and(m.memberId.in(schoolMemberIds)))
            .exists();

        BooleanExpression mentorMatch = JPAExpressions
            .selectOne()
            .from(o)
            .where(o.studyGroup.eq(studyGroup).and(o.memberId.in(schoolMemberIds)))
            .exists();

        return memberMatch.or(mentorMatch);
    }

    /** 파트장 Scope: "해당 memberId 가 mentor 로 등록된 스터디 그룹" EXISTS 서브쿼리. */
    private BooleanExpression partLeaderExists(Long memberId) {
        if (memberId == null) {
            return null;
        }
        QStudyGroupMentor o = new QStudyGroupMentor("o_leader");
        return JPAExpressions
            .selectOne()
            .from(o)
            .where(o.studyGroup.eq(studyGroup).and(o.memberId.eq(memberId)))
            .exists();
    }

    /** id DESC 정렬 기준 커서. cursor null 이면 조건 미적용 (첫 페이지). */
    private BooleanExpression descCursorCondition(Long cursor) {
        return cursor != null ? studyGroup.id.lt(cursor) : null;
    }

    /** study_group_member.id ASC 정렬 기준 커서. cursor null 이면 조건 미적용 (첫 페이지). */
    private BooleanExpression ascCursorCondition(Long cursor) {
        return cursor != null ? studyGroupMember.id.gt(cursor) : null;
    }

    // ============================================================================
    // 멤버 중복 검사 (Command 검증용)
    // ============================================================================

    public Set<Long> findConflictedMemberIds(
        Long gisuId, ChallengerPart part, Set<Long> memberIds, Long excludedStudyGroupId
    ) {
        return new HashSet<>(queryFactory
            .select(studyGroupMember.memberId)
            .from(studyGroupMember)
            .join(studyGroupMember.studyGroup, studyGroup)
            .where(
                studyGroup.gisuId.eq(gisuId),
                studyGroup.part.eq(part),
                studyGroupMember.memberId.in(memberIds),
                excludedStudyGroupCondition(excludedStudyGroupId)
            )
            .fetch()
        );
    }

    public Set<Long> findConflictedTrackMemberIds(
        Long gisuId, ChallengerTrack track, Set<Long> memberIds, Long excludedStudyGroupId
    ) {
        return new HashSet<>(queryFactory
            .select(studyGroupMember.memberId)
            .from(studyGroupMember)
            .join(studyGroupMember.studyGroup, studyGroup)
            .where(
                studyGroup.gisuId.eq(gisuId),
                studyGroup.track.eq(track),
                studyGroupMember.memberId.in(memberIds),
                excludedStudyGroupCondition(excludedStudyGroupId)
            )
            .fetch()
        );
    }

    private BooleanExpression excludedStudyGroupCondition(Long excludedStudyGroupId) {
        return excludedStudyGroupId != null ? studyGroup.id.ne(excludedStudyGroupId) : null;
    }
}
