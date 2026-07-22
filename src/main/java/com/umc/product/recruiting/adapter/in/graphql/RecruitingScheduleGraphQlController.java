package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationAccessGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationPrivateGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationReviewGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.Confirm;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.RequestAvailability;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.SkipRecruitingInterviewGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingInterviewAvailabilityCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResourceInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingScheduleGraphQlController {

    private final GetRecruitingInterviewScheduleUseCase getInterviewScheduleUseCase;
    private final ManageRecruitingInterviewScheduleUseCase manageInterviewScheduleUseCase;
    private final SkipRecruitingInterviewUseCase skipInterviewUseCase;
    private final GetRecruitingResourceUseCase getResourceUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @SchemaMapping(typeName = "RecruitingApplicationPrivate", field = "interviewSchedule")
    public RecruitingInterviewScheduleGraphQlResponse privateSchedule(
        RecruitingApplicationPrivateGraphQlResponse privateView
    ) {
        Long requesterMemberId = permissionSupport.nullableCurrentMemberId();
        if (requesterMemberId == null) {
            return null;
        }
        return schedule(privateView.applicationId(), requesterMemberId);
    }

    @SchemaMapping(typeName = "RecruitingApplicationReview", field = "interviewSchedule")
    public RecruitingInterviewScheduleGraphQlResponse reviewSchedule(
        RecruitingApplicationReviewGraphQlResponse review
    ) {
        return schedule(review.applicationId(), permissionSupport.currentMemberId());
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse skipRecruitingInterview(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument SkipRecruitingInterviewGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        RecruitingApplicationResourceInfo application = getResourceUseCase.getApplication(
            applicationId,
            requesterMemberId
        );
        permissionSupport.assertRecruitmentPermission(
            requesterMemberId,
            application.seasonId(),
            PermissionType.EDIT
        );
        SkipRecruitingInterviewGraphQlRequest actualInput = input == null
            ? new SkipRecruitingInterviewGraphQlRequest(null)
            : input;
        skipInterviewUseCase.skip(actualInput.toCommand(applicationId, requesterMemberId));
        return application(applicationId, requesterMemberId);
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse requestRecruitingInterviewAvailability(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument RequestAvailability input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageInterviewScheduleUseCase.requestAvailability(input.toCommand(applicationId, requesterMemberId));
        return application(applicationId, requesterMemberId);
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse submitRecruitingInterviewAvailability(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingApplicationAccessGraphQlRequest access
    ) {
        if (access.usesCredential()) {
            throw new IllegalArgumentException("면접 가능 시간 제출은 인증 회원만 지원합니다.");
        }
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageInterviewScheduleUseCase.submitAvailability(
            SubmitRecruitingInterviewAvailabilityCommand.of(access.applicationId(), requesterMemberId)
        );
        return application(access.applicationId(), requesterMemberId);
    }

    @MutationMapping
    public RecruitingApplicationGraphQlResponse confirmRecruitingInterviewSchedule(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument Confirm input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageInterviewScheduleUseCase.confirm(input.toCommand(applicationId, requesterMemberId));
        return application(applicationId, requesterMemberId);
    }

    private RecruitingInterviewScheduleGraphQlResponse schedule(Long applicationId, Long requesterMemberId) {
        return getInterviewScheduleUseCase.findByApplicationId(applicationId, requesterMemberId)
            .map(RecruitingInterviewScheduleGraphQlResponse::from)
            .orElse(null);
    }

    private RecruitingApplicationGraphQlResponse application(Long applicationId, Long requesterMemberId) {
        return RecruitingApplicationGraphQlResponse.from(
            getResourceUseCase.getApplication(applicationId, requesterMemberId)
        );
    }
}
