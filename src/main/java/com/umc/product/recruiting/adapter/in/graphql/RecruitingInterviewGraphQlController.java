package com.umc.product.recruiting.adapter.in.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.recruiting.adapter.in.graphql.dto.AssignRecruitingInterviewGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.FindRecruitingInterviewScheduleCandidatesGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewEvaluationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleCandidateGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.SaveRecruitingInterviewEvaluationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.SendRecruitingInterviewGuideGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.SkipRecruitingInterviewGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.AssignRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.FindRecruitingInterviewScheduleCandidatesUseCase;
import com.umc.product.recruiting.application.port.in.command.SaveRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.SendRecruitingInterviewGuideUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewEvaluationUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingInterviewGraphQlController {

    private final GetRecruitingInterviewEvaluationUseCase getEvaluationUseCase;
    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingFormQueryUseCase getFormQueryUseCase;
    private final AssignRecruitingInterviewUseCase assignInterviewUseCase;
    private final SkipRecruitingInterviewUseCase skipInterviewUseCase;
    private final FindRecruitingInterviewScheduleCandidatesUseCase findScheduleCandidatesUseCase;
    private final SendRecruitingInterviewGuideUseCase sendInterviewGuideUseCase;
    private final SaveRecruitingInterviewEvaluationUseCase saveEvaluationUseCase;
    private final SubmitRecruitingInterviewEvaluationUseCase submitEvaluationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public List<RecruitingInterviewEvaluationGraphQlResponse> recruitingVisibleInterviewEvaluations(
        @Argument Long seasonId,
        @Argument Long applicationId
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        Long requesterMemberId = permissionSupport.currentMemberId();
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.READ);
        return getEvaluationUseCase.listVisibleEvaluations(applicationId, requesterMemberId).stream()
            .map(RecruitingInterviewEvaluationGraphQlResponse::from)
            .toList();
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse assignRecruitingInterview(
        @Argument Long seasonId,
        @Argument Long applicationId,
        @Argument AssignRecruitingInterviewGraphQlRequest input
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        Long requesterMemberId = permissionSupport.currentMemberId();
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        return RecruitingIdGraphQlResponse.from(
            assignInterviewUseCase.assign(input.toCommand(applicationId, requesterMemberId))
        );
    }

    @MutationMapping
    public Boolean skipRecruitingInterview(
        @Argument Long seasonId,
        @Argument Long applicationId,
        @Argument SkipRecruitingInterviewGraphQlRequest input
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        Long requesterMemberId = permissionSupport.currentMemberId();
        SkipRecruitingInterviewGraphQlRequest actualInput = input == null
            ? new SkipRecruitingInterviewGraphQlRequest(null)
            : input;
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        skipInterviewUseCase.skip(actualInput.toCommand(applicationId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public List<RecruitingInterviewScheduleCandidateGraphQlResponse> findRecruitingInterviewScheduleCandidates(
        @Argument Long seasonId,
        @Argument FindRecruitingInterviewScheduleCandidatesGraphQlRequest input
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getFormQueryUseCase.isFormBelongsToSeason(input.formId(), seasonId)
        );
        permissionSupport.assertRecruitmentPermission(seasonId, PermissionType.READ);
        return findScheduleCandidatesUseCase.findScheduleCandidates(input.toCommand()).stream()
            .map(candidate -> RecruitingInterviewScheduleCandidateGraphQlResponse.from(
                candidate.startsAt(),
                candidate.endsAt(),
                candidate.availableApplicantCount()
            ))
            .toList();
    }

    @MutationMapping
    public Boolean sendRecruitingInterviewGuide(
        @Argument Long seasonId,
        @Argument Long applicationId,
        @Argument SendRecruitingInterviewGuideGraphQlRequest input
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(seasonId, PermissionType.EDIT);
        sendInterviewGuideUseCase.sendGuide(input.toCommand(applicationId));
        return true;
    }

    @MutationMapping
    public Boolean saveRecruitingInterviewEvaluation(
        @Argument Long seasonId,
        @Argument Long assignmentId,
        @Argument SaveRecruitingInterviewEvaluationGraphQlRequest input
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getEvaluationUseCase.isAssignmentBelongsToSeason(assignmentId, seasonId)
        );
        Long requesterMemberId = permissionSupport.currentMemberId();
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        saveEvaluationUseCase.saveEvaluation(input.toSaveCommand(assignmentId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public Boolean submitRecruitingInterviewEvaluation(
        @Argument Long seasonId,
        @Argument Long assignmentId,
        @Argument SaveRecruitingInterviewEvaluationGraphQlRequest input
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getEvaluationUseCase.isAssignmentBelongsToSeason(assignmentId, seasonId)
        );
        Long requesterMemberId = permissionSupport.currentMemberId();
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        submitEvaluationUseCase.submitEvaluation(input.toSubmitCommand(assignmentId, requesterMemberId));
        return true;
    }
}
