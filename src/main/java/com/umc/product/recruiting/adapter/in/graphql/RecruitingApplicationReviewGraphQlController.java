package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationReviewDetailGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationReviewPageGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationSearchGraphQlRequest;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingApplicationUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingApplicationReviewGraphQlController {

    private final SearchRecruitingApplicationUseCase searchApplicationUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public RecruitingApplicationReviewPageGraphQlResponse recruitingRoundApplications(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long roundId,
        @Argument RecruitingApplicationSearchGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        RecruitingApplicationSearchGraphQlRequest actualInput = input == null
            ? new RecruitingApplicationSearchGraphQlRequest(null, null, null, null)
            : input;
        return RecruitingApplicationReviewPageGraphQlResponse.from(
            searchApplicationUseCase.search(actualInput.toQuery(roundId, requesterMemberId))
        );
    }

    @QueryMapping
    public RecruitingApplicationReviewDetailGraphQlResponse recruitingRoundApplication(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long roundId,
        @Argument Long applicationId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingApplicationReviewDetailGraphQlResponse.from(
            searchApplicationUseCase.getDetail(roundId, applicationId, requesterMemberId)
        );
    }
}
