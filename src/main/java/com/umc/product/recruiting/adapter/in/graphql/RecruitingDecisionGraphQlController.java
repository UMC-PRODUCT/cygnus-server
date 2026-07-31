package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingDecisionGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.PrepareRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingDecisionGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final DecideRecruitingDocumentUseCase decideDocumentUseCase;
    private final DecideRecruitingFinalUseCase decideFinalUseCase;
    private final PrepareRecruitingRegistrationUseCase prepareRegistrationUseCase;
    private final CancelRecruitingRegistrationUseCase cancelRegistrationUseCase;
    private final ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @MutationMapping
    public Boolean decideRecruitingDocument(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationId,
        @Argument RecruitingDecisionGraphQlRequest input
    ) {
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            seasonId,
            applicationId,
            PermissionType.APPROVE
        );
        decideDocumentUseCase.decideDocument(input.toDocumentCommand(applicationId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public Boolean decideRecruitingFinal(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationId,
        @Argument RecruitingDecisionGraphQlRequest input
    ) {
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            seasonId,
            applicationId,
            PermissionType.APPROVE
        );
        decideFinalUseCase.decideFinal(input.toFinalCommand(applicationId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public Boolean prepareRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationId
    ) {
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            seasonId,
            applicationId,
            PermissionType.MANAGE
        );
        prepareRegistrationUseCase.prepareRegistration(
            PrepareRecruitingRegistrationCommand.of(applicationId, requesterMemberId)
        );
        return true;
    }

    @MutationMapping
    public Boolean cancelRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationId
    ) {
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            seasonId,
            applicationId,
            PermissionType.MANAGE
        );
        cancelRegistrationUseCase.cancelRegistration(
            CancelRecruitingRegistrationCommand.of(applicationId, requesterMemberId)
        );
        return true;
    }

    @MutationMapping
    public Boolean confirmRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationId
    ) {
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            seasonId,
            applicationId,
            PermissionType.MANAGE
        );
        confirmRegistrationUseCase.confirmRegistration(ConfirmRecruitingRegistrationCommand.builder()
            .applicationId(applicationId)
            .executorMemberId(requesterMemberId)
            .build());
        return true;
    }

    private Long requireApplicationPermission(
        MemberPrincipal memberPrincipal,
        Long seasonId,
        Long applicationId,
        PermissionType permission
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, permission);
        return requesterMemberId;
    }
}
