package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.AddRecruitingFormSectionPolicyGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.LinkRecruitingApplicationFormGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingFormSectionPolicyUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingFormAdminGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingFormQueryUseCase getFormQueryUseCase;
    private final LinkRecruitingApplicationFormUseCase linkFormUseCase;
    private final ManageRecruitingFormSectionPolicyUseCase manageFormSectionPolicyUseCase;
    private final PublishRecruitingApplicationFormUseCase publishFormUseCase;
    private final CloseRecruitingApplicationFormUseCase closeFormUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @MutationMapping
    public RecruitingIdGraphQlResponse linkRecruitingApplicationForm(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument LinkRecruitingApplicationFormGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.WRITE);
        return RecruitingIdGraphQlResponse.from(linkFormUseCase.link(input.toCommand(seasonId, roundId)));
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse addRecruitingFormSectionPolicy(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationFormId,
        @Argument AddRecruitingFormSectionPolicyGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        requireFormPermission(requesterMemberId, seasonId, applicationFormId, PermissionType.EDIT);
        return RecruitingIdGraphQlResponse.from(
            manageFormSectionPolicyUseCase.addPolicy(input.toCommand(applicationFormId))
        );
    }

    @MutationMapping
    public Boolean publishRecruitingApplicationForm(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationFormId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        requireFormPermission(requesterMemberId, seasonId, applicationFormId, PermissionType.EDIT);
        publishFormUseCase.publish(PublishRecruitingApplicationFormCommand.builder()
            .seasonId(seasonId)
            .applicationFormId(applicationFormId)
            .requesterMemberId(requesterMemberId)
            .build());
        return true;
    }

    @MutationMapping
    public Boolean closeRecruitingApplicationForm(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationFormId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        requireFormPermission(requesterMemberId, seasonId, applicationFormId, PermissionType.EDIT);
        closeFormUseCase.close(CloseRecruitingApplicationFormCommand.builder()
            .seasonId(seasonId)
            .applicationFormId(applicationFormId)
            .build());
        return true;
    }

    private void requireFormPermission(
        Long requesterMemberId,
        Long seasonId,
        Long applicationFormId,
        PermissionType permission
    ) {
        permissionSupport.assertResourceBelongsToSeason(
            getFormQueryUseCase.isApplicationFormBelongsToSeason(applicationFormId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, permission);
    }
}
