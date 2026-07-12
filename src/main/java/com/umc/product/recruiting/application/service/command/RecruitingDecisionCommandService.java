package com.umc.product.recruiting.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingDocumentCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DecideRecruitingFinalCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingDecisionCommandService implements
    DecideRecruitingDocumentUseCase,
    DecideRecruitingFinalUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final SaveRecruitingApplicationPort saveApplicationPort;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final RecruitingConcurrencyLockService concurrencyLockService;

    @Override
    public void decideDocument(DecideRecruitingDocumentCommand command) {
        RecruitingApplication application = concurrencyLockService.lockApplication(command.applicationId());
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
        RecruitingApplication application = concurrencyLockService.lockApplicantThenApplication(
            command.applicationId(),
            List.of()
        );
        validateFinalDecisionPermission(command.decidedByMemberId(), application);
        if (command.decision() == RecruitingDecisionStatus.PASS) {
            validateNoFinalPassDuplicate(application);
            application.passFinal(command.decidedByMemberId(), command.reason(), command.acceptedTrack());
        } else if (command.decision() == RecruitingDecisionStatus.FAIL) {
            application.failFinal(command.decidedByMemberId(), command.reason());
        } else {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);
        }
        saveApplicationPort.save(application);
    }

    private void validateNoFinalPassDuplicate(RecruitingApplication application) {
        Long gisuId = application.getRound().getSeason().getGisuId();
        if (loadApplicationPort.existsFinalPassedByGisuIdAndApplicant(
            gisuId,
            application.getApplicantMemberId(),
            application.getApplicantEmail(),
            application.getId()
        )) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FINAL_PASS_ALREADY_EXISTS);
        }
    }

    private void validateFinalDecisionPermission(Long memberId, RecruitingApplication application) {
        Long gisuId = application.getRound().getSeason().getGisuId();
        Long schoolId = application.getRound().getSeason().getSchoolId();
        if (getChallengerRoleUseCase.isCentralCoreInGisu(memberId, gisuId)) {
            return;
        }
        if (getChallengerRoleUseCase.isSchoolCoreInGisu(memberId, gisuId, schoolId)) {
            return;
        }
        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FINAL_DECISION_FORBIDDEN);
    }
}
