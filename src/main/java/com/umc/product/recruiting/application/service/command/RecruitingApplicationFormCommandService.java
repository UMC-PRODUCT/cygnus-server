package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
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
import com.umc.product.survey.application.port.in.command.ManageFormUseCase;
import com.umc.product.survey.application.port.in.command.dto.PublishFormCommand;

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

    @Override
    public Long link(LinkRecruitingApplicationFormCommand command) {
        if (loadApplicationFormPort.findByRoundIdAndFormId(command.roundId(), command.formId()).isPresent()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_ALREADY_EXISTS);
        }
        RecruitingRound round = loadRoundPort.getById(command.roundId());
        RecruitingApplicationForm saved = saveApplicationFormPort.save(
            RecruitingApplicationForm.create(round, command.formId(), command.track())
        );
        return saved.getId();
    }

    @Override
    public void publish(PublishRecruitingApplicationFormCommand command) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getById(command.applicationFormId());
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
        applicationForm.close();
        saveApplicationFormPort.save(applicationForm);
    }
}
