package com.umc.product.recruiting.adapter.out.persistence;

import static com.umc.product.recruiting.domain.QRecruitingApplication.recruitingApplication;
import static com.umc.product.recruiting.domain.QRecruitingApplicationForm.recruitingApplicationForm;
import static com.umc.product.recruiting.domain.QRecruitingRound.recruitingRound;
import static com.umc.product.recruiting.domain.QRecruitingSeason.recruitingSeason;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RecruitingApplicationQueryRepository {

    private static final List<RecruitingApplicationStatus> BLOCKING_STATUSES = List.of(
        RecruitingApplicationStatus.DRAFT,
        RecruitingApplicationStatus.SUBMITTED,
        RecruitingApplicationStatus.DOCUMENT_PASSED,
        RecruitingApplicationStatus.INTERVIEW_ASSIGNED,
        RecruitingApplicationStatus.INTERVIEW_SKIPPED,
        RecruitingApplicationStatus.FINAL_PASSED
    );

    private final JPAQueryFactory queryFactory;

    public Optional<RecruitingApplication> findByIdWithDetails(Long id) {
        RecruitingApplication result = queryFactory
            .selectFrom(recruitingApplication)
            .innerJoin(recruitingApplication.applicationForm, recruitingApplicationForm).fetchJoin()
            .innerJoin(recruitingApplicationForm.round, recruitingRound).fetchJoin()
            .innerJoin(recruitingRound.season, recruitingSeason).fetchJoin()
            .where(recruitingApplication.id.eq(id))
            .fetchOne();
        return Optional.ofNullable(result);
    }

    public Optional<RecruitingApplication> findActiveByRoundIdAndApplicantIdentityKey(
        Long roundId,
        String applicantIdentityKey
    ) {
        RecruitingApplication result = queryFactory
            .selectFrom(recruitingApplication)
            .where(
                recruitingApplication.round.id.eq(roundId),
                recruitingApplication.applicantIdentityKey.eq(applicantIdentityKey),
                recruitingApplication.status.in(BLOCKING_STATUSES)
            )
            .fetchFirst();
        return Optional.ofNullable(result);
    }

    public boolean existsBlockingApplicationByGisuIdAndApplicantIdentityKey(
        Long gisuId,
        String applicantIdentityKey
    ) {
        return existsBlockingApplicationByGisuIdAndApplicantIdentityKeyAndIdNot(gisuId, applicantIdentityKey, null);
    }

    public boolean existsBlockingApplicationByGisuIdAndApplicantIdentityKeyAndIdNot(
        Long gisuId,
        String applicantIdentityKey,
        Long excludedApplicationId
    ) {
        return queryFactory
            .selectOne()
            .from(recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(
                recruitingSeason.gisuId.eq(gisuId),
                recruitingApplication.applicantIdentityKey.eq(applicantIdentityKey),
                recruitingApplication.status.in(BLOCKING_STATUSES),
                applicationIdNotEq(excludedApplicationId)
            )
            .fetchFirst() != null;
    }

    public boolean existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKey(
        Long gisuId,
        Long schoolId,
        String applicantIdentityKey
    ) {
        return existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKeyAndIdNot(
            gisuId,
            schoolId,
            applicantIdentityKey,
            null
        );
    }

    public boolean existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKeyAndIdNot(
        Long gisuId,
        Long schoolId,
        String applicantIdentityKey,
        Long excludedApplicationId
    ) {
        return queryFactory
            .selectOne()
            .from(recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(
                recruitingSeason.gisuId.eq(gisuId),
                recruitingSeason.schoolId.ne(schoolId),
                recruitingApplication.applicantIdentityKey.eq(applicantIdentityKey),
                applicationIdNotEq(excludedApplicationId)
            )
            .fetchFirst() != null;
    }

    public boolean existsFinalPassedByGisuIdAndApplicantIdentityKey(Long gisuId, String applicantIdentityKey) {
        return existsFinalPassedByGisuIdAndApplicantIdentityKeyAndIdNot(gisuId, applicantIdentityKey, null);
    }

    public boolean existsFinalPassedByGisuIdAndApplicantIdentityKeyAndIdNot(
        Long gisuId,
        String applicantIdentityKey,
        Long excludedApplicationId
    ) {
        return queryFactory
            .selectOne()
            .from(recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(
                recruitingSeason.gisuId.eq(gisuId),
                recruitingApplication.applicantIdentityKey.eq(applicantIdentityKey),
                recruitingApplication.status.eq(RecruitingApplicationStatus.FINAL_PASSED),
                applicationIdNotEq(excludedApplicationId)
            )
            .fetchFirst() != null;
    }

    public List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Long schoolId,
        Collection<RecruitingApplicationStatus> statuses
    ) {
        return queryFactory
            .select(Projections.constructor(
                RecruitingApplicationSummaryRow.class,
                recruitingSeason.id,
                recruitingSeason.gisuId,
                recruitingSeason.schoolId,
                recruitingRound.id,
                recruitingRound.type,
                recruitingRound.roundNo,
                recruitingApplicationForm.id,
                recruitingApplicationForm.formId,
                recruitingApplicationForm.track,
                recruitingApplication.id,
                recruitingApplication.applicationNo,
                recruitingApplication.maskedEmail,
                recruitingApplication.status,
                recruitingApplication.registrationStatus,
                recruitingApplication.submittedAt
            ))
            .from(recruitingApplication)
            .innerJoin(recruitingApplication.applicationForm, recruitingApplicationForm)
            .innerJoin(recruitingApplicationForm.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(
                recruitingSeason.gisuId.eq(gisuId),
                schoolIdEq(schoolId),
                statusIn(statuses)
            )
            .orderBy(recruitingSeason.schoolId.asc(), recruitingRound.roundNo.asc(), recruitingApplication.id.asc())
            .fetch();
    }

    private BooleanExpression schoolIdEq(Long schoolId) {
        return schoolId == null ? null : recruitingSeason.schoolId.eq(schoolId);
    }

    private BooleanExpression statusIn(Collection<RecruitingApplicationStatus> statuses) {
        return statuses == null || statuses.isEmpty() ? null : recruitingApplication.status.in(statuses);
    }

    private BooleanExpression applicationIdNotEq(Long applicationId) {
        return applicationId == null ? null : recruitingApplication.id.ne(applicationId);
    }
}
