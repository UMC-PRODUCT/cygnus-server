package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.converter.RecruitingApplicationFormStructureGraphQlConverter;
import com.umc.product.recruiting.adapter.in.graphql.dto.CancelRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationAccessGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationCreatedGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationFormStructureGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationPrivateGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.SubmitRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingApplicationGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.CancelAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateAnonymousRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.query.GetAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingGraphQlController {

    private final GetRecruitingFormQueryUseCase getFormQueryUseCase;
    private final GetRecruitingResourceUseCase getResourceUseCase;
    private final GetAnonymousRecruitingApplicationUseCase getAnonymousApplicationUseCase;
    private final CreateRecruitingApplicationDraftUseCase createDraftUseCase;
    private final CreateAnonymousRecruitingApplicationDraftUseCase createAnonymousDraftUseCase;
    private final UpdateRecruitingApplicationDraftUseCase updateDraftUseCase;
    private final UpdateAnonymousRecruitingApplicationUseCase updateAnonymousApplicationUseCase;
    private final SubmitRecruitingApplicationUseCase submitApplicationUseCase;
    private final SubmitAnonymousRecruitingApplicationUseCase submitAnonymousApplicationUseCase;
    private final CancelRecruitingApplicationUseCase cancelApplicationUseCase;
    private final CancelAnonymousRecruitingApplicationUseCase cancelAnonymousApplicationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public RecruitingApplicationGraphQlResponse recruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingApplicationAccessGraphQlRequest access
    ) {
        if (access.usesCredential()) {
            return anonymousApplication(access.credential().email(), access.credential().applicationKey());
        }
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationGraphQlResponse.from(
            getResourceUseCase.getApplication(access.applicationId(), requesterMemberId)
        );
    }

    @MutationMapping
    public RecruitingApplicationCreatedGraphQlResponse createRecruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument CreateRecruitingApplicationGraphQlRequest input
    ) {
        if (memberPrincipal != null) {
            Long memberId = memberPrincipal.getMemberId();
            RecruitingApplicationCreatedInfo created = createDraftUseCase.createDraft(input.toMemberCommand(memberId));
            return new RecruitingApplicationCreatedGraphQlResponse(
                memberApplication(created.applicationId(), memberId),
                null
            );
        }
        RecruitingApplicationCreatedInfo created = createAnonymousDraftUseCase.createAnonymousDraft(
            input.toAnonymousCommand()
        );
        return new RecruitingApplicationCreatedGraphQlResponse(
            anonymousApplication(input.applicantEmail(), created.applicationKey()),
            new RecruitingApplicationCreatedGraphQlResponse.Credential(
                input.applicantEmail(),
                created.applicationKey()
            )
        );
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse updateRecruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingApplicationAccessGraphQlRequest access,
        @Argument UpdateRecruitingApplicationGraphQlRequest input
    ) {
        if (access.usesCredential()) {
            updateAnonymousApplicationUseCase.updateAnonymous(input.toAnonymousCommand(access.credential()));
            return anonymousApplication(input.applicantEmail(), access.credential().applicationKey());
        }
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        updateDraftUseCase.updateDraft(input.toMemberCommand(access.applicationId(), requesterMemberId));
        return memberApplication(access.applicationId(), requesterMemberId);
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse submitRecruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingApplicationAccessGraphQlRequest access,
        @Argument SubmitRecruitingApplicationGraphQlRequest input
    ) {
        if (access.usesCredential()) {
            submitAnonymousApplicationUseCase.submitAnonymous(SubmitAnonymousRecruitingApplicationCommand.builder()
                .credentialEmail(access.credential().email())
                .applicationKey(access.credential().applicationKey())
                .build());
            return anonymousApplication(access.credential().email(), access.credential().applicationKey());
        }
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        SubmitRecruitingApplicationGraphQlRequest actualInput = input == null
            ? new SubmitRecruitingApplicationGraphQlRequest(null)
            : input;
        submitApplicationUseCase.submit(actualInput.toCommand(access.applicationId(), requesterMemberId));
        return memberApplication(access.applicationId(), requesterMemberId);
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse cancelRecruitingApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingApplicationAccessGraphQlRequest access,
        @Argument CancelRecruitingApplicationGraphQlRequest input
    ) {
        if (access.usesCredential()) {
            cancelAnonymousApplicationUseCase.cancelAnonymous(CancelAnonymousRecruitingApplicationCommand.builder()
                .credentialEmail(access.credential().email())
                .applicationKey(access.credential().applicationKey())
                .build());
            return anonymousApplication(access.credential().email(), access.credential().applicationKey());
        }
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        CancelRecruitingApplicationGraphQlRequest actualInput = input == null
            ? new CancelRecruitingApplicationGraphQlRequest(null)
            : input;
        cancelApplicationUseCase.cancel(actualInput.toCommand(access.applicationId(), requesterMemberId));
        return memberApplication(access.applicationId(), requesterMemberId);
    }

    @SchemaMapping(typeName = "RecruitingRound", field = "applicationForm")
    public RecruitingApplicationFormGraphQlResponse applicationForm(RecruitingRoundGraphQlResponse round) {
        if (round.applicationForm() != null) {
            return round.applicationForm();
        }
        return getFormQueryUseCase.findApplicationFormByRoundId(round.id())
            .map(RecruitingApplicationFormGraphQlResponse::from)
            .orElse(null);
    }

    @SchemaMapping(typeName = "RecruitingRound", field = "formStructure")
    public RecruitingApplicationFormStructureGraphQlResponse formStructure(
        RecruitingRoundGraphQlResponse round,
        @Argument RecruitingApplicationFormStructureGraphQlRequest input
    ) {
        RecruitingApplicationFormGraphQlResponse applicationForm = applicationForm(round);
        if (applicationForm == null) {
            return null;
        }
        return RecruitingApplicationFormStructureGraphQlConverter.from(
            getFormQueryUseCase.getPublicFormStructure(
                applicationForm.id(),
                input.firstChoice(),
                input.secondChoice()
            )
        );
    }

    @SchemaMapping(typeName = "RecruitingApplication", field = "round")
    public RecruitingRoundGraphQlResponse applicationRound(RecruitingApplicationGraphQlResponse application) {
        if (application.roundId() == null) {
            return null;
        }
        return RecruitingRoundGraphQlResponse.from(
            getResourceUseCase.getRound(application.roundId(), permissionSupport.nullableCurrentMemberId())
        );
    }

    @SchemaMapping(typeName = "RecruitingApplication", field = "private")
    public RecruitingApplicationPrivateGraphQlResponse applicationPrivate(
        RecruitingApplicationGraphQlResponse application
    ) {
        if (application.credentialPrivate() != null) {
            return application.credentialPrivate();
        }
        if (!application.applicantAccess()) {
            return null;
        }
        return RecruitingApplicationPrivateGraphQlResponse.from(
            getResourceUseCase.getApplicantView(application.id(), permissionSupport.currentMemberId())
        );
    }

    private RecruitingApplicationGraphQlResponse memberApplication(Long applicationId, Long requesterMemberId) {
        return RecruitingApplicationGraphQlResponse.from(
            getResourceUseCase.getApplication(applicationId, requesterMemberId)
        );
    }

    private RecruitingApplicationGraphQlResponse anonymousApplication(String email, String applicationKey) {
        return RecruitingApplicationGraphQlResponse.from(
            getAnonymousApplicationUseCase.getByCredential(email, applicationKey)
        );
    }
}
