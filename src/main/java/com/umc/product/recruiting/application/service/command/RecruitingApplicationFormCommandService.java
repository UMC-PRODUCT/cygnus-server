package com.umc.product.recruiting.application.service.command;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ValidateRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationFormPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingApplicationFormCommandService implements
    LinkRecruitingApplicationFormUseCase,
    PublishRecruitingApplicationFormUseCase,
    CloseRecruitingApplicationFormUseCase {

    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final SaveRecruitingApplicationFormPort saveApplicationFormPort;
    private final ManageFormUseCase manageFormUseCase;
    private final ValidateRecruitingApplicationFormUseCase validateApplicationFormUseCase;

    @Override
    public Long link(LinkRecruitingApplicationFormCommand command) {
        RecruitingRound round = getRoundInSeason(command.roundId(), command.seasonId());
        if (loadApplicationFormPort.findByRoundId(command.roundId()).isPresent()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_ALREADY_EXISTS);
        }
        // TODO: Form 기간 설정 공개 UseCase가 제공되면 Round의 documentStartAt/documentEndAt을 Form에 동기화한다.
        RecruitingApplicationForm saved = saveApplicationFormPort.save(
            RecruitingApplicationForm.create(round, command.formId())
        );
        return saved.getId();
    }

    @Override
    public void publish(PublishRecruitingApplicationFormCommand command) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getById(command.applicationFormId());
        validateApplicationFormInSeason(applicationForm, command.seasonId());
        validateApplicationFormUseCase.validateForPublish(applicationForm.getId());
        manageFormUseCase.publishForm(PublishFormCommand.builder()
            .formId(applicationForm.getFormId())
            .requesterMemberId(command.requesterMemberId())
            .build());
        applicationForm.publish();
        saveApplicationFormPort.save(applicationForm);
    }

    @Override
    public void close(CloseRecruitingApplicationFormCommand command) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getById(command.applicationFormId());
        validateApplicationFormInSeason(applicationForm, command.seasonId());
        applicationForm.close();
        saveApplicationFormPort.save(applicationForm);
    }

    private RecruitingRound getRoundInSeason(Long roundId, Long seasonId) {
        RecruitingRound round = loadRoundPort.getById(roundId);
        if (!Objects.equals(round.getSeason().getId(), seasonId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND);
        }
        return round;
    }

    private void validateApplicationFormInSeason(RecruitingApplicationForm applicationForm, Long seasonId) {
        if (!Objects.equals(applicationForm.getRound().getSeason().getId(), seasonId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND);
        }
    }
}
