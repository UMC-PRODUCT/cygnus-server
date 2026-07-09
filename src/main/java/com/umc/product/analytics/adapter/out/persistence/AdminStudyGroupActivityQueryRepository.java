package com.umc.product.analytics.adapter.out.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityInfo.NoScheduleGroupInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityInfo.PartActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo.PartGroupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo.StudyGroupInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.domain.QMember;
import com.umc.product.organization.domain.QChapter;
import com.umc.product.organization.domain.QChapterSchool;
import com.umc.product.organization.domain.QStudyGroup;
import com.umc.product.organization.domain.QStudyGroupMember;
import com.umc.product.organization.domain.QStudyGroupSchedule;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AdminStudyGroupActivityQueryRepository {

    private final JPAQueryFactory queryFactory;

    public AdminStudyGroupListInfo getStudyGroupList(AdminAnalyticsScope scope) {
        QStudyGroup sg = new QStudyGroup("groupsStudyGroup");
        QStudyGroupSchedule sgs = new QStudyGroupSchedule("groupsStudyGroupSchedule");
        QStudyGroupMember sgm = new QStudyGroupMember("groupsStudyGroupMember");
        QMember m = new QMember("groupsMember");
        NumberExpression<Long> scheduleCountExpr = sgs.id.countDistinct();

        BooleanBuilder condition = new BooleanBuilder();
        condition.and(sg.gisuId.eq(scope.gisuId()));
        condition.and(m.schoolId.eq(scope.schoolId()));
        if (scope.responsiblePart() != null) {
            condition.and(sg.part.eq(scope.responsiblePart()));
        }

        List<Tuple> rows = queryFactory
            .select(sg.id, sg.name, sg.part, scheduleCountExpr)
            .from(sg)
            .join(sgm).on(sgm.studyGroup.id.eq(sg.id))
            .join(m).on(m.id.eq(sgm.memberId))
            .leftJoin(sgs).on(sgs.studyGroupId.eq(sg.id))
            .where(condition)
            .groupBy(sg.id, sg.name, sg.part)
            .orderBy(sg.part.asc(), sg.name.asc())
            .fetch();

        // 파트별로 그룹화 (LinkedHashMap으로 DB 정렬 순서 보존)
        Map<ChallengerPart, List<StudyGroupInfo>> byPartMap = new LinkedHashMap<>();
        for (Tuple row : rows) {
            ChallengerPart part = row.get(sg.part);
            StudyGroupInfo groupInfo = StudyGroupInfo.of(
                row.get(sg.id),
                row.get(sg.name),
                defaultLong(row.get(scheduleCountExpr))
            );
            byPartMap.computeIfAbsent(part, ignored -> new ArrayList<>()).add(groupInfo);
        }

        List<PartGroupsInfo> byPart = byPartMap.entrySet().stream()
            .map(e -> PartGroupsInfo.of(e.getKey(), e.getValue()))
            .toList();

        return AdminStudyGroupListInfo.from(byPart);
    }

    public AdminStudyGroupActivityInfo getStudyGroupActivity(AdminAnalyticsScope scope) {
        QStudyGroup sg = new QStudyGroup("activityStudyGroup");
        QStudyGroupSchedule sgs = new QStudyGroupSchedule("activityStudyGroupSchedule");
        NumberExpression<Long> scheduleCountExpr = sgs.id.countDistinct();

        BooleanBuilder condition = new BooleanBuilder();
        condition.and(sg.gisuId.eq(scope.gisuId()));
        if (scope.responsiblePart() != null) {
            condition.and(sg.part.eq(scope.responsiblePart()));
        }

        JPAQuery<Tuple> query = queryFactory
            .select(sg.id, sg.name, sg.part, scheduleCountExpr)
            .from(sg)
            .leftJoin(sgs).on(sgs.studyGroupId.eq(sg.id));

        // chapterId/schoolId scope가 있을 때만 member 경로 추가 조인
        if (scope.chapterId() != null || scope.schoolId() != null) {
            QStudyGroupMember sgm = new QStudyGroupMember("activityStudyGroupMember");
            QMember m = new QMember("activityMember");
            QChapterSchool cs = new QChapterSchool("activityChapterSchool");
            QChapter chapter = new QChapter("activityChapter");

            query.leftJoin(sgm).on(sgm.studyGroup.id.eq(sg.id))
                .leftJoin(m).on(m.id.eq(sgm.memberId))
                .leftJoin(cs).on(cs.school.id.eq(m.schoolId))
                .leftJoin(chapter).on(chapter.id.eq(cs.chapter.id)
                    .and(chapter.gisu.id.eq(sg.gisuId)));

            if (scope.chapterId() != null) {
                condition.and(chapter.id.eq(scope.chapterId()));
            }
            if (scope.schoolId() != null) {
                condition.and(m.schoolId.eq(scope.schoolId()));
            }
        }

        List<Tuple> rows = query
            .where(condition)
            .groupBy(sg.id, sg.name, sg.part)
            .orderBy(sg.part.asc(), sg.name.asc())
            .fetch();

        // 파트별 집계 (LinkedHashMap으로 DB 정렬 순서 보존)
        Map<ChallengerPart, List<StudyGroupRawInfo>> rawByPart = new LinkedHashMap<>();

        for (Tuple row : rows) {
            ChallengerPart part = row.get(sg.part);
            long scheduleCount = defaultLong(row.get(scheduleCountExpr));
            rawByPart.computeIfAbsent(part, ignored -> new ArrayList<>())
                .add(new StudyGroupRawInfo(row.get(sg.id), row.get(sg.name), scheduleCount));
        }

        long totalGroupCount = 0;
        long totalScheduleCount = 0;
        List<PartActivityInfo> byPart = new ArrayList<>();

        for (Map.Entry<ChallengerPart, List<StudyGroupRawInfo>> entry : rawByPart.entrySet()) {
            List<StudyGroupRawInfo> groups = entry.getValue();
            long groupCount = groups.size();
            long partScheduleCount = groups.stream().mapToLong(StudyGroupRawInfo::scheduleCount).sum();
            double averageScheduleCount = groupCount > 0 ? (double) partScheduleCount / groupCount : 0.0;
            List<NoScheduleGroupInfo> noScheduleGroups = groups.stream()
                .filter(g -> g.scheduleCount() == 0)
                .map(g -> NoScheduleGroupInfo.of(g.studyGroupId(), g.name()))
                .toList();

            byPart.add(PartActivityInfo.of(entry.getKey(), groupCount, partScheduleCount, averageScheduleCount, noScheduleGroups));
            totalGroupCount += groupCount;
            totalScheduleCount += partScheduleCount;
        }

        return AdminStudyGroupActivityInfo.of(totalGroupCount, totalScheduleCount, byPart);
    }

    private long defaultLong(Long value) {
        return value != null ? value : 0L;
    }

    private record StudyGroupRawInfo(Long studyGroupId, String name, long scheduleCount) {}
}
