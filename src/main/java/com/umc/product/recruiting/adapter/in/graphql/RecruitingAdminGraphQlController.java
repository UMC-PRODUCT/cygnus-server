package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingSeasonGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.LinkRecruitingApplicationFormGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingDecisionGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingStatusSummaryGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingStatusSummaryGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingRoundStatusGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingSeasonStatusGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingAdminGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingFormQueryUseCase getFormQueryUseCase;
    private final CreateRecruitingSeasonUseCase createSeasonUseCase;
    private final UpdateRecruitingSeasonStatusUseCase updateSeasonStatusUseCase;
    private final CreateRecruitingRoundUseCase createRoundUseCase;
    private final UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    private final LinkRecruitingApplicationFormUseCase linkFormUseCase;
    private final PublishRecruitingApplicationFormUseCase publishFormUseCase;
    private final CloseRecruitingApplicationFormUseCase closeFormUseCase;
    private final DecideRecruitingDocumentUseCase decideDocumentUseCase;
    private final DecideRecruitingFinalUseCase decideFinalUseCase;
    private final ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public RecruitingStatusSummaryGraphQlResponse recruitingStatusSummary(
        @Argument RecruitingStatusSummaryGraphQlRequest input
    ) {
        permissionSupport.assertRecruitmentTypePermission(PermissionType.MANAGE);
        return RecruitingStatusSummaryGraphQlResponse.from(
            getApplicationQueryUseCase.getStatusSummary(input.gisuId(), input.schoolId())
        );
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse createRecruitingSeason(@Argument CreateRecruitingSeasonGraphQlRequest input) {
        permissionSupport.assertRecruitmentTypePermission(PermissionType.WRITE);
        return RecruitingIdGraphQlResponse.from(createSeasonUseCase.createSeason(input.toCommand()));
    }

    @MutationMapping
    public Boolean updateRecruitingSeasonStatus(
        @Argument Long seasonId,
        @Argument UpdateRecruitingSeasonStatusGraphQlRequest input
    ) {
        permissionSupport.assertRecruitmentPermission(seasonId, PermissionType.EDIT);
        updateSeasonStatusUseCase.updateSeasonStatus(input.toCommand(seasonId));
        return true;
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse createRecruitingRound(
        @Argument Long seasonId,
        @Argument CreateRecruitingRoundGraphQlRequest input
    ) {
        permissionSupport.assertRecruitmentPermission(seasonId, PermissionType.WRITE);
        return RecruitingIdGraphQlResponse.from(createRoundUseCase.createRound(input.toCommand(seasonId)));
    }

    @MutationMapping
    public Boolean updateRecruitingRoundStatus(
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument UpdateRecruitingRoundStatusGraphQlRequest input
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(seasonId, PermissionType.EDIT);
        updateRoundStatusUseCase.updateRoundStatus(input.toCommand(roundId));
        return true;
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse linkRecruitingApplicationForm(
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument LinkRecruitingApplicationFormGraphQlRequest input
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(seasonId, PermissionType.WRITE);
        return RecruitingIdGraphQlResponse.from(linkFormUseCase.link(input.toCommand(roundId)));
    }

    @MutationMapping
    public Boolean publishRecruitingApplicationForm(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationFormId
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getFormQueryUseCase.isApplicationFormBelongsToSeason(applicationFormId, seasonId)
        );
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        publishFormUseCase.publish(PublishRecruitingApplicationFormCommand.builder()
            .applicationFormId(applicationFormId)
            .requesterMemberId(requesterMemberId)
            .build());
        return true;
    }

    @MutationMapping
    public Boolean closeRecruitingApplicationForm(@Argument Long seasonId, @Argument Long applicationFormId) {
        permissionSupport.assertResourceBelongsToSeason(
            getFormQueryUseCase.isApplicationFormBelongsToSeason(applicationFormId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(seasonId, PermissionType.EDIT);
        closeFormUseCase.close(CloseRecruitingApplicationFormCommand.builder()
            .applicationFormId(applicationFormId)
            .build());
        return true;
    }

    @MutationMapping
    public Boolean decideRecruitingDocument(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationId,
        @Argument RecruitingDecisionGraphQlRequest input
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.APPROVE);
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
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.APPROVE);
        decideFinalUseCase.decideFinal(input.toFinalCommand(applicationId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public Boolean confirmRecruitingRegistration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationId
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.MANAGE);
        confirmRegistrationUseCase.confirmRegistration(ConfirmRecruitingRegistrationCommand.builder()
            .applicationId(applicationId)
            .executorMemberId(requesterMemberId)
            .build());
        return true;
    }
}
