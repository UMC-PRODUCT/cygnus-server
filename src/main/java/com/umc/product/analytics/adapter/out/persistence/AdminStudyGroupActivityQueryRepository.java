package com.umc.product.analytics.adapter.out.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo.PartGroupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo.StudyGroupInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.domain.QMember;
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

    private long defaultLong(Long value) {
        return value != null ? value : 0L;
    }
}
