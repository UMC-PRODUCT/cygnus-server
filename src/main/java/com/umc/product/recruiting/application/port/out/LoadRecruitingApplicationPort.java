package com.umc.product.recruiting.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicantLockTarget;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public interface LoadRecruitingApplicationPort {

    Optional<RecruitingApplication> findById(Long id);

    Optional<RecruitingApplication> findByApplicantEmailAndApplicationKey(
        String applicantEmail,
        String applicationKey
    );

    RecruitingApplication getById(Long id);

    Optional<RecruitingApplication> findByIdWithDetails(Long id);

    RecruitingApplication getByIdWithDetails(Long id);

    List<RecruitingApplication> listByIdsWithDetails(Set<Long> ids);

    RecruitingApplication getByIdWithDetailsForUpdate(Long id);

    RecruitingApplicantLockTarget getApplicantLockTarget(Long id);

    Long getRoundIdByApplicationId(Long id);

    boolean existsByApplicantEmailAndApplicationKey(String applicantEmail, String applicationKey);

    boolean existsByRoundIdAndApplicantMemberId(Long roundId, Long applicantMemberId);

    boolean existsByRoundIdAndApplicantMemberIdAndIdNot(Long roundId, Long applicantMemberId, Long excludedId);

    boolean existsByRoundIdAndApplicantEmail(Long roundId, String applicantEmail);

    boolean existsByRoundIdAndApplicantEmailAndIdNot(Long roundId, String applicantEmail, Long excludedId);

    boolean existsByRoundId(Long roundId);

    boolean existsBlockingApplicationByGisuIdAndApplicant(
        Long gisuId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedId
    );

    boolean existsBlockingApplicationByGisuIdAndDifferentSchoolIdAndApplicant(
        Long gisuId,
        Long schoolId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedId
    );

    boolean existsFinalPassedByGisuIdAndApplicant(
        Long gisuId,
        Long applicantMemberId,
        String applicantEmail,
        Long excludedId
    );

    long countReservedOrRegisteredBySeasonIdAndTrack(Long seasonId, ChallengerTrack track);

    List<RecruitingApplication> listByRoundId(Long roundId);

    Page<RecruitingApplication> searchByRoundId(
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses,
        Collection<ChallengerTrack> tracks,
        Pageable pageable
    );

    List<RecruitingApplication> listByRoundIdAndStatusIn(
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses
    );

    List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Long schoolId,
        Collection<RecruitingApplicationStatus> statuses
    );

    List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Long schoolId,
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses
    );

    List<RecruitingApplicationSummaryRow> searchSummaryRows(
        Long gisuId,
        Collection<Long> schoolIds,
        Collection<Long> roundIds,
        Collection<RecruitingApplicationStatus> statuses
    );
}
