package com.umc.product.analytics.application.service.query;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.analytics.application.port.in.query.GetAdminGisuPointsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsCommunityActivityUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardActionQueueUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardContextUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardSummaryUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminGisuSummaryUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsAttendanceChaptersUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsAttendancePartsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsAttendanceTagsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsAttendanceUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsOverviewUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsPointsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsSchoolsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsSignupsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsStudyGroupsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminRiskChallengerUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminSchoolSummaryUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminStudyGroupActivityUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminStudyGroupListUseCase;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardActionQueueInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardActionQueueQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardContextInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardSummaryInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminGisuPointsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminGisuPointsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminGisuSummaryInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminGisuSummaryQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceChaptersInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceChaptersQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendancePartsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendancePartsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceTagsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceTagsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsPointsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsPointsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSchoolsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSchoolsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSignupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSignupsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsStudyGroupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsStudyGroupsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminRiskChallengerInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminRiskChallengerQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminSchoolSummaryInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminSchoolSummaryQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListQuery;
import com.umc.product.analytics.application.port.out.LoadAdminDashboardAnalyticsPort;
import com.umc.product.analytics.application.port.out.LoadAdminGisuPointsPort;
import com.umc.product.analytics.application.port.out.LoadAdminGisuSummaryPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsCommunityActivityPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAnalyticsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAttendanceChaptersPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAttendancePartsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAttendancePort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAttendanceTagsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsPointsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsSchoolsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsSignupsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsStudyGroupsPort;
import com.umc.product.analytics.application.port.out.LoadAdminRiskChallengerAnalyticsPort;
import com.umc.product.analytics.application.port.out.LoadAdminSchoolAnalyticsPort;
import com.umc.product.analytics.application.port.out.LoadAdminStudyGroupActivityPort;
import com.umc.product.analytics.application.port.out.LoadAdminStudyGroupListPort;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AnalyticsDomainException;
import com.umc.product.analytics.domain.AnalyticsErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAnalyticsQueryService implements
    GetAdminDashboardSummaryUseCase,
    GetAdminDashboardActionQueueUseCase,
    GetAdminDashboardContextUseCase,
    GetAdminSchoolSummaryUseCase,
    GetAdminRiskChallengerUseCase,
    GetAdminOperationsOverviewUseCase,
    GetAdminOperationsSchoolsUseCase,
    GetAdminOperationsPointsUseCase,
    GetAdminOperationsAttendanceUseCase,
    GetAdminOperationsAttendanceChaptersUseCase,
    GetAdminOperationsAttendanceTagsUseCase,
    GetAdminOperationsAttendancePartsUseCase,
    GetAdminOperationsStudyGroupsUseCase,
    GetAdminOperationsSignupsUseCase,
    GetAdminGisuSummaryUseCase,
    GetAdminGisuPointsUseCase,
    GetAdminOperationsCommunityActivityUseCase,
    GetAdminStudyGroupActivityUseCase,
    GetAdminStudyGroupListUseCase {

    private final AdminAnalyticsScopeResolver scopeResolver;
    private final LoadAdminDashboardAnalyticsPort loadAdminDashboardAnalyticsPort;
    private final LoadAdminSchoolAnalyticsPort loadAdminSchoolAnalyticsPort;
    private final LoadAdminRiskChallengerAnalyticsPort loadAdminRiskChallengerAnalyticsPort;
    private final LoadAdminOperationsAnalyticsPort loadAdminOperationsAnalyticsPort;
    private final LoadAdminOperationsSchoolsPort loadAdminOperationsSchoolsPort;
    private final LoadAdminOperationsPointsPort loadAdminOperationsPointsPort;
    private final LoadAdminOperationsAttendancePort loadAdminOperationsAttendancePort;
    private final LoadAdminOperationsAttendanceChaptersPort loadAdminOperationsAttendanceChaptersPort;
    private final LoadAdminOperationsAttendanceTagsPort loadAdminOperationsAttendanceTagsPort;
    private final LoadAdminOperationsAttendancePartsPort loadAdminOperationsAttendancePartsPort;
    private final LoadAdminOperationsStudyGroupsPort loadAdminOperationsStudyGroupsPort;
    private final LoadAdminOperationsSignupsPort loadAdminOperationsSignupsPort;
    private final LoadAdminGisuSummaryPort loadAdminGisuSummaryPort;
    private final LoadAdminGisuPointsPort loadAdminGisuPointsPort;
    private final LoadAdminOperationsCommunityActivityPort loadAdminOperationsCommunityActivityPort;
    private final LoadAdminStudyGroupActivityPort loadAdminStudyGroupActivityPort;
    private final LoadAdminStudyGroupListPort loadAdminStudyGroupListPort;

    @Override
    public AdminDashboardSummaryInfo getSummary(AdminDashboardQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(
            query.requesterMemberId(),
            query.gisuId(),
            query.chapterId(),
            query.schoolId(),
            null
        );
        return loadAdminDashboardAnalyticsPort.getSummary(scope);
    }

    @Override
    public AdminDashboardActionQueueInfo getActionQueue(AdminDashboardActionQueueQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminDashboardAnalyticsPort.getActionQueue(scope, query.riskThreshold());
    }

    @Override
    public AdminDashboardContextInfo getContext(Long requesterMemberId) {
        return AdminDashboardContextInfo.from(scopeResolver.resolve(requesterMemberId, null, null, null, null));
    }

    @Override
    public Page<AdminSchoolSummaryInfo> getSchoolSummaries(AdminSchoolSummaryQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(
            query.requesterMemberId(),
            query.gisuId(),
            query.chapterId(),
            null,
            null
        );
        return loadAdminSchoolAnalyticsPort.getSchoolSummaries(scope, query);
    }

    @Override
    public Page<AdminRiskChallengerInfo> getRiskChallengers(AdminRiskChallengerQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(
            query.requesterMemberId(),
            query.gisuId(),
            query.chapterId(),
            query.schoolId(),
            null
        );
        return loadAdminRiskChallengerAnalyticsPort.getRiskChallengers(scope, query);
    }

    @Override
    public AdminOperationsOverviewInfo getOperationsOverview(AdminOperationsOverviewQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsAnalyticsPort.getOperationsOverview(scope, query);
    }

    @Override
    public AdminOperationsSchoolsInfo getOperationsSchools(AdminOperationsSchoolsQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsSchoolsPort.getOperationsSchools(scope);
    }

    @Override
    public AdminOperationsPointsInfo getOperationsPoints(AdminOperationsPointsQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsPointsPort.getOperationsPoints(scope, query.from(), query.to());
    }

    @Override
    public AdminOperationsAttendanceInfo getOperationsAttendance(AdminOperationsAttendanceQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsAttendancePort.getOperationsAttendance(scope, query.from(), query.to());
    }

    @Override
    public AdminOperationsAttendanceChaptersInfo getOperationsAttendanceByChapters(AdminOperationsAttendanceChaptersQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsAttendanceChaptersPort.getAttendanceByChapters(scope, query.from(), query.to());
    }

    @Override
    public AdminOperationsAttendanceTagsInfo getOperationsAttendanceByTags(AdminOperationsAttendanceTagsQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsAttendanceTagsPort.getAttendanceByTags(scope, query.from(), query.to());
    }

    @Override
    public AdminOperationsAttendancePartsInfo getOperationsAttendanceByParts(AdminOperationsAttendancePartsQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsAttendancePartsPort.getAttendanceByParts(scope, query.from(), query.to());
    }

    @Override
    public AdminOperationsStudyGroupsInfo getOperationsStudyGroups(AdminOperationsStudyGroupsQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsStudyGroupsPort.getOperationsStudyGroups(scope, query.from(), query.to());
    }

    @Override
    public AdminOperationsSignupsInfo getOperationsSignups(AdminOperationsSignupsQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsSignupsPort.getOperationsSignups(scope, query.from(), query.to());
    }

    @Override
    public AdminGisuSummaryInfo getGisuSummary(AdminGisuSummaryQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminGisuSummaryPort.getGisuSummary(scope);
    }

    @Override
    public AdminGisuPointsInfo getGisuPoints(AdminGisuPointsQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminGisuPointsPort.getGisuPoints(scope);
    }

    @Override
    public AdminOperationsCommunityActivityInfo getCommunityActivity(AdminOperationsCommunityActivityQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminOperationsCommunityActivityPort.getCommunityActivity(scope, query.from(), query.to(), query.granularity());
    }

    @Override
    public AdminStudyGroupActivityInfo getStudyGroupActivity(AdminStudyGroupActivityQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(query.requesterMemberId(), query.gisuId(), null, null, null);
        return loadAdminStudyGroupActivityPort.getStudyGroupActivity(scope);
    }

    @Override
    public AdminStudyGroupListInfo getStudyGroupList(AdminStudyGroupListQuery query) {
        AdminAnalyticsScope scope = scopeResolver.resolve(
            query.requesterMemberId(),
            query.gisuId(),
            null,
            query.schoolId(),
            null
        );
        // 교내 운영진이 아닌 경우 schoolId 파라미터 필수
        if (scope.schoolId() == null) {
            throw new AnalyticsDomainException(AnalyticsErrorCode.SCHOOL_ID_REQUIRED);
        }
        return loadAdminStudyGroupListPort.getStudyGroupList(scope);
    }
}
