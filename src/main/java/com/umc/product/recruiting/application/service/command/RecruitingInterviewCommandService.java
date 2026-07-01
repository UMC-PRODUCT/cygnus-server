package com.umc.product.recruiting.application.service.command;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.recruiting.application.port.in.command.AssignRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.FindRecruitingInterviewScheduleCandidatesUseCase;
import com.umc.product.recruiting.application.port.in.command.SaveRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.SendRecruitingInterviewGuideUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.AssignRecruitingInterviewCommand;
import com.umc.product.recruiting.application.port.in.command.dto.FindRecruitingInterviewScheduleCandidatesCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SaveRecruitingInterviewEvaluationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SendRecruitingInterviewGuideCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SkipRecruitingInterviewCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewEvaluationCommand;
import com.umc.product.recruiting.application.port.out.FindRecruitingScheduleOverlapPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewAssignmentPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewAssignmentPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewEvaluationPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingInterviewScheduleCandidate;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingInterviewAssignment;
import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingInterviewCommandService implements
    AssignRecruitingInterviewUseCase,
    SkipRecruitingInterviewUseCase,
    FindRecruitingInterviewScheduleCandidatesUseCase,
    SendRecruitingInterviewGuideUseCase,
    SaveRecruitingInterviewEvaluationUseCase,
    SubmitRecruitingInterviewEvaluationUseCase {

    private static final String FROM_ADDRESS = "no-reply@umc.product";
    private static final String FROM_DISPLAY_NAME = "UMC";

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final SaveRecruitingApplicationPort saveApplicationPort;
    private final LoadRecruitingInterviewAssignmentPort loadAssignmentPort;
    private final SaveRecruitingInterviewAssignmentPort saveAssignmentPort;
    private final LoadRecruitingInterviewEvaluationPort loadEvaluationPort;
    private final SaveRecruitingInterviewEvaluationPort saveEvaluationPort;
    private final FindRecruitingScheduleOverlapPort findScheduleOverlapPort;
    private final SendEmailPort sendEmailPort;

    @Override
    public Long assign(AssignRecruitingInterviewCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        application.assignInterview(command.assignedByMemberId(), "면접 배정");
        RecruitingInterviewAssignment assignment = RecruitingInterviewAssignment.assign(
            application,
            command.interviewerMemberId(),
            command.startsAt(),
            command.endsAt(),
            command.location()
        );
        RecruitingInterviewAssignment saved = saveAssignmentPort.saveAssignment(assignment);
        saveApplicationPort.save(application);
        return saved.getId();
    }

    @Override
    public void skip(SkipRecruitingInterviewCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        application.skipInterview(command.skippedByMemberId(), command.reason());
        saveApplicationPort.save(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitingInterviewScheduleCandidate> findScheduleCandidates(
        FindRecruitingInterviewScheduleCandidatesCommand command
    ) {
        return findScheduleOverlapPort.findOverlaps(command.formId(), command.formResponseIds());
    }

    @Override
    public void sendGuide(SendRecruitingInterviewGuideCommand command) {
        RecruitingApplication application = loadApplicationPort.getByIdWithDetails(command.applicationId());
        String body = buildGuideBody(application, command.startsAt(), command.location());
        sendEmailPort.send(new EmailMessage(
            FROM_ADDRESS,
            FROM_DISPLAY_NAME,
            command.recipientEmail(),
            "[UMC] 면접 안내",
            body
        ));
    }

    @Override
    public void saveEvaluation(SaveRecruitingInterviewEvaluationCommand command) {
        RecruitingInterviewAssignment assignment = loadAssignmentPort.getAssignmentById(command.assignmentId());
        RecruitingInterviewEvaluation evaluation = loadEvaluationPort
            .findByApplicationIdAndEvaluatorMemberId(assignment.getApplication().getId(), command.evaluatorMemberId())
            .orElseGet(() -> RecruitingInterviewEvaluation.createDraft(assignment, command.evaluatorMemberId()));
        evaluation.updateDraft(command.score(), command.comment());
        saveEvaluationPort.saveEvaluation(evaluation);
    }

    @Override
    public void submitEvaluation(SubmitRecruitingInterviewEvaluationCommand command) {
        RecruitingInterviewAssignment assignment = loadAssignmentPort.getAssignmentById(command.assignmentId());
        RecruitingInterviewEvaluation evaluation = loadEvaluationPort
            .findByApplicationIdAndEvaluatorMemberId(assignment.getApplication().getId(), command.evaluatorMemberId())
            .orElseGet(() -> RecruitingInterviewEvaluation.createDraft(assignment, command.evaluatorMemberId()));
        evaluation.validateNoSubmittedDuplicate(
            loadEvaluationPort.listSubmittedByApplicationId(assignment.getApplication().getId())
        );
        evaluation.submit(command.score(), command.comment());
        saveEvaluationPort.saveEvaluation(evaluation);
    }

    private String buildGuideBody(RecruitingApplication application, Instant startsAt, String location) {
        return """
            <p>UMC 면접 안내입니다.</p>
            <p>지원서 번호: %s</p>
            <p>면접 시간: %s</p>
            <p>장소: %s</p>
            """.formatted(application.getApplicationNo(), startsAt, location);
    }
}
