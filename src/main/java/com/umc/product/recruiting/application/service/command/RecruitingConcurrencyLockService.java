package com.umc.product.recruiting.application.service.command;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.LockRecruitingApplicantPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicantLockTarget;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingRound;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingConcurrencyLockService {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final LockRecruitingApplicantPort lockApplicantPort;

    public void lockNewApplicant(
        RecruitingRound round,
        Long applicantMemberId,
        String normalizedEmail
    ) {
        lockApplicantPort.lockByGisuAndApplicant(
            round.getSeason().getGisuId(),
            applicantMemberId,
            List.of(normalizedEmail)
        );
    }

    public RecruitingApplication lockApplicantThenApplication(
        Long applicationId,
        Collection<String> additionalNormalizedEmails
    ) {
        RecruitingApplicantLockTarget target = loadApplicationPort.getApplicantLockTarget(applicationId);
        Collection<String> emails = Stream.concat(
            Stream.of(target.applicantEmail()),
            additionalNormalizedEmails.stream()
        ).toList();
        lockApplicantPort.lockByGisuAndApplicant(
            target.gisuId(),
            target.applicantMemberId(),
            emails
        );
        return loadApplicationPort.getByIdWithDetailsForUpdate(applicationId);
    }

    public RecruitingApplication lockApplication(Long applicationId) {
        return loadApplicationPort.getByIdWithDetailsForUpdate(applicationId);
    }

    public List<RecruitingApplication> lockApplications(Collection<Long> applicationIds) {
        return applicationIds.stream()
            .distinct()
            .sorted()
            .map(loadApplicationPort::getByIdWithDetailsForUpdate)
            .toList();
    }

    public RecruitingApplication lockRoundThenApplication(Long applicationId) {
        Long roundId = loadApplicationPort.getRoundIdByApplicationId(applicationId);
        loadRoundPort.getByIdForUpdate(roundId);
        return loadApplicationPort.getByIdWithDetailsForUpdate(applicationId);
    }

    public RecruitingRound lockRound(Long roundId) {
        return loadRoundPort.getByIdForUpdate(roundId);
    }
}
