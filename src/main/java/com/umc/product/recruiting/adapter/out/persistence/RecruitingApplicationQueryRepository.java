package com.umc.product.recruiting.adapter.out.persistence;

import static com.umc.product.recruiting.domain.QRecruitingApplication.recruitingApplication;
import static com.umc.product.recruiting.domain.QRecruitingApplicationForm.recruitingApplicationForm;
import static com.umc.product.recruiting.domain.QRecruitingRound.recruitingRound;
import static com.umc.product.recruiting.domain.QRecruitingSeason.recruitingSeason;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicantLockTarget;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RecruitingApplicationQueryRepository {

    private static final int LOCK_TIMEOUT_MILLIS = 3_000;

    private static final List<RecruitingApplicationStatus> BLOCKING_STATUSES = List.of(
        RecruitingApplicationStatus.DRAFT,
        RecruitingApplicationStatus.SUBMITTED,
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

    public Optional<RecruitingApplication> findByIdForUpdate(Long id) {
        RecruitingApplication result = queryFactory
            .selectFrom(recruitingApplication)
            .where(recruitingApplication.id.eq(id))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint("jakarta.persistence.lock.timeout", LOCK_TIMEOUT_MILLIS)
            .fetchOne();
        return Optional.ofNullable(result);
    }

    public Optional<RecruitingApplicantLockTarget> findApplicantLockTarget(Long applicationId) {
        Tuple result = queryFactory
            .select(
                recruitingSeason.gisuId,
                recruitingApplication.applicantMemberId,
                recruitingApplication.applicantProfile.applicantEmail
            )
            .from(recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(recruitingApplication.id.eq(applicationId))
            .fetchOne();
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(new RecruitingApplicantLockTarget(
            result.get(recruitingSeason.gisuId),
            result.get(recruitingApplication.applicantMemberId),
            result.get(recruitingApplication.applicantProfile.applicantEmail)
        ));
    }

    public Optional<Long> findRoundIdByApplicationId(Long applicationId) {
        return Optional.ofNullable(queryFactory
            .select(recruitingApplication.round.id)
            .from(recruitingApplication)
            .where(recruitingApplication.id.eq(applicationId))
            .fetchOne());
    }

    public boolean existsBlockingApplicationByGisuIdAndApplicant(
        Long gisuId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedId
    ) {
        return queryFactory
            .selectOne()
            .from(recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(
                recruitingSeason.gisuId.eq(gisuId),
                applicantEq(applicantMemberId, applicantEmail),
                recruitingApplication.status.in(BLOCKING_STATUSES),
                applicationIdNotEq(excludedId)
            )
            .fetchFirst() != null;
    }

    public boolean existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicant(
        Long gisuId,
        Long schoolId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedId
    ) {
        return queryFactory
            .selectOne()
            .from(recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(
                recruitingSeason.gisuId.eq(gisuId),
                recruitingSeason.schoolId.ne(schoolId),
                applicantEq(applicantMemberId, applicantEmail),
                recruitingApplication.status.in(BLOCKING_STATUSES),
                applicationIdNotEq(excludedId)
            )
            .fetchFirst() != null;
    }

    public boolean existsFinalPassedByGisuIdAndApplicant(
        Long gisuId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedId
    ) {
        return queryFactory
            .selectOne()
            .from(recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(
                recruitingSeason.gisuId.eq(gisuId),
                applicantEq(applicantMemberId, applicantEmail),
                recruitingApplication.status.eq(RecruitingApplicationStatus.FINAL_PASSED),
                applicationIdNotEq(excludedId)
            )
            .fetchFirst() != null;
    }

    public List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Long schoolId,
        Collection<RecruitingApplicationStatus> statuses
    ) {
        return searchSummaryRows(gisuId, schoolId, null, statuses);
    }

    public List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Long schoolId,
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses
    ) {
        return queryFactory
            .select(Projections.constructor(
                RecruitingApplicationSummaryRow.class,
                recruitingSeason.id,
                recruitingSeason.gisuId,
                recruitingSeason.schoolId,
                recruitingRound.id,
                recruitingRound.title,
                recruitingRound.type,
                recruitingRound.roundNo,
                recruitingApplicationForm.id,
                recruitingApplicationForm.formId,
                recruitingApplication.id,
                recruitingApplication.applicantProfile.applicantName,
                recruitingApplication.applicantProfile.applicantEmail,
                recruitingApplication.applicantProfile.firstChoice,
                recruitingApplication.applicantProfile.secondChoice,
                recruitingApplication.acceptedTrack,
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
                roundIdEq(roundId),
                statusIn(statuses)
            )
            .orderBy(recruitingSeason.schoolId.asc(), recruitingRound.roundNo.asc(), recruitingApplication.id.asc())
            .fetch();
    }

    public List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Collection<Long> schoolIds,
        Collection<Long> roundIds,
        Collection<RecruitingApplicationStatus> statuses
    ) {
        return queryFactory
            .select(Projections.constructor(
                RecruitingApplicationSummaryRow.class,
                recruitingSeason.id,
                recruitingSeason.gisuId,
                recruitingSeason.schoolId,
                recruitingRound.id,
                recruitingRound.title,
                recruitingRound.type,
                recruitingRound.roundNo,
                recruitingApplicationForm.id,
                recruitingApplicationForm.formId,
                recruitingApplication.id,
                recruitingApplication.applicantProfile.applicantName,
                recruitingApplication.applicantProfile.applicantEmail,
                recruitingApplication.applicantProfile.firstChoice,
                recruitingApplication.applicantProfile.secondChoice,
                recruitingApplication.acceptedTrack,
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
                schoolIdIn(schoolIds),
                roundIdIn(roundIds),
                statusIn(statuses)
            )
            .orderBy(recruitingSeason.schoolId.asc(), recruitingRound.roundNo.asc(), recruitingApplication.id.asc())
            .fetch();
    }

    public Page<RecruitingApplication> searchByRoundId(
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses,
        Collection<ChallengerTrack> tracks,
        Pageable pageable
    ) {
        BooleanExpression predicate = recruitingApplication.round.id.eq(roundId)
            .and(recruitingApplication.status.ne(RecruitingApplicationStatus.DRAFT))
            .and(statusIn(statuses))
            .and(trackIn(tracks));
        List<RecruitingApplication> content = queryFactory
            .selectFrom(recruitingApplication)
            .innerJoin(recruitingApplication.applicationForm, recruitingApplicationForm).fetchJoin()
            .innerJoin(recruitingApplicationForm.round, recruitingRound).fetchJoin()
            .innerJoin(recruitingRound.season, recruitingSeason).fetchJoin()
            .where(predicate)
            .orderBy(recruitingApplication.submittedAt.desc(), recruitingApplication.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
        Long total = queryFactory
            .select(recruitingApplication.count())
            .from(recruitingApplication)
            .where(predicate)
            .fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    private BooleanExpression applicantEq(Long applicantMemberId, String applicantEmail) {
        BooleanExpression emailEq = recruitingApplication.applicantProfile.applicantEmail.eq(applicantEmail);
        return applicantMemberId == null
            ? emailEq
            : recruitingApplication.applicantMemberId.eq(applicantMemberId).or(emailEq);
    }

    private BooleanExpression schoolIdEq(Long schoolId) {
        return schoolId == null ? null : recruitingSeason.schoolId.eq(schoolId);
    }

    private BooleanExpression schoolIdIn(Collection<Long> schoolIds) {
        return schoolIds == null || schoolIds.isEmpty() ? null : recruitingSeason.schoolId.in(schoolIds);
    }

    private BooleanExpression statusIn(Collection<RecruitingApplicationStatus> statuses) {
        return statuses == null || statuses.isEmpty() ? null : recruitingApplication.status.in(statuses);
    }

    private BooleanExpression trackIn(Collection<ChallengerTrack> tracks) {
        return tracks == null || tracks.isEmpty() ? null
            : recruitingApplication.applicantProfile.firstChoice.in(tracks)
                .or(recruitingApplication.applicantProfile.secondChoice.in(tracks));
    }

    private BooleanExpression applicationIdNotEq(Long applicationId) {
        return applicationId == null ? null : recruitingApplication.id.ne(applicationId);
    }

    private BooleanExpression roundIdEq(Long roundId) {
        return roundId == null ? null : recruitingRound.id.eq(roundId);
    }

    private BooleanExpression roundIdIn(Collection<Long> roundIds) {
        return roundIds == null || roundIds.isEmpty() ? null : recruitingRound.id.in(roundIds);
    }
}
