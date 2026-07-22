package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicantLockTarget;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingApplicationPersistenceAdapter
    implements LoadRecruitingApplicationPort, SaveRecruitingApplicationPort {

    private final RecruitingApplicationJpaRepository recruitingApplicationJpaRepository;
    private final RecruitingApplicationQueryRepository recruitingApplicationQueryRepository;

    @Override
    public Optional<RecruitingApplication> findById(Long id) {
        return recruitingApplicationJpaRepository.findById(id);
    }

    @Override
    public Optional<RecruitingApplication> findByApplicantEmailAndApplicationKey(
        String applicantEmail,
        String applicationKey
    ) {
        return recruitingApplicationJpaRepository.findByApplicantProfile_ApplicantEmailAndApplicationKey(
            applicantEmail,
            applicationKey
        );
    }

    @Override
    public RecruitingApplication getById(Long id) {
        return findById(id)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
    }

    @Override
    public Optional<RecruitingApplication> findByIdWithDetails(Long id) {
        return recruitingApplicationQueryRepository.findByIdWithDetails(id);
    }

    @Override
    public RecruitingApplication getByIdWithDetails(Long id) {
        return findByIdWithDetails(id)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
    }

    @Override
    public List<RecruitingApplication> listByIdsWithDetails(Set<Long> ids) {
        return recruitingApplicationQueryRepository.findAllByIdsWithDetails(ids);
    }

    @Override
    public RecruitingApplication getByIdWithDetailsForUpdate(Long id) {
        return RecruitingLockExceptionTranslator.translate(() -> {
            recruitingApplicationQueryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecruitingDomainException(
                    RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND
                ));
            return getByIdWithDetails(id);
        });
    }

    @Override
    public RecruitingApplicantLockTarget getApplicantLockTarget(Long id) {
        return recruitingApplicationQueryRepository.findApplicantLockTarget(id)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
    }

    @Override
    public Long getRoundIdByApplicationId(Long id) {
        return recruitingApplicationQueryRepository.findRoundIdByApplicationId(id)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
    }

    @Override
    public boolean existsByApplicantEmailAndApplicationKey(String applicantEmail, String applicationKey) {
        return recruitingApplicationJpaRepository.existsByApplicantProfile_ApplicantEmailAndApplicationKey(
            applicantEmail,
            applicationKey
        );
    }

    @Override
    public boolean existsByRoundIdAndApplicantMemberId(Long roundId, Long applicantMemberId) {
        return recruitingApplicationJpaRepository.existsByRound_IdAndApplicantMemberId(roundId, applicantMemberId);
    }

    @Override
    public boolean existsByRoundIdAndApplicantMemberIdAndIdNot(
        Long roundId,
        Long applicantMemberId,
        Long excludedId
    ) {
        return recruitingApplicationJpaRepository.existsByRound_IdAndApplicantMemberIdAndIdNot(
            roundId,
            applicantMemberId,
            excludedId
        );
    }

    @Override
    public boolean existsByRoundIdAndApplicantEmail(Long roundId, String applicantEmail) {
        return recruitingApplicationJpaRepository.existsByRound_IdAndApplicantProfile_ApplicantEmail(
            roundId,
            applicantEmail
        );
    }

    @Override
    public boolean existsByRoundIdAndApplicantEmailAndIdNot(
        Long roundId,
        String applicantEmail,
        Long excludedId
    ) {
        return recruitingApplicationJpaRepository.existsByRound_IdAndApplicantProfile_ApplicantEmailAndIdNot(
            roundId,
            applicantEmail,
            excludedId
        );
    }

    @Override
    public boolean existsByRoundId(Long roundId) {
        return recruitingApplicationJpaRepository.existsByRound_Id(roundId);
    }

    @Override
    public boolean existsBlockingApplicationByGisuIdAndApplicant(
        Long gisuId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedId
    ) {
        return recruitingApplicationQueryRepository.existsBlockingApplicationByGisuIdAndApplicant(
            gisuId,
            applicantMemberId,
            applicantEmail,
            excludedId
        );
    }

    @Override
    public boolean existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicant(
        Long gisuId,
        Long schoolId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedId
    ) {
        return recruitingApplicationQueryRepository.existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicant(
            gisuId,
            schoolId,
            applicantMemberId,
            applicantEmail,
            excludedId
        );
    }

    @Override
    public boolean existsFinalPassedByGisuIdAndApplicant(
        Long gisuId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedId
    ) {
        return recruitingApplicationQueryRepository.existsFinalPassedByGisuIdAndApplicant(
            gisuId,
            applicantMemberId,
            applicantEmail,
            excludedId
        );
    }

    @Override
    public long countReservedOrRegisteredBySeasonIdAndTrack(Long seasonId, ChallengerTrack track) {
        return recruitingApplicationJpaRepository.countByRound_Season_IdAndAcceptedTrackAndRegistrationStatusIn(
            seasonId,
            track,
            List.of(
                RecruitingApplicationRegistrationStatus.READY,
                RecruitingApplicationRegistrationStatus.REGISTERED
            )
        );
    }

    @Override
    public List<RecruitingApplication> listByRoundId(Long roundId) {
        return recruitingApplicationJpaRepository.findAllByRound_IdOrderBySubmittedAtAscIdAsc(roundId);
    }

    @Override
    public Page<RecruitingApplication> searchByRoundId(
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses,
        Collection<ChallengerTrack> tracks,
        Pageable pageable
    ) {
        return recruitingApplicationQueryRepository.searchByRoundId(roundId, statuses, tracks, pageable);
    }

    @Override
    public List<RecruitingApplication> listByRoundIdAndStatusIn(
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses
    ) {
        return recruitingApplicationJpaRepository.findAllByRound_IdAndStatusInOrderBySubmittedAtAscIdAsc(
            roundId,
            statuses
        );
    }

    @Override
    public List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Long schoolId,
        Collection<RecruitingApplicationStatus> statuses
    ) {
        return recruitingApplicationQueryRepository.searchSummaryRows(gisuId, schoolId, statuses);
    }

    @Override
    public List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Long schoolId,
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses
    ) {
        return recruitingApplicationQueryRepository.searchSummaryRows(gisuId, schoolId, roundId, statuses);
    }

    @Override
    public List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Collection<Long> schoolIds,
        Collection<Long> roundIds,
        Collection<RecruitingApplicationStatus> statuses
    ) {
        return recruitingApplicationQueryRepository.searchSummaryRows(gisuId, schoolIds, roundIds, statuses);
    }

    @Override
    public RecruitingApplication save(RecruitingApplication application) {
        return recruitingApplicationJpaRepository.save(application);
    }

    @Override
    public List<RecruitingApplication> saveAll(Collection<RecruitingApplication> applications) {
        return recruitingApplicationJpaRepository.saveAll(applications);
    }
}
