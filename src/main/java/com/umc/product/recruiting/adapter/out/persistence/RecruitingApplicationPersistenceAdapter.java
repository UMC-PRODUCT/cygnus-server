package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplication;
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
    public RecruitingApplication getById(Long id) {
        return findById(id)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND));
    }

    @Override
    public Optional<RecruitingApplication> findByApplicationNo(String applicationNo) {
        return recruitingApplicationJpaRepository.findByApplicationNo(applicationNo);
    }

    @Override
    public RecruitingApplication getByApplicationNo(String applicationNo) {
        return findByApplicationNo(applicationNo)
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
    public Optional<RecruitingApplication> findActiveByRoundIdAndApplicantIdentityKey(
        Long roundId,
        String applicantIdentityKey
    ) {
        return recruitingApplicationQueryRepository.findActiveByRoundIdAndApplicantIdentityKey(
            roundId,
            applicantIdentityKey
        );
    }

    @Override
    public boolean existsByRoundIdAndApplicantIdentityKey(Long roundId, String applicantIdentityKey) {
        return recruitingApplicationJpaRepository.existsByRound_IdAndApplicantIdentityKey(
            roundId,
            applicantIdentityKey
        );
    }

    @Override
    public boolean existsByRoundIdAndApplicantIdentityKeyAndIdNot(
        Long roundId,
        String applicantIdentityKey,
        Long excludedApplicationId
    ) {
        return recruitingApplicationJpaRepository.existsByRound_IdAndApplicantIdentityKeyAndIdNot(
            roundId,
            applicantIdentityKey,
            excludedApplicationId
        );
    }

    @Override
    public boolean existsBlockingApplicationByGisuIdAndApplicantIdentityKey(
        Long gisuId,
        String applicantIdentityKey
    ) {
        return recruitingApplicationQueryRepository.existsBlockingApplicationByGisuIdAndApplicantIdentityKey(
            gisuId,
            applicantIdentityKey
        );
    }

    @Override
    public boolean existsBlockingApplicationByGisuIdAndApplicantIdentityKeyAndIdNot(
        Long gisuId,
        String applicantIdentityKey,
        Long excludedApplicationId
    ) {
        return recruitingApplicationQueryRepository.existsBlockingApplicationByGisuIdAndApplicantIdentityKeyAndIdNot(
            gisuId,
            applicantIdentityKey,
            excludedApplicationId
        );
    }

    @Override
    public boolean existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKey(
        Long gisuId,
        Long schoolId,
        String applicantIdentityKey
    ) {
        return recruitingApplicationQueryRepository
            .existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKey(
                gisuId,
                schoolId,
                applicantIdentityKey
            );
    }

    @Override
    public boolean existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKeyAndIdNot(
        Long gisuId,
        Long schoolId,
        String applicantIdentityKey,
        Long excludedApplicationId
    ) {
        return recruitingApplicationQueryRepository
            .existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKeyAndIdNot(
                gisuId,
                schoolId,
                applicantIdentityKey,
                excludedApplicationId
            );
    }

    @Override
    public boolean existsFinalPassedByGisuIdAndApplicantIdentityKey(Long gisuId, String applicantIdentityKey) {
        return recruitingApplicationQueryRepository.existsFinalPassedByGisuIdAndApplicantIdentityKey(
            gisuId,
            applicantIdentityKey
        );
    }

    @Override
    public boolean existsFinalPassedByGisuIdAndApplicantIdentityKeyAndIdNot(
        Long gisuId,
        String applicantIdentityKey,
        Long excludedApplicationId
    ) {
        return recruitingApplicationQueryRepository.existsFinalPassedByGisuIdAndApplicantIdentityKeyAndIdNot(
            gisuId,
            applicantIdentityKey,
            excludedApplicationId
        );
    }

    @Override
    public List<RecruitingApplication> listByRoundId(Long roundId) {
        return recruitingApplicationJpaRepository.findAllByRound_IdOrderBySubmittedAtAscIdAsc(roundId);
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
    public RecruitingApplication save(RecruitingApplication application) {
        return recruitingApplicationJpaRepository.save(application);
    }

    @Override
    public List<RecruitingApplication> saveAll(Collection<RecruitingApplication> applications) {
        return recruitingApplicationJpaRepository.saveAll(applications);
    }
}
