package com.umc.product.recruiting.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.global.graphql.dto.PageGraphQlRequest;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationPageGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationReviewGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingApplicationSearchGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundManagementGraphQlResponse;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingApplicationUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingApplicationReviewGraphQlController {

    private final SearchRecruitingApplicationUseCase searchApplicationUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @SchemaMapping(typeName = "RecruitingRoundManagement", field = "applications")
    public RecruitingApplicationPageGraphQlResponse applications(
        RecruitingRoundManagementGraphQlResponse management,
        @Argument("filter") RecruitingApplicationSearchGraphQlRequest filter,
        @Argument PageGraphQlRequest page
    ) {
        RecruitingApplicationSearchGraphQlRequest actualFilter = filter == null
            ? new RecruitingApplicationSearchGraphQlRequest(null, null)
            : filter;
        return RecruitingApplicationPageGraphQlResponse.from(
            searchApplicationUseCase.search(actualFilter.toQuery(
                management.roundId(),
                permissionSupport.currentMemberId(),
                PageGraphQlRequest.defaultIfNull(page).toPageable()
            )),
            management.roundId()
        );
    }

    @BatchMapping(typeName = "RecruitingApplication", field = "review")
    public Map<RecruitingApplicationGraphQlResponse, RecruitingApplicationReviewGraphQlResponse> reviews(
        List<RecruitingApplicationGraphQlResponse> applications
    ) {
        Set<Long> reviewableIds = applications.stream()
            .filter(RecruitingApplicationGraphQlResponse::reviewAccess)
            .map(RecruitingApplicationGraphQlResponse::id)
            .collect(Collectors.toSet());
        Map<Long, com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationDetailInfo> details =
            searchApplicationUseCase.getDetails(reviewableIds, permissionSupport.currentMemberId());

        Map<RecruitingApplicationGraphQlResponse, RecruitingApplicationReviewGraphQlResponse> result =
            new LinkedHashMap<>();
        for (RecruitingApplicationGraphQlResponse application : applications) {
            var detail = details.get(application.id());
            result.put(
                application,
                detail == null ? null : RecruitingApplicationReviewGraphQlResponse.from(detail)
            );
        }
        return result;
    }

    @BatchMapping(typeName = "RecruitingApplicationReview", field = "applicant")
    public Map<RecruitingApplicationReviewGraphQlResponse, MemberPublicInfo> applicants(
        List<RecruitingApplicationReviewGraphQlResponse> reviews
    ) {
        Set<Long> memberIds = reviews.stream()
            .map(RecruitingApplicationReviewGraphQlResponse::applicantMemberId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, MemberPublicInfo> byId = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        Map<RecruitingApplicationReviewGraphQlResponse, MemberPublicInfo> result = new LinkedHashMap<>();
        for (RecruitingApplicationReviewGraphQlResponse review : reviews) {
            result.put(review, byId.get(review.applicantMemberId()));
        }
        return result;
    }
}
