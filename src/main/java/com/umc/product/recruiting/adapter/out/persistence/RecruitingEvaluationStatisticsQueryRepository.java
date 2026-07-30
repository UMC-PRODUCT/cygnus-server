package com.umc.product.recruiting.adapter.out.persistence;

import static com.umc.product.recruiting.domain.QRecruitingApplication.recruitingApplication;
import static com.umc.product.recruiting.domain.QRecruitingRound.recruitingRound;
import static com.umc.product.recruiting.domain.QRecruitingSeason.recruitingSeason;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.recruiting.application.port.out.dto.RecruitingEvaluationStatisticsRow;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RecruitingEvaluationStatisticsQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<RecruitingEvaluationStatisticsRow> listByGisuId(Long gisuId) {
        return queryFactory
            .select(Projections.constructor(
                RecruitingEvaluationStatisticsRow.class,
                recruitingSeason.schoolId,
                recruitingApplication.applicantProfile.firstChoice,
                recruitingApplication.status,
                recruitingApplication.count()
            ))
            .from(recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(
                recruitingSeason.gisuId.eq(gisuId),
                recruitingApplication.status.notIn(
                    RecruitingApplicationStatus.DRAFT,
                    RecruitingApplicationStatus.CANCELLED
                )
            )
            .groupBy(
                recruitingSeason.schoolId,
                recruitingApplication.applicantProfile.firstChoice,
                recruitingApplication.status
            )
            .fetch();
    }
}
