package com.umc.product.recruiting.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public interface LoadRecruitingApplicationPort {

    Optional<RecruitingApplication> findById(Long id);

    RecruitingApplication getById(Long id);

    Optional<RecruitingApplication> findByApplicationNo(String applicationNo);

    RecruitingApplication getByApplicationNo(String applicationNo);

    Optional<RecruitingApplication> findByIdWithDetails(Long id);

    RecruitingApplication getByIdWithDetails(Long id);

    Optional<RecruitingApplication> findActiveByRoundIdAndApplicantIdentityKey(
        Long roundId,
        String applicantIdentityKey
    );

    boolean existsByRoundIdAndApplicantIdentityKey(Long roundId, String applicantIdentityKey);

    boolean existsByRoundIdAndApplicantIdentityKeyAndIdNot(
        Long roundId,
        String applicantIdentityKey,
        Long excludedApplicationId
    );

    boolean existsBlockingApplicationByGisuIdAndApplicantIdentityKey(Long gisuId, String applicantIdentityKey);

    boolean existsBlockingApplicationByGisuIdAndApplicantIdentityKeyAndIdNot(
        Long gisuId,
        String applicantIdentityKey,
        Long excludedApplicationId
    );

    boolean existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKey(
        Long gisuId,
        Long schoolId,
        String applicantIdentityKey
    );

    boolean existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicantIdentityKeyAndIdNot(
        Long gisuId,
        Long schoolId,
        String applicantIdentityKey,
        Long excludedApplicationId
    );

    boolean existsFinalPassedByGisuIdAndApplicantIdentityKey(Long gisuId, String applicantIdentityKey);

    boolean existsFinalPassedByGisuIdAndApplicantIdentityKeyAndIdNot(
        Long gisuId,
        String applicantIdentityKey,
        Long excludedApplicationId
    );

    List<RecruitingApplication> listByRoundId(Long roundId);

    List<RecruitingApplication> listByRoundIdAndStatusIn(
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses
    );

    List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Long schoolId,
        Collection<RecruitingApplicationStatus> statuses
    );
}
