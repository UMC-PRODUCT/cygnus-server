package com.umc.product.recruiting.application.service.command;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnswerCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.QuestionWithOptions;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.command.AuthorizeRecruitingManagementUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingInterviewSchedulesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RequestRecruitingInterviewScheduleCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewSessionPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewSchedulePort;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingInterviewScheduleCommandService implements ManageRecruitingInterviewScheduleUseCase {

    private final LoadRecruitingInterviewSchedulePort loadSchedulePort;
    private final SaveRecruitingInterviewSchedulePort saveSchedulePort;
    private final AuthorizeRecruitingManagementUseCase authorizeManagementUseCase;
    private final RecruitingInterviewAvailabilityRequestCoordinator availabilityRequestCoordinator;
    private final RecruitingConcurrencyLockService concurrencyLockService;
    private final GetFormUseCase getFormUseCase;
    private final ManageFormResponseUseCase manageFormResponseUseCase;
    private final LoadRecruitingInterviewSessionPort loadSessionPort;
    private final ConfirmRecruitingInterviewSchedulesUseCase confirmSchedulesUseCase;

    @Override
    public Long requestAvailability(RequestRecruitingInterviewScheduleCommand command) {
        RecruitingApplication application = concurrencyLockService.lockApplication(command.applicationId());
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            application.getRound().getSeason().getId()
        );
        validateInterviewAssigned(application);
        return availabilityRequestCoordinator.request(application, command.contactSnapshot()).getId();
    }

    @Override
    public void submitAvailability(SubmitRecruitingInterviewAvailabilityCommand command) {
        RecruitingApplication application = concurrencyLockService.lockApplication(command.applicationId());
        application.validateApplicant(command.requesterMemberId());
        validateInterviewAssigned(application);

        RecruitingInterviewSchedule schedule = loadSchedulePort.getByApplicationId(command.applicationId());
        validateAvailabilityRequested(schedule);

        Long formId = application.getRound().getAvailabilityFormId();
        Long questionId = application.getRound().getAvailabilityScheduleQuestionId();
        validateAvailabilityMapping(formId, questionId);
        validateAvailabilityForm(getFormUseCase.getFormWithStructure(formId), formId, questionId);
        validateAvailabilityTimes(
            command.times(),
            application.getRound().getInterviewStartAt(),
            application.getRound().getInterviewEndAt()
        );

        Long formResponseId = manageFormResponseUseCase.submitImmediately(
            SubmitFormResponseCommand.builder()
                .formId(formId)
                .respondentMemberId(command.requesterMemberId())
                .answers(List.of(AnswerCommand.builder()
                    .questionId(questionId)
                    .times(command.times())
                    .build()))
                .build()
        );
        schedule.submitAvailability(formResponseId);
        saveSchedulePort.saveSchedule(schedule);
    }

    @Override
    public void confirm(ConfirmRecruitingInterviewScheduleCommand command) {
        if (command.sessionId() == null) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SESSION_INVALID);
        }
        RecruitingInterviewSchedule schedule = loadSchedulePort.getByApplicationId(command.applicationId());
        Long roundId = schedule.getApplication().getRound().getId();
        authorizeManagementUseCase.authorizeSeasonManagement(
            command.requesterMemberId(),
            schedule.getApplication().getRound().getSeason().getId()
        );
        var session = loadSessionPort.getById(command.sessionId());
        validateSessionDerivedValues(command, session);
        confirmSchedulesUseCase.confirmAll(ConfirmRecruitingInterviewSchedulesCommand.of(
            roundId,
            command.requesterMemberId(),
            List.of(ConfirmRecruitingInterviewSchedulesCommand.Assignment.of(
                command.applicationId(),
                command.sessionId(),
                command.startsAt(),
                command.contactSnapshot()
            ))
        ));
        // TODO(#1147): HTML 메일 계약이 제공되면 확정 메일을 발송하고 delivery 상태를 기록한다.
    }

    private void validateSessionDerivedValues(
        ConfirmRecruitingInterviewScheduleCommand command,
        com.umc.product.recruiting.domain.RecruitingInterviewSession session
    ) {
        if (command.startsAt() == null
            || command.endsAt() == null
            || !command.startsAt().plus(Duration.ofMinutes(session.getSlotDurationMinutes())).equals(command.endsAt())
            || !session.getLocation().equals(command.location())) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT);
        }
    }

    private void validateInterviewAssigned(RecruitingApplication application) {
        if (application.getStatus() != RecruitingApplicationStatus.INTERVIEW_ASSIGNED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION);
        }
    }

    private void validateAvailabilityRequested(RecruitingInterviewSchedule schedule) {
        if (schedule.getStatus() != RecruitingInterviewScheduleStatus.AVAILABILITY_REQUESTED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION);
        }
    }

    private void validateAvailabilityMapping(Long formId, Long questionId) {
        if (formId == null || formId <= 0 || questionId == null || questionId <= 0) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE);
        }
    }

    private void validateAvailabilityForm(
        FormWithStructureInfo form,
        Long expectedFormId,
        Long expectedQuestionId
    ) {
        if (form == null
            || !expectedFormId.equals(form.formId())
            || form.status() != FormStatus.PUBLISHED
            || form.isAnonymous()
            || form.sections() == null
            || form.sections().stream().anyMatch(section -> section == null || section.questions() == null)
            || form.sections().stream()
                .flatMap(section -> section.questions().stream())
                .anyMatch(question -> question == null)) {
            throw invalidAvailabilityResponse();
        }

        List<QuestionWithOptions> questions = form.sections().stream()
            .flatMap(section -> section.questions().stream())
            .toList();
        List<QuestionWithOptions> availabilityQuestions = questions.stream()
            .filter(question -> expectedQuestionId.equals(question.questionId()))
            .toList();
        if (availabilityQuestions.size() != 1) {
            throw invalidAvailabilityResponse();
        }
        QuestionWithOptions availabilityQuestion = availabilityQuestions.getFirst();

        boolean invalidAvailabilityQuestion = availabilityQuestion.type() != QuestionType.SCHEDULE
            || !availabilityQuestion.isRequired();
        boolean hasOtherRequiredQuestion = questions.stream()
            .anyMatch(question -> question.isRequired() && !expectedQuestionId.equals(question.questionId()));
        if (invalidAvailabilityQuestion || hasOtherRequiredQuestion) {
            throw invalidAvailabilityResponse();
        }
    }

    private void validateAvailabilityTimes(List<Instant> times, Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw invalidAvailabilityPeriod();
        }
        boolean outOfRange = times.stream()
            .anyMatch(time -> time.isBefore(startsAt) || !time.isBefore(endsAt));
        if (outOfRange) {
            throw invalidAvailabilityPeriod();
        }
    }

    private static RecruitingDomainException invalidAvailabilityResponse() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE);
    }

    private static RecruitingDomainException invalidAvailabilityPeriod() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD);
    }
}
