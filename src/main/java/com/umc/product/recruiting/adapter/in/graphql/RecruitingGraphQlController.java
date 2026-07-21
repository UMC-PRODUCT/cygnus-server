package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.CancelRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateAnonymousRecruitingApplicationDraftGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingApplicationDraftGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationCreatedGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationCredentialGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingPublicApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingPublicRoundGroupGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingPublicRoundSearchGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.SubmitAnonymousRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.SubmitRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateAnonymousRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingApplicationDraftGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.CancelAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateAnonymousRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.query.GetAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchPublicRecruitingRoundUseCase;

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
    private final GetAnonymousRecruitingApplicationUseCase getAnonymousApplicationUseCase;
    private final CreateAnonymousRecruitingApplicationDraftUseCase createAnonymousDraftUseCase;
    private final UpdateAnonymousRecruitingApplicationUseCase updateAnonymousApplicationUseCase;
    private final SubmitAnonymousRecruitingApplicationUseCase submitAnonymousApplicationUseCase;
    private final CancelAnonymousRecruitingApplicationUseCase cancelAnonymousApplicationUseCase;
    private final SearchPublicRecruitingRoundUseCase searchPublicRoundUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public java.util.List<RecruitingPublicRoundGroupGraphQlResponse> publicRecruitingRounds(
        @Argument RecruitingPublicRoundSearchGraphQlRequest input
    ) {
        return searchPublicRoundUseCase.searchPublicRounds(input.toQuery()).stream()
            .map(RecruitingPublicRoundGroupGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public RecruitingApplicationFormStructureGraphQlResponse recruitingApplicationFormStructure(
        @Argument Long applicationFormId,
        @Argument RecruitingApplicationFormStructureGraphQlRequest input
    ) {
        return RecruitingApplicationFormStructureGraphQlResponse.from(
            getFormQueryUseCase.getPublicFormStructure(
                applicationFormId,
                input.firstChoice(),
                input.secondChoice()
            )
        );
    }

    @QueryMapping
    public RecruitingPublicApplicationGraphQlResponse recruitingApplicationByCredential(
        @Argument RecruitingApplicationCredentialGraphQlRequest input
    ) {
        return RecruitingPublicApplicationGraphQlResponse.from(
            getAnonymousApplicationUseCase.getByCredential(input.email(), input.applicationKey())
        );
    }

    @QueryMapping
    public RecruitingApplicationGraphQlResponse recruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationGraphQlResponse.from(
            getApplicationQueryUseCase.getById(applicationId, requesterMemberId)
        );
    }

    @MutationMapping
    public RecruitingApplicationCreatedGraphQlResponse createRecruitingApplicationDraft(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument CreateRecruitingApplicationDraftGraphQlRequest input
    ) {
        Long resolvedMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationCreatedGraphQlResponse.from(
            createDraftUseCase.createDraft(input.toCommand(resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationCreatedGraphQlResponse createAnonymousRecruitingApplicationDraft(
        @Argument CreateAnonymousRecruitingApplicationDraftGraphQlRequest input
    ) {
        return RecruitingApplicationCreatedGraphQlResponse.from(
            createAnonymousDraftUseCase.createAnonymousDraft(input.toCommand())
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse updateRecruitingApplicationDraft(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument UpdateRecruitingApplicationDraftGraphQlRequest input
    ) {
        Long resolvedMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationGraphQlResponse.from(
            updateDraftUseCase.updateDraft(input.toCommand(applicationId, resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse updateAnonymousRecruitingApplication(
        @Argument UpdateAnonymousRecruitingApplicationGraphQlRequest input
    ) {
        return RecruitingApplicationGraphQlResponse.from(
            updateAnonymousApplicationUseCase.updateAnonymous(input.toCommand())
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse submitRecruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument SubmitRecruitingApplicationGraphQlRequest input
    ) {
        SubmitRecruitingApplicationGraphQlRequest actualInput = input == null
            ? new SubmitRecruitingApplicationGraphQlRequest(null)
            : input;
        Long resolvedMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationGraphQlResponse.from(
            submitApplicationUseCase.submit(actualInput.toCommand(applicationId, resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse submitAnonymousRecruitingApplication(
        @Argument SubmitAnonymousRecruitingApplicationGraphQlRequest input
    ) {
        return RecruitingApplicationGraphQlResponse.from(
            submitAnonymousApplicationUseCase.submitAnonymous(input.toCommand())
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse cancelRecruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument CancelRecruitingApplicationGraphQlRequest input
    ) {
        CancelRecruitingApplicationGraphQlRequest actualInput = input == null
            ? new CancelRecruitingApplicationGraphQlRequest(null)
            : input;
        Long resolvedMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationGraphQlResponse.from(
            cancelApplicationUseCase.cancel(actualInput.toCommand(applicationId, resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse cancelAnonymousRecruitingApplication(
        @Argument RecruitingApplicationCredentialGraphQlRequest input
    ) {
        return RecruitingApplicationGraphQlResponse.from(
            cancelAnonymousApplicationUseCase.cancelAnonymous(CancelAnonymousRecruitingApplicationCommand.builder()
                .credentialEmail(input.email())
                .applicationKey(input.applicationKey())
                .build())
        );
    }
}
