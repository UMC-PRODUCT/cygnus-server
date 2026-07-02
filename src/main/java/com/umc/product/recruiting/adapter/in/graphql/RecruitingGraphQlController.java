package com.umc.product.recruiting.adapter.in.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.recruiting.adapter.in.graphql.dto.CancelRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingApplicationDraftGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormSearchGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationResultGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationResultGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.SubmitRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingApplicationDraftGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingGraphQlController {

    private final GetRecruitingFormQueryUseCase getFormQueryUseCase;
    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final CreateRecruitingApplicationDraftUseCase createDraftUseCase;
    private final UpdateRecruitingApplicationDraftUseCase updateDraftUseCase;
    private final SubmitRecruitingApplicationUseCase submitApplicationUseCase;
    private final CancelRecruitingApplicationUseCase cancelApplicationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public List<RecruitingApplicationFormGraphQlResponse> recruitingApplicationForms(
        @Argument RecruitingApplicationFormSearchGraphQlRequest input
    ) {
        return getFormQueryUseCase.listPublicForms(input.gisuId(), input.schoolId()).stream()
            .map(RecruitingApplicationFormGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public RecruitingApplicationResultGraphQlResponse recruitingApplicationResult(
        @Argument RecruitingApplicationResultGraphQlRequest input
    ) {
        return RecruitingApplicationResultGraphQlResponse.from(
            getApplicationQueryUseCase.getAnonymousResult(input.applicationNo(), input.applicantIdentityKey())
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse createRecruitingApplicationDraft(
        @Argument CreateRecruitingApplicationDraftGraphQlRequest input
    ) {
        Long resolvedMemberId = permissionSupport.nullableCurrentMemberId();
        return RecruitingApplicationGraphQlResponse.from(createDraftUseCase.createDraft(input.toCommand(resolvedMemberId)));
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse updateRecruitingApplicationDraft(
        @Argument Long applicationId,
        @Argument UpdateRecruitingApplicationDraftGraphQlRequest input
    ) {
        Long resolvedMemberId = permissionSupport.nullableCurrentMemberId();
        return RecruitingApplicationGraphQlResponse.from(
            updateDraftUseCase.updateDraft(input.toCommand(applicationId, resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse submitRecruitingApplication(
        @Argument Long applicationId,
        @Argument SubmitRecruitingApplicationGraphQlRequest input
    ) {
        SubmitRecruitingApplicationGraphQlRequest actualInput = input == null
            ? new SubmitRecruitingApplicationGraphQlRequest(null, null)
            : input;
        Long resolvedMemberId = permissionSupport.nullableCurrentMemberId();
        return RecruitingApplicationGraphQlResponse.from(
            submitApplicationUseCase.submit(actualInput.toCommand(applicationId, resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse cancelRecruitingApplication(
        @Argument Long applicationId,
        @Argument CancelRecruitingApplicationGraphQlRequest input
    ) {
        CancelRecruitingApplicationGraphQlRequest actualInput = input == null
            ? new CancelRecruitingApplicationGraphQlRequest(null, null)
            : input;
        Long resolvedMemberId = permissionSupport.nullableCurrentMemberId();
        return RecruitingApplicationGraphQlResponse.from(
            cancelApplicationUseCase.cancel(actualInput.toCommand(applicationId, resolvedMemberId))
        );
    }
}
