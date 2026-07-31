package com.umc.product.recruiting.application.service.command;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.dto.CloseFormCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.application.port.in.command.dto.UnpublishFormCommand;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.UnpublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ValidateRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UnpublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationFormPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingApplicationFormCommandService implements
    PublishRecruitingApplicationFormUseCase,
    CloseRecruitingApplicationFormUseCase,
    UnpublishRecruitingApplicationFormUseCase {

    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final SaveRecruitingApplicationFormPort saveApplicationFormPort;
    private final ManageFormUseCase manageFormUseCase;
    private final ValidateRecruitingApplicationFormUseCase validateApplicationFormUseCase;

    @Override
    public void publish(PublishRecruitingApplicationFormCommand command) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getByIdForUpdate(command.applicationFormId());
        validateApplicationFormInSeason(applicationForm, command.seasonId());
        var trackSections = validateApplicationFormUseCase.validateForPublish(applicationForm.getId());
        applicationForm.publish(trackSections);
        manageFormUseCase.publishForm(PublishFormCommand.builder()
            .formId(applicationForm.getFormId())
            .requesterMemberId(command.requesterMemberId())
            .build());
        saveApplicationFormPort.save(applicationForm);
    }

    @Override
    public void close(CloseRecruitingApplicationFormCommand command) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getByIdForUpdate(command.applicationFormId());
        validateApplicationFormInSeason(applicationForm, command.seasonId());
        applicationForm.close();
        manageFormUseCase.closeForm(CloseFormCommand.builder()
            .formId(applicationForm.getFormId())
            .build());
        saveApplicationFormPort.save(applicationForm);
    }

    @Override
    public void unpublish(UnpublishRecruitingApplicationFormCommand command) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getByIdForUpdate(command.applicationFormId());
        validateApplicationFormInSeason(applicationForm, command.seasonId());
        applicationForm.unpublish();
        manageFormUseCase.unpublishForm(UnpublishFormCommand.builder()
            .formId(applicationForm.getFormId())
            .requesterMemberId(command.requesterMemberId())
            .build());
        saveApplicationFormPort.save(applicationForm);
    }

    private void validateApplicationFormInSeason(RecruitingApplicationForm applicationForm, Long seasonId) {
        if (!Objects.equals(applicationForm.getRound().getSeason().getId(), seasonId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND);
        }
    }
}
