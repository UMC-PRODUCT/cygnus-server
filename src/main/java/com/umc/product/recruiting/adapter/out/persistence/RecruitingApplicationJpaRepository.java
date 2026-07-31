package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public interface RecruitingApplicationJpaRepository extends JpaRepository<RecruitingApplication, Long> {

    boolean existsByApplicantProfile_ApplicantEmailAndApplicationKey(String applicantEmail, String applicationKey);

    Optional<RecruitingApplication> findByApplicantProfile_ApplicantEmailAndApplicationKey(
        String applicantEmail,
        String applicationKey
    );

    boolean existsByRound_IdAndApplicantMemberId(Long roundId, Long applicantMemberId);

    boolean existsByRound_IdAndApplicantMemberIdAndIdNot(Long roundId, Long applicantMemberId, Long excludedId);

    boolean existsByRound_IdAndApplicantProfile_ApplicantEmail(Long roundId, String applicantEmail);

    boolean existsByRound_IdAndApplicantProfile_ApplicantEmailAndIdNot(
        Long roundId,
        String applicantEmail,
        Long excludedId
    );

    boolean existsByRound_Id(Long roundId);

    long countByRound_Season_IdAndAcceptedTrackAndRegistrationStatusIn(
        Long seasonId,
        ChallengerTrack acceptedTrack,
        Collection<RecruitingApplicationRegistrationStatus> registrationStatuses
    );

    List<RecruitingApplication> findAllByRound_IdOrderBySubmittedAtAscIdAsc(Long roundId);

    List<RecruitingApplication> findAllByRound_IdAndStatusInOrderBySubmittedAtAscIdAsc(
        Long roundId,
        Collection<RecruitingApplicationStatus> statuses
    );
}
