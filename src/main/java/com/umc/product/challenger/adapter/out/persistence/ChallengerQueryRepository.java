package com.umc.product.challenger.adapter.out.persistence;

import static com.umc.product.challenger.domain.QChallenger.challenger;
import static com.umc.product.challenger.domain.QChallengerPoint.challengerPoint;
import static com.umc.product.member.domain.QMember.member;
import static com.umc.product.organization.domain.QChapter.chapter;
import static com.umc.product.organization.domain.QChapterSchool.chapterSchool;
import static com.umc.product.organization.domain.QGisu.gisu;
import static com.umc.product.organization.domain.QSchool.school;
import static java.util.Objects.requireNonNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.challenger.application.port.in.query.dto.SearchChallengerQuery;
import com.umc.product.challenger.application.port.out.dto.ChallengerSearchBundle;
import com.umc.product.challenger.application.port.out.dto.ChallengerSearchRow;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.QChallenger;
import com.umc.product.challenger.domain.QChallengerPoint;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.domain.QMember;
import com.umc.product.organization.domain.QGisu;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChallengerQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 지부(chapterId)에 속한 챌린저 목록 조회.
     * <p>
     * 지부의 기수와 일치하고, 회원의 학교가 해당 지부 소속 학교에 포함되는 챌린저를 반환합니다.
     */
    public List<Challenger> listByChapterId(Long chapterId) {
        if (chapterId == null) {
            return List.of();
        }
        return queryFactory
            .selectFrom(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .where(chapterIdEq(chapterId))
            .fetch();
    }

    /**
     * 페이지네이션 검색 구현
     */
    public Page<Challenger> pagingSearch(SearchChallengerQuery query, Pageable pageable) {

        BooleanBuilder condition = buildBaseCondition(query);

        List<Challenger> content = queryFactory
            .selectFrom(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .where(condition)
            .orderBy(partOrder(challenger).asc(), challenger.gisuId.desc(), member.name.asc())
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
     * 커서 기반 검색 구현
     */
    public List<Challenger> cursorSearch(SearchChallengerQuery query, Long cursor, int size) {

        BooleanBuilder condition = buildBaseCondition(query);

        // 커서가 주어졌다면 커서 기반 검색 조건을 추가함
        if (cursor != null) {
            condition.and(buildCursorSearchCondition(cursor));
        }

        return queryFactory
            .selectFrom(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .where(condition)
            .orderBy(partOrder(challenger).asc(), challenger.gisuId.desc(), member.name.asc(), challenger.id.asc())
            .limit(size + 1)
            .fetch();
    }

    /**
     * Search Query에 해당하는 전체 조회 결과의 파트별 챌린저 수
     */
    public Map<ChallengerPart, Long> countByPart(SearchChallengerQuery query) {

        BooleanBuilder condition = buildBaseCondition(query);

        List<Tuple> tuples = queryFactory
            .select(challenger.part, challenger.count())
            .from(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .where(condition)
            .groupBy(challenger.part)
            .fetch();

        return tuples.stream()
            .filter(tuple -> tuple.get(challenger.part) != null)
            .collect(Collectors.toMap(
                tuple -> requireNonNull(tuple.get(challenger.part)),
                tuple -> requireNonNull(tuple.get(challenger.count()))
            ));
    }

    public Map<Long, Double> sumPointsByChallengerIds(Set<Long> challengerIds) {
        if (challengerIds == null || challengerIds.isEmpty()) {
            return Map.of();
        }

        NumberExpression<Double> pointSum = pointValueExpression(challengerPoint).sum();

        List<Tuple> tuples = queryFactory
            .select(challengerPoint.challenger.id, pointSum)
            .from(challengerPoint)
            .where(challengerPoint.challenger.id.in(challengerIds))
            .groupBy(challengerPoint.challenger.id)
            .fetch();

        return tuples.stream()
            .filter(tuple -> tuple.get(challengerPoint.challenger.id) != null)
            .collect(Collectors.toMap(
                tuple -> requireNonNull(tuple.get(challengerPoint.challenger.id)),
                tuple -> {
                    Double value = tuple.get(pointSum);
                    return value != null ? value : 0.0;
                }
            ));
    }

    /**
     * 모든 회원의 가장 최근 기수 챌린저 엔티티 조회
     */
    public List<Challenger> getAllLatestGisuPerMember() {
        QChallenger subChallenger = new QChallenger("subChallenger");
        QGisu subGisu = new QGisu("subGisu");

        return queryFactory
            .selectFrom(challenger)
            .join(gisu).on(gisu.id.eq(challenger.gisuId))
            .where(gisu.generation.eq(
                // SubQuery: member에 gisu를 다시 join한 subquery, 거기서 gisu.generation이 max인 것
                JPAExpressions.select(subGisu.generation.max())
                    .from(subChallenger)
                    .join(subGisu).on(subGisu.id.eq(subChallenger.gisuId))
                    .where(subChallenger.memberId.eq(challenger.memberId))
            ))
            .fetch();
    }

    /**
     * 커서 기반 검색 + 파트별 카운트를 하나의 호출로 수행합니다.
     * <p>
     * - 검색 조건(BooleanBuilder)을 한 번만 생성하여 검색/카운트 쿼리에 공유
     * <p>
     * - member + school을 JOIN하여 프로필 정보를 함께 조회 (별도 member 재조회 제거)
     */
    public ChallengerSearchBundle cursorSearchWithCounts(SearchChallengerQuery query, Long cursor, int size) {
        BooleanBuilder condition = buildBaseCondition(query);

        // 파트별 카운트 (같은 condition 공유)
        // 이 partCount는 전체 DB에서 조건에 부합하는 숫자입니당 ~
        Map<ChallengerPart, Long> partCounts = executeCountByPart(condition);

        // 커서 조건 추가
        if (cursor != null) {
            condition.and(buildCursorSearchCondition(cursor));
        }

        // 메인 검색: challenger + member + school JOIN
        List<Tuple> tuples = queryFactory
            .select(
                challenger.id, challenger.memberId, challenger.gisuId,
                challenger.part, challenger.status,
                member.name, member.nickname, school.name, member.profileImageId
            )
            .from(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .leftJoin(school).on(member.schoolId.eq(school.id))
            .where(condition)
            .orderBy(partOrder(challenger).asc(), challenger.gisuId.desc(), member.name.asc(), challenger.id.asc())
            .limit(size + 1)
            .fetch();

        List<ChallengerSearchRow> rows = tuples.stream()
            .map(this::toSearchRow)
            .toList();

        return new ChallengerSearchBundle(rows, partCounts);
    }

    /**
     * 오프셋 기반 검색 + 파트별 카운트를 하나의 호출로 수행합니다.
     */
    public ChallengerSearchBundle pagingSearchWithCounts(SearchChallengerQuery query, Pageable pageable) {
        BooleanBuilder condition = buildBaseCondition(query);

        // 파트별 카운트 (같은 condition 공유)
        Map<ChallengerPart, Long> partCounts = executeCountByPart(condition);

        // 메인 검색: challenger + member + school JOIN
        List<Tuple> tuples = queryFactory
            .select(
                challenger.id, challenger.memberId, challenger.gisuId,
                challenger.part, challenger.status,
                member.name, member.nickname, school.name, member.profileImageId
            )
            .from(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .leftJoin(school).on(member.schoolId.eq(school.id))
            .where(condition)
            .orderBy(partOrder(challenger).asc(), challenger.gisuId.desc(), member.name.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<ChallengerSearchRow> rows = tuples.stream()
            .map(this::toSearchRow)
            .toList();

        return new ChallengerSearchBundle(rows, partCounts);
    }

    /**
     * Tuple을 ChallengerSearchRow로 변환
     */
    private ChallengerSearchRow toSearchRow(Tuple tuple) {
        return ChallengerSearchRow.builder()
            .challengerId(tuple.get(challenger.id))
            .memberId(tuple.get(challenger.memberId))
            .gisuId(tuple.get(challenger.gisuId))
            .part(tuple.get(challenger.part))
            .status(tuple.get(challenger.status))
            .memberName(tuple.get(member.name))
            .memberNickname(tuple.get(member.nickname))
            .schoolName(tuple.get(school.name))
            .profileImageId(tuple.get(member.profileImageId))
            .build();
    }

    /**
     * 주어진 조건으로 파트별 카운트를 실행 (condition 재사용)
     */
    private Map<ChallengerPart, Long> executeCountByPart(BooleanBuilder condition) {
        List<Tuple> tuples = queryFactory
            .select(challenger.part, challenger.count())
            .from(challenger)
            .join(member).on(challenger.memberId.eq(member.id))
            .where(condition)
            .groupBy(challenger.part)
            .fetch();

        Map<ChallengerPart, Long> counts = new EnumMap<>(ChallengerPart.class);
        for (ChallengerPart part : ChallengerPart.values()) {
            counts.put(part, 0L);
        }

        tuples.stream()
            .filter(tuple -> tuple.get(challenger.part) != null)
            .forEach(tuple -> counts.put(
                requireNonNull(tuple.get(challenger.part)),
                requireNonNull(tuple.get(challenger.count()))
            ));

        return counts;
    }

    // ========== PRIVATE METHODS ==========

    private NumberExpression<Double> pointValueExpression(QChallengerPoint point) {
        CaseBuilder.Cases<Double, NumberExpression<Double>> caseBuilder = null;

        // PointType enum의 모든 타입에 대해 case문을 추가
        for (PointType type : PointType.values()) {
            if (caseBuilder == null) {
                caseBuilder = new CaseBuilder()
                    .when(point.type.eq(type)).then(type.getValue());
            } else {
                caseBuilder = caseBuilder
                    .when(point.type.eq(type)).then(type.getValue());
            }
        }

        assert caseBuilder != null;
        return caseBuilder.otherwise(0.0);
    }

    /**
     * 파트별 정렬 순서 결정
     */
    private NumberExpression<Integer> partOrder(QChallenger challenger) {
        return new CaseBuilder()
            .when(challenger.part.eq(ChallengerPart.PLAN)).then(ChallengerPart.PLAN.getSortOrder())
            .when(challenger.part.eq(ChallengerPart.DESIGN)).then(ChallengerPart.DESIGN.getSortOrder())
            .when(challenger.part.eq(ChallengerPart.WEB)).then(ChallengerPart.WEB.getSortOrder())
            .when(challenger.part.eq(ChallengerPart.ANDROID)).then(ChallengerPart.ANDROID.getSortOrder())
            .when(challenger.part.eq(ChallengerPart.IOS)).then(ChallengerPart.IOS.getSortOrder())
            .when(challenger.part.eq(ChallengerPart.NODEJS)).then(ChallengerPart.NODEJS.getSortOrder())
            .when(challenger.part.eq(ChallengerPart.SPRINGBOOT)).then(ChallengerPart.SPRINGBOOT.getSortOrder())
            .otherwise(999);
    }

    /**
     * Challenger와 Member를 기준으로 공통 검색 조건을 생성함
     * <p>
     * 페이지네이션 방식과 관계없이 공통으로 쓰이는 조건
     */
    private BooleanBuilder buildBaseCondition(
        SearchChallengerQuery query
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(challengerIdEq(query.challengerId()));

        // keyword가 있으면 name/nickname 무시하고 keyword로 검색, 없으면 각각 검색
        if (query.keyword() != null && !query.keyword().isBlank()) {
            builder.and(containsKeyword(query.keyword()));
        } else {
            // JavaDoc 읽을 것, 둘 다 주어지면 AND 조건, 하나만 주어지면 해당 조건으로, 없으면 적용 X
            builder.and(containsNicknameAndName(query.nickname(), query.name()));
        }

        builder
            .and(schoolIdEq(query.schoolId())) // 학교 ID가 있다면 일치해야함
            .and(chapterIdEq(query.chapterId())) // 지부 ID가 있다면 일치해야함
            .and(partEq(query.part())) // 파트값이 주어졌다면 일치하여야 함
            .and(gisuIdEq(query.gisuId())) // 기수 값이 주어졌다면 일치하여야 함
            .and(statusIn(query.statuses())); // 챌린저 상태값이 주어졌다면 일치하여야 함

        return builder;
    }

    /**
     * 커서 기반 검색 조건을 추가함
     */
    private BooleanExpression buildCursorSearchCondition(Long cursorId) {
        // 메인 쿼리에서 이미 QChallenger와 QMember의 alias를 사용하고 있어서 새로운 alias 생성
        QChallenger c = new QChallenger("cursorC");
        QMember m = new QMember("cursorM");

        // 커서 챌린저의 정렬 키 값 조회
        Tuple cursorRow = queryFactory
            .select(partOrder(c), c.gisuId, m.name, c.id)
            // 챌린저 테이블에 회원 테이블을 조인함
            .from(c)
            .join(m).on(c.memberId.eq(m.id))
            // 커서 ID (챌린저 ID)가 일치하는 행을 가져옴
            .where(c.id.eq(cursorId))
            .fetchOne();

        // 커서 ID가 유효하지 않은 경우 예외 처리
        if (cursorRow == null) {
            throw new ChallengerDomainException(ChallengerErrorCode.INVALID_CURSOR_ID);
        }

        // 가져온 Cursor Row를 기반으로 각 키 값을
        Integer cursorPartOrder = cursorRow.get(partOrder(c));
        Long cursorGisuId = cursorRow.get(c.gisuId);
        String cursorName = cursorRow.get(m.name);
        Long cursorIdVal = cursorRow.get(c.id);

        NumberExpression<Integer> currentPartOrder = partOrder(challenger); // 해당 챌린저 행의 파트 키 값

        // keyset pagination: 정렬 순서(partOrder ASC, gisuId DESC, name ASC, id ASC) 기반
        return currentPartOrder.gt(cursorPartOrder) // 가장 먼저 정렬되는 키: 파트 순서가 더 크거나
            .or(
                currentPartOrder.eq(cursorPartOrder)
                    .and(challenger.gisuId.lt(cursorGisuId)) // 두 번째: 파트 순서가 같고, 기수 ID가 더 크거나
            )
            .or(
                currentPartOrder.eq(cursorPartOrder)
                    .and(challenger.gisuId.eq(cursorGisuId))
                    .and(member.name.gt(cursorName)) // 세 번째: 기수 ID까지 같고, 이름값이 사전순으로 더 크거나
            )
            .or(
                currentPartOrder.eq(cursorPartOrder)
                    .and(challenger.gisuId.eq(cursorGisuId))
                    .and(member.name.eq(cursorName))
                    .and(challenger.id.gt(cursorIdVal)) // 네 번째: 이름까지 같으면 챌린저 ID가 큰 것을 기준으로
            );
    }

    /**
     * 키워드 검색, 이름 또는 닉네임이 포함하는지
     */
    private BooleanExpression containsKeyword(String keyword) {
        return member.nickname.containsIgnoreCase(keyword)
            .or(member.name.containsIgnoreCase(keyword));
    }

    /**
     * 이름과 닉네임을 포함하는지 검색
     * <p>
     * 둘 다 주어진 경우 AND로 검색함. 둘 중 하나만 주어진 경우에는 해당 조건으로 검색함
     */
    private BooleanExpression containsNicknameAndName(String nickname, String name) {
        // 닉네임 관련 조건, 닉네임이 비어있지 않다면 조건 생성
        BooleanExpression nicknameCondition =
            (nickname != null && !nickname.isBlank())
                ? member.nickname.containsIgnoreCase(nickname)
                : null;

        // 이름 관련 조건, 이름이 비어있지 않다면 조건 생성
        BooleanExpression nameCondition =
            (name != null && !name.isBlank())
                ? member.name.containsIgnoreCase(name)
                : null;

        // 닉네임 및 이름 조건이 모두 존재하는 경우 (둘 다 주어진 경우) AND 조건으로 결합
        if (nicknameCondition != null && nameCondition != null) {
            return nicknameCondition.and(nameCondition);
        }

        // 닉네임 조건이 존재하는 경우 반환
        if (nicknameCondition != null) {
            return nicknameCondition;
        }

        return nameCondition;
    }

    /**
     * 챌린저 ID가 일치하는지
     */
    private BooleanExpression challengerIdEq(Long challengerId) {
        return challengerId != null
            ? challenger.id.eq(challengerId)
            : null;
    }

    /**
     * 기수 ID가 일치하는지
     */
    private BooleanExpression gisuIdEq(Long gisuId) {
        return gisuId != null
            ? challenger.gisuId.eq(gisuId)
            : null;
    }

    /**
     * 파트가 일치하는지
     */
    private BooleanExpression partEq(ChallengerPart part) {
        return part != null
            ? challenger.part.eq(part)
            : null;
    }

    /**
     * 학교 ID가 일치하는지
     */
    private BooleanExpression schoolIdEq(Long schoolId) {
        return schoolId != null
            ? member.schoolId.eq(schoolId)
            : null;
    }

    private BooleanExpression chapterIdEq(Long chapterId) {
        // 메인 쿼리 = challenger에 member만 JOIN되어 있음
        // 그 중에서 지부가 일치하는걸 검색해야 하는거임

        return chapterId != null
            ? challenger.gisuId.eq( // 2. 챌린저의 기수와 일치해야함
                // 1. 주어진 지부의 기수가
                JPAExpressions
                    .select(chapter.gisu.id)
                    .from(chapter)
                    .where(chapter.id.eq(chapterId))
            )
            // 회원의 학교가
            .and(
                member.schoolId.in( // 3. 이 member의 schoolId 안에 있어야 하고
                    JPAExpressions
                        .select(chapterSchool.school.id) // 2. 에서 schoolId를 가져옴
                        .from(chapterSchool)
                        .where(chapterSchool.chapter.id.eq(chapterId)) // 1. chapterSchool에서 chapterId가 주어진 id인 행
                )
            )
            : null;
    }

    /**
     * 챌린저 상태값이 일치하는지
     */
    private BooleanExpression statusIn(List<ChallengerStatus> statuses) {
        return (statuses != null && !statuses.isEmpty()) ? challenger.status.in(statuses) : null;
    }

    /**
     * 회원이 속한 학교가 속했던 지부 ID들이 검색 조건의 지부 ID와 일치하는지
     * <p>
     * TODO: 이거 이렇게 제한하면 안 될 것 같아요 - 경운
     */
    private BooleanExpression chapterIdExists(Long chapterId, QMember member) {
        if (chapterId == null) {
            return null;
        }

        return JPAExpressions
            .selectOne()
            .from(chapterSchool) // 지부-학교 매핑 테이블에서
            // WHERE 회원 학교 = 매핑된 학교 AND 매핑된 지부 = 검색 조건의 지부
            .where(
                chapterSchool.school.id.eq(member.schoolId),
                chapterSchool.chapter.id.eq(chapterId)
            )
            .exists();
    }
}
