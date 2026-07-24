package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.challenger.application.port.in.command.AddChallengerTrackUseCase;
import com.umc.product.challenger.application.port.in.command.dto.AddChallengerTrackCommand;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAction;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAuthorizationService;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.PrepareRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonTrackQuotaPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.RecruitingSeasonTrackQuota;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingRegistrationCommandService implements
    PrepareRecruitingRegistrationUseCase,
    CancelRecruitingRegistrationUseCase,
    ConfirmRecruitingRegistrationUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final SaveRecruitingApplicationPort saveApplicationPort;
    private final LoadRecruitingSeasonTrackQuotaPort loadQuotaPort;
    private final AddChallengerTrackUseCase addChallengerTrackUseCase;
    private final RecruitingPolicyAuthorizationService policyAuthorizationService;

    @Override
    public void prepareRegistration(PrepareRecruitingRegistrationCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetailsForUpdate(command.applicationId());
        RecruitingSeason season = application.getRound().getSeason();
        validateCentralCore(command.executorMemberId(), season.getGisuId());
        ChallengerTrack acceptedTrack = getAcceptedTrack(application);
        RecruitingSeasonTrackQuota quota = loadQuotaPort.getBySeasonIdAndTrackForUpdate(
            season.getId(),
            acceptedTrack
        );
        long usedCount = loadApplicationPort.countReservedOrRegisteredBySeasonIdAndTrack(
            season.getId(),
            acceptedTrack
        );
        if (usedCount >= quota.getTargetCount()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_QUOTA_EXCEEDED);
        }
        application.markRegistrationReady(command.executorMemberId());
        saveApplicationPort.save(application);
    }

    @Override
    public void cancelRegistration(CancelRecruitingRegistrationCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetailsForUpdate(command.applicationId());
        RecruitingSeason season = application.getRound().getSeason();
        validateCentralCore(command.executorMemberId(), season.getGisuId());
        application.cancelRegistrationReady(command.executorMemberId());
        saveApplicationPort.save(application);
    }

    @Override
    public void confirmRegistration(ConfirmRecruitingRegistrationCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetailsForUpdate(command.applicationId());
        RecruitingSeason season = application.getRound().getSeason();
        validateCentralCore(command.executorMemberId(), season.getGisuId());
        validateMemberPresent(application);
        ChallengerTrack acceptedTrack = getAcceptedTrack(application);
        addChallengerTrackUseCase.addTrack(AddChallengerTrackCommand.of(
            application.getApplicantMemberId(),
            season.getGisuId(),
            acceptedTrack
        ));
        application.register(command.executorMemberId());
        saveApplicationPort.save(application);
    }

    private void validateCentralCore(Long executorMemberId, Long gisuId) {
        if (policyAuthorizationService.evaluateMember(
            RecruitingPolicyAction.REGISTRATION_MANAGE,
            executorMemberId,
            gisuId,
            null
        )) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_REGISTRATION_FORBIDDEN);
    }

    private void validateMemberPresent(RecruitingApplication application) {
        if (application.getApplicantMemberId() == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_MEMBER_REQUIRED);
        }
    }

    private ChallengerTrack getAcceptedTrack(RecruitingApplication application) {
        if (application.getAcceptedTrack() == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_ACCEPTED_TRACK);
        }
        return application.getAcceptedTrack();
    }
}
