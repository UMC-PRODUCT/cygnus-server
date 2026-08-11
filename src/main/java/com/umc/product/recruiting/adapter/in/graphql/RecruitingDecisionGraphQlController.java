package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.GraphQlSuccessPayload;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingDecisionGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRegistrationGraphQlRequest;
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
    public GraphQlSuccessPayload decideRecruitingDocument(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingDecisionGraphQlRequest input
    ) {
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            input.decodedSeasonId(),
            input.decodedApplicationId(),
            PermissionType.APPROVE
        );
        decideDocumentUseCase.decideDocument(input.toDocumentCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload decideRecruitingFinal(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingDecisionGraphQlRequest input
    ) {
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            input.decodedSeasonId(),
            input.decodedApplicationId(),
            PermissionType.APPROVE
        );
        decideFinalUseCase.decideFinal(input.toFinalCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload prepareRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingRegistrationGraphQlRequest input
    ) {
        Long applicationId = input.decodedApplicationId();
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            input.decodedSeasonId(),
            applicationId,
            PermissionType.MANAGE
        );
        prepareRegistrationUseCase.prepareRegistration(
            PrepareRecruitingRegistrationCommand.of(applicationId, requesterMemberId)
        );
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload cancelRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingRegistrationGraphQlRequest input
    ) {
        Long applicationId = input.decodedApplicationId();
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            input.decodedSeasonId(),
            applicationId,
            PermissionType.MANAGE
        );
        cancelRegistrationUseCase.cancelRegistration(
            CancelRecruitingRegistrationCommand.of(applicationId, requesterMemberId)
        );
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload confirmRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingRegistrationGraphQlRequest input
    ) {
        Long applicationId = input.decodedApplicationId();
        Long requesterMemberId = requireApplicationPermission(
            memberPrincipal,
            input.decodedSeasonId(),
            applicationId,
            PermissionType.MANAGE
        );
        confirmRegistrationUseCase.confirmRegistration(ConfirmRecruitingRegistrationCommand.builder()
            .applicationId(applicationId)
            .executorMemberId(requesterMemberId)
            .build());
        return GraphQlSuccessPayload.ok();
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
