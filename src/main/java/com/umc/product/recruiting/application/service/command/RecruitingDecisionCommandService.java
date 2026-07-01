package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.command.ManageChallengerUseCase;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerCommand;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingDecisionCommandService implements
    DecideRecruitingDocumentUseCase,
    DecideRecruitingFinalUseCase,
    ConfirmRecruitingRegistrationUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final SaveRecruitingApplicationPort saveApplicationPort;
    private final ManageChallengerUseCase manageChallengerUseCase;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Override
    public void decideDocument(DecideRecruitingDocumentCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        if (command.decision() == RecruitingDecisionStatus.PASS) {
            application.passDocument(command.decidedByMemberId(), command.reason());
        } else if (command.decision() == RecruitingDecisionStatus.FAIL) {
            application.failDocument(command.decidedByMemberId(), command.reason());
        } else {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        saveApplicationPort.save(application);
    }

    @Override
    public void decideFinal(DecideRecruitingFinalCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        if (command.decision() == RecruitingDecisionStatus.PASS) {
            validateNoFinalPassDuplicate(application);
            application.passFinal(command.decidedByMemberId(), command.reason());
            application.markRegistrationReady(command.decidedByMemberId());
        } else if (command.decision() == RecruitingDecisionStatus.FAIL) {
            application.failFinal(command.decidedByMemberId(), command.reason());
        } else {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        saveApplicationPort.save(application);
    }

    @Override
    public void confirmRegistration(ConfirmRecruitingRegistrationCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        RecruitingSeason season = application.getRound().getSeason();
        validateCentralCore(command.executorMemberId(), season.getGisuId());
        validateMemberPresent(application);

        manageChallengerUseCase.createChallenger(CreateChallengerCommand.builder()
            .memberId(application.getApplicantMemberId())
            .track(application.getApplicationForm().getTrack())
            .gisuId(season.getGisuId())
            .build());
        application.register(command.executorMemberId());
        saveApplicationPort.save(application);
    }

    private void validateNoFinalPassDuplicate(RecruitingApplication application) {
        Long gisuId = application.getRound().getSeason().getGisuId();
        if (loadApplicationPort.existsFinalPassedByGisuIdAndApplicantIdentityKeyAndIdNot(
            gisuId,
            application.getApplicantIdentityKey(),
            application.getId()
        )) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FINAL_PASS_ALREADY_EXISTS);
        }
    }

    private void validateCentralCore(Long executorMemberId, Long gisuId) {
        if (getChallengerRoleUseCase.isCentralCoreInGisu(executorMemberId, gisuId)) {
            return;
        }
        if (getChallengerRoleUseCase.isSuperAdmin(executorMemberId)) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_REGISTRATION_FORBIDDEN);
    }

    private void validateMemberPresent(RecruitingApplication application) {
        if (application.getApplicantMemberId() == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_MEMBER_REQUIRED);
        }
    }
}
