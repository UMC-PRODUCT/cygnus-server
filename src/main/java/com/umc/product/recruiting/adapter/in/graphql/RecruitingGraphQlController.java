package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.CancelRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateAnonymousRecruitingApplicationDraftGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingApplicationDraftGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationCredentialGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationDraftCreatedGraphQlPayload;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlPayload;
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
    public RelayConnection<RecruitingPublicRoundGroupGraphQlResponse> publicRecruitingRounds(
        @Argument RecruitingPublicRoundSearchGraphQlRequest input,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        return RelayConnection.fromList(
            searchPublicRoundUseCase.searchPublicRounds(input.toQuery()),
            arguments,
            RecruitingPublicRoundGroupGraphQlResponse::from
        );
    }

    @QueryMapping
    public RecruitingApplicationFormStructureGraphQlResponse recruitingApplicationFormStructure(
        @Argument String applicationFormId,
        @Argument RecruitingApplicationFormStructureGraphQlRequest input
    ) {
        return RecruitingApplicationFormStructureGraphQlResponse.from(
            getFormQueryUseCase.getPublicFormStructure(
                GlobalId.decodeLong(applicationFormId, GlobalIdTypes.RECRUITING_APPLICATION_FORM),
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
        @Argument String applicationId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationGraphQlResponse.from(
            getApplicationQueryUseCase.getById(
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId
            )
        );
    }

    @MutationMapping
    public RecruitingApplicationDraftCreatedGraphQlPayload createRecruitingApplicationDraft(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument CreateRecruitingApplicationDraftGraphQlRequest input
    ) {
        Long resolvedMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationDraftCreatedGraphQlPayload.from(
            createDraftUseCase.createDraft(input.toCommand(resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationDraftCreatedGraphQlPayload createAnonymousRecruitingApplicationDraft(
        @Argument CreateAnonymousRecruitingApplicationDraftGraphQlRequest input
    ) {
        return RecruitingApplicationDraftCreatedGraphQlPayload.from(
            createAnonymousDraftUseCase.createAnonymousDraft(input.toCommand())
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlPayload updateRecruitingApplicationDraft(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument UpdateRecruitingApplicationDraftGraphQlRequest input
    ) {
        Long resolvedMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationGraphQlPayload.from(
            updateDraftUseCase.updateDraft(input.toCommand(resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlPayload updateAnonymousRecruitingApplication(
        @Argument UpdateAnonymousRecruitingApplicationGraphQlRequest input
    ) {
        return RecruitingApplicationGraphQlPayload.from(
            updateAnonymousApplicationUseCase.updateAnonymous(input.toCommand())
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlPayload submitRecruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument SubmitRecruitingApplicationGraphQlRequest input
    ) {
        Long resolvedMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationGraphQlPayload.from(
            submitApplicationUseCase.submit(input.toCommand(resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlPayload submitAnonymousRecruitingApplication(
        @Argument SubmitAnonymousRecruitingApplicationGraphQlRequest input
    ) {
        return RecruitingApplicationGraphQlPayload.from(
            submitAnonymousApplicationUseCase.submitAnonymous(input.toCommand())
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlPayload cancelRecruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument CancelRecruitingApplicationGraphQlRequest input
    ) {
        Long resolvedMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationGraphQlPayload.from(
            cancelApplicationUseCase.cancel(input.toCommand(resolvedMemberId))
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlPayload cancelAnonymousRecruitingApplication(
        @Argument RecruitingApplicationCredentialGraphQlRequest input
    ) {
        return RecruitingApplicationGraphQlPayload.from(
            cancelAnonymousApplicationUseCase.cancelAnonymous(CancelAnonymousRecruitingApplicationCommand.builder()
                .credentialEmail(input.email())
                .applicationKey(input.applicationKey())
                .build())
        );
    }
}
