package com.umc.product.recruiting.adapter.out.persistence;

import static com.umc.product.recruiting.domain.QRecruitingApplication.recruitingApplication;
import static com.umc.product.recruiting.domain.QRecruitingInterviewSchedule.recruitingInterviewSchedule;
import static com.umc.product.recruiting.domain.QRecruitingRound.recruitingRound;

import java.util.List;

import org.springframework.stereotype.Component;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewScheduleBoardPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleBoardRow;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingInterviewScheduleBoardQueryAdapter implements LoadRecruitingInterviewScheduleBoardPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RecruitingInterviewScheduleBoardRow> listByRoundId(Long roundId) {
        return queryFactory
            .select(Projections.constructor(
                RecruitingInterviewScheduleBoardRow.class,
                recruitingInterviewSchedule.id,
                recruitingApplication.id,
                recruitingApplication.applicantProfile.applicantName,
                recruitingInterviewSchedule.availabilityFormResponseId,
                recruitingInterviewSchedule.status,
                recruitingInterviewSchedule.interviewSessionId,
                recruitingInterviewSchedule.startsAt
            ))
            .from(recruitingInterviewSchedule)
            .innerJoin(recruitingInterviewSchedule.application, recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .where(
                recruitingRound.id.eq(roundId),
                recruitingInterviewSchedule.status.in(
                    RecruitingInterviewScheduleStatus.AVAILABILITY_SUBMITTED,
                    RecruitingInterviewScheduleStatus.CONFIRMED
                )
            )
            .orderBy(recruitingApplication.id.asc())
            .fetch();
    }
}
