package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingDecisionGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.PrepareRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResourceInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingDecisionGraphQlController {

    private final DecideRecruitingDocumentUseCase decideDocumentUseCase;
    private final DecideRecruitingFinalUseCase decideFinalUseCase;
    private final PrepareRecruitingRegistrationUseCase prepareRegistrationUseCase;
    private final CancelRecruitingRegistrationUseCase cancelRegistrationUseCase;
    private final ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;
    private final GetRecruitingResourceUseCase getResourceUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @MutationMapping
    public RecruitingApplicationGraphQlResponse decideRecruitingDocument(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument RecruitingDecisionGraphQlRequest input
    ) {
        Long requesterMemberId = requirePermission(memberPrincipal, applicationId, PermissionType.APPROVE);
        decideDocumentUseCase.decideDocument(input.toDocumentCommand(applicationId, requesterMemberId));
        return application(applicationId, requesterMemberId);
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse decideRecruitingFinal(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument RecruitingDecisionGraphQlRequest input
    ) {
        Long requesterMemberId = requirePermission(memberPrincipal, applicationId, PermissionType.APPROVE);
        decideFinalUseCase.decideFinal(input.toFinalCommand(applicationId, requesterMemberId));
        return application(applicationId, requesterMemberId);
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse prepareRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId
    ) {
        Long requesterMemberId = requirePermission(memberPrincipal, applicationId, PermissionType.MANAGE);
        prepareRegistrationUseCase.prepareRegistration(
            PrepareRecruitingRegistrationCommand.of(applicationId, requesterMemberId)
        );
        return application(applicationId, requesterMemberId);
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse cancelRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId
    ) {
        Long requesterMemberId = requirePermission(memberPrincipal, applicationId, PermissionType.MANAGE);
        cancelRegistrationUseCase.cancelRegistration(
            CancelRecruitingRegistrationCommand.of(applicationId, requesterMemberId)
        );
        return application(applicationId, requesterMemberId);
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse confirmRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId
    ) {
        Long requesterMemberId = requirePermission(memberPrincipal, applicationId, PermissionType.MANAGE);
        confirmRegistrationUseCase.confirmRegistration(ConfirmRecruitingRegistrationCommand.builder()
            .applicationId(applicationId)
            .executorMemberId(requesterMemberId)
            .build());
        return application(applicationId, requesterMemberId);
    }

    private Long requirePermission(
        MemberPrincipal memberPrincipal,
        Long applicationId,
        PermissionType permission
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        RecruitingApplicationResourceInfo application = getResourceUseCase.getApplication(
            applicationId,
            requesterMemberId
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, application.seasonId(), permission);
        return requesterMemberId;
    }

    private RecruitingApplicationGraphQlResponse application(Long applicationId, Long requesterMemberId) {
        return RecruitingApplicationGraphQlResponse.from(
            getResourceUseCase.getApplication(applicationId, requesterMemberId)
        );
    }
}
