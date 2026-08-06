package com.umc.product.organization.adapter.out.persistence.school;

import static com.umc.product.organization.domain.QChapter.chapter;
import static com.umc.product.organization.domain.QChapterSchool.chapterSchool;
import static com.umc.product.organization.domain.QGisu.gisu;
import static com.umc.product.organization.domain.QSchool.school;
import static com.umc.product.organization.domain.QSchoolLink.schoolLink;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterNameInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolGisuChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolListItemInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolSearchCondition;
import com.umc.product.organization.domain.School;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SchoolQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<SchoolListItemInfo> getSchools(SchoolSearchCondition condition, Pageable pageable) {
        JPQLQuery<Long> activeChapterIds = activeChapterIdSubQuery();

        List<SchoolListItemInfo> content = queryFactory
            .select(Projections.constructor(SchoolListItemInfo.class,
                school.id,
                school.name,
                school.shortName,
                chapter.id,      // 활성 기수에 속하지 않으면 null
                chapter.name,    // 활성 기수에 속하지 않으면 null
                school.createdAt,
                chapter.id.isNotNull(),  // 활성 기수의 ChapterSchool 존재 여부
                school.remark,
                school.logoImageId       // 서비스 레이어에서 URL로 변환
            ))
            .from(school)
            .leftJoin(chapterSchool).on(
                chapterSchool.school.eq(school)
                    .and(chapterSchool.chapter.id.in(activeChapterIds))
            )
            .leftJoin(chapterSchool.chapter, chapter)
            .where(
                keywordContains(condition.keyword()),
                chapterIdEq(condition.chapterId())
            )
            .orderBy(school.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(school.count())
            .from(school)
            .leftJoin(chapterSchool).on(
                chapterSchool.school.eq(school)
                    .and(chapterSchool.chapter.id.in(activeChapterIds))
            )
            .leftJoin(chapterSchool.chapter, chapter)
            .where(
                keywordContains(condition.keyword()),
                chapterIdEq(condition.chapterId())
            );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword) ? school.name.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression chapterIdEq(Long chapterId) {
        return chapterId != null ? chapter.id.eq(chapterId) : null;
    }

    private JPQLQuery<Long> activeChapterIdSubQuery() {
        return JPAExpressions
            .select(chapter.id)
            .from(chapter)
            .join(chapter.gisu, gisu)
            .where(gisu.isActive.isTrue());
    }

    /**
     * 학교 상세 정보를 반환합니다.
     */
    public SchoolChapterInfo getSchoolDetail(Long schoolId) {
        JPQLQuery<Long> activeChapterIds = activeChapterIdSubQuery();

        return queryFactory
            .select(Projections.constructor(SchoolChapterInfo.class,
                chapter.id,
                chapter.name,
                school.name,
                school.shortName,
                school.id,
                school.remark,
                school.logoImageId,
                chapter.id.isNotNull(),
                school.createdAt,
                school.updatedAt
            ))
            .from(school)
            .leftJoin(chapterSchool).on(
                chapterSchool.school.eq(school)
                    .and(chapterSchool.chapter.id.in(activeChapterIds))
            )
            .leftJoin(chapterSchool.chapter, chapter)
            .where(school.id.eq(schoolId))
            .fetchFirst();
    }

    public List<SchoolChapterInfo> getSchoolDetailsByGisuId(Long gisuId) {
        return queryFactory
            .select(Projections.constructor(SchoolChapterInfo.class,
                chapter.id,
                chapter.name,
                school.name,
                school.shortName,
                school.id,
                school.remark,
                school.logoImageId,
                gisu.isActive,
                school.createdAt,
                school.updatedAt
            ))
            .from(school)
            .join(chapterSchool).on(chapterSchool.school.eq(school))
            .join(chapterSchool.chapter, chapter)
            .join(chapter.gisu, gisu)
            .where(gisu.id.eq(gisuId))
            .fetch();
    }

    public List<SchoolChapterNameInfo> getSchoolChapterNamesByGisuId(Long gisuId) {
        return queryFactory
            .select(Projections.constructor(SchoolChapterNameInfo.class,
                chapter.id,
                chapter.name,
                school.name,
                school.id
            ))
            .from(school)
            .join(chapterSchool).on(chapterSchool.school.eq(school))
            .join(chapterSchool.chapter, chapter)
            .join(chapter.gisu, gisu)
            .where(gisu.id.eq(gisuId))
            .fetch();
    }

    public List<SchoolGisuChapterInfo> getSchoolDetailsByGisuIds(Set<Long> gisuIds) {
        if (gisuIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
            .select(Projections.constructor(SchoolGisuChapterInfo.class,
                gisu.id,
                chapter.id,
                chapter.name,
                school.name,
                school.shortName,
                school.id,
                school.remark,
                school.logoImageId,
                gisu.isActive,
                school.createdAt,
                school.updatedAt
            ))
            .from(school)
            .join(chapterSchool).on(chapterSchool.school.eq(school))
            .join(chapterSchool.chapter, chapter)
            .join(chapter.gisu, gisu)
            .where(gisu.id.in(gisuIds))
            .fetch();
    }

    public List<SchoolChapterInfo> getSchoolDetailsByIds(Set<Long> schoolIds) {
        if (schoolIds.isEmpty()) {
            return List.of();
        }

        JPQLQuery<Long> activeChapterIds = activeChapterIdSubQuery();

        return queryFactory
            .select(Projections.constructor(SchoolChapterInfo.class,
                chapter.id,
                chapter.name,
                school.name,
                school.shortName,
                school.id,
                school.remark,
                school.logoImageId,
                chapter.id.isNotNull(),
                school.createdAt,
                school.updatedAt
            ))
            .from(school)
            .leftJoin(chapterSchool).on(
                chapterSchool.school.eq(school)
                    .and(chapterSchool.chapter.id.in(activeChapterIds))
            )
            .leftJoin(chapterSchool.chapter, chapter)
            .where(school.id.in(schoolIds))
            .fetch();
    }

    public List<School> findSchoolsByGisuId(Long gisuId) {
        return queryFactory
            .selectDistinct(school)
            .from(school)
            .join(school.chapterSchools, chapterSchool).fetchJoin()
            .join(chapterSchool.chapter, chapter).fetchJoin()
            .join(chapter.gisu, gisu).fetchJoin()
            .where(gisu.id.eq(gisuId))
            .fetch();
    }

    public List<SchoolNameInfo> findAllNames() {
        return queryFactory
            .select(Projections.constructor(SchoolNameInfo.class,
                school.id,
                school.name
            ))
            .from(school)
            .orderBy(school.name.asc())
            .fetch();
    }

    public List<SchoolDetailInfo.SchoolLinkItem> findLinksBySchoolId(Long schoolId) {
        return queryFactory
            .select(Projections.constructor(SchoolDetailInfo.SchoolLinkItem.class,
                schoolLink.title,
                schoolLink.type,
                schoolLink.url
            ))
            .from(schoolLink)
            .where(schoolLink.school.id.eq(schoolId))
            .fetch();
    }

    public Map<Long, List<SchoolDetailInfo.SchoolLinkItem>> findLinksBySchoolIds(List<Long> schoolIds) {
        if (schoolIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> tuples = queryFactory
            .select(schoolLink.school.id, schoolLink.title, schoolLink.type, schoolLink.url)
            .from(schoolLink)
            .where(schoolLink.school.id.in(schoolIds))
            .fetch();

        return tuples.stream()
            .collect(Collectors.groupingBy(
                t -> t.get(schoolLink.school.id),
                Collectors.mapping(
                    t -> new SchoolDetailInfo.SchoolLinkItem(
                        t.get(schoolLink.title),
                        t.get(schoolLink.type),
                        t.get(schoolLink.url)
                    ),
                    Collectors.toList()
                )
            ));
    }
}
