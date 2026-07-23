package com.umc.product.analytics.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.analytics.adapter.in.graphql.dto.AdminAnalyticsGraphQlRequest;
import com.umc.product.analytics.adapter.in.graphql.dto.AdminAnalyticsGraphQlResponse;
import com.umc.product.analytics.adapter.in.graphql.dto.AdminAnalyticsSchoolFilterGraphQlRequest;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardActionQueueUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardContextUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardSummaryUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminRiskChallengerUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminSchoolSummaryUseCase;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardActionQueueInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardContextInfo;
import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.graphql.dto.PageGraphQlRequest;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdminAnalyticsGraphQlController {

    private final GetAdminDashboardContextUseCase getContextUseCase;
    private final GetAdminDashboardSummaryUseCase getSummaryUseCase;
    private final GetAdminDashboardActionQueueUseCase getActionQueueUseCase;
    private final GetAdminRiskChallengerUseCase getRiskChallengerUseCase;
    private final GetAdminSchoolSummaryUseCase getSchoolSummaryUseCase;

    @QueryMapping
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminAnalyticsGraphQlResponse.Root adminAnalytics(
        @CurrentMember MemberPrincipal principal,
        @Argument AdminAnalyticsGraphQlRequest input
    ) {
        return new AdminAnalyticsGraphQlResponse.Root(
            principal.getMemberId(),
            AdminAnalyticsGraphQlRequest.defaultIfNull(input)
        );
    }

    @SchemaMapping(typeName = "AdminAnalytics", field = "context")
    public AdminDashboardContextInfo context(AdminAnalyticsGraphQlResponse.Root source) {
        return getContextUseCase.getContext(source.requesterMemberId());
    }

    @SchemaMapping(typeName = "AdminAnalytics", field = "summary")
    public AdminAnalyticsGraphQlResponse.Summary summary(AdminAnalyticsGraphQlResponse.Root source) {
        return AdminAnalyticsGraphQlResponse.Summary.from(
            getSummaryUseCase.getSummary(source.input().toDashboardQuery(source.requesterMemberId()))
        );
    }

    @SchemaMapping(typeName = "AdminAnalytics", field = "actionQueue")
    public AdminDashboardActionQueueInfo actionQueue(AdminAnalyticsGraphQlResponse.Root source) {
        return getActionQueueUseCase.getActionQueue(
            source.input().toActionQueueQuery(source.requesterMemberId())
        );
    }

    @SchemaMapping(typeName = "AdminAnalytics", field = "riskChallengers")
    public AdminAnalyticsGraphQlResponse.RiskPage riskChallengers(
        AdminAnalyticsGraphQlResponse.Root source,
        @Argument PageGraphQlRequest page
    ) {
        return AdminAnalyticsGraphQlResponse.RiskPage.from(
            getRiskChallengerUseCase.getRiskChallengers(
                source.input().toRiskQuery(source.requesterMemberId(), page)
            )
        );
    }

    @SchemaMapping(typeName = "AdminAnalytics", field = "schools")
    public AdminAnalyticsGraphQlResponse.SchoolPage schools(
        AdminAnalyticsGraphQlResponse.Root source,
        @Argument AdminAnalyticsSchoolFilterGraphQlRequest filter,
        @Argument PageGraphQlRequest page
    ) {
        return AdminAnalyticsGraphQlResponse.SchoolPage.from(
            getSchoolSummaryUseCase.getSchoolSummaries(
                source.input().toSchoolQuery(source.requesterMemberId(), filter, page)
            )
        );
    }
}
