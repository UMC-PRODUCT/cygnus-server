package com.umc.product.analytics.adapter.in.web;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.analytics.adapter.in.web.dto.request.AdminDashboardActionQueueRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminDashboardRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminGisuPointsRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminGisuSummaryRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsCommunityActivityRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminStudyGroupActivityRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminStudyGroupListRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsAttendanceChaptersRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsAttendancePartsRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsAttendanceRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsAttendanceTagsRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsOverviewRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsPointsRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsSchoolsRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsSignupsRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminOperationsStudyGroupsRequest;
import com.umc.product.analytics.adapter.in.web.dto.request.AdminRiskChallengerRequest;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminDashboardActionQueueResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminDashboardContextResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminDashboardSummaryResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminGisuPointsResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminGisuSummaryResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsCommunityActivityResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminStudyGroupActivityResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminStudyGroupListResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsAttendanceChaptersResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsAttendancePartsResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsAttendanceResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsAttendanceTagsResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsOverviewResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsPointsResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsSchoolsResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsSignupsResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminOperationsStudyGroupsResponse;
import com.umc.product.analytics.adapter.in.web.dto.response.AdminRiskChallengerResponse;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardActionQueueUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardContextUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardSummaryUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminGisuPointsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminGisuSummaryUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsCommunityActivityUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminStudyGroupActivityUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminStudyGroupListUseCase;
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
import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.response.PageResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/analytics/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Analytics | 운영진 종합 대시보드", description = "운영진이 주요 운영 지표와 액션 큐를 확인합니다.")
public class AdminDashboardController {

    private final GetAdminDashboardSummaryUseCase getAdminDashboardSummaryUseCase;
    private final GetAdminDashboardActionQueueUseCase getAdminDashboardActionQueueUseCase;
    private final GetAdminDashboardContextUseCase getAdminDashboardContextUseCase;
    private final GetAdminRiskChallengerUseCase getAdminRiskChallengerUseCase;
    private final GetAdminOperationsOverviewUseCase getAdminOperationsOverviewUseCase;
    private final GetAdminOperationsSchoolsUseCase getAdminOperationsSchoolsUseCase;
    private final GetAdminOperationsPointsUseCase getAdminOperationsPointsUseCase;
    private final GetAdminOperationsAttendanceUseCase getAdminOperationsAttendanceUseCase;
    private final GetAdminOperationsAttendanceChaptersUseCase getAdminOperationsAttendanceChaptersUseCase;
    private final GetAdminOperationsAttendanceTagsUseCase getAdminOperationsAttendanceTagsUseCase;
    private final GetAdminOperationsAttendancePartsUseCase getAdminOperationsAttendancePartsUseCase;
    private final GetAdminOperationsStudyGroupsUseCase getAdminOperationsStudyGroupsUseCase;
    private final GetAdminOperationsSignupsUseCase getAdminOperationsSignupsUseCase;
    private final GetAdminGisuSummaryUseCase getAdminGisuSummaryUseCase;
    private final GetAdminGisuPointsUseCase getAdminGisuPointsUseCase;
    private final GetAdminOperationsCommunityActivityUseCase getAdminOperationsCommunityActivityUseCase;
    private final GetAdminStudyGroupActivityUseCase getAdminStudyGroupActivityUseCase;
    private final GetAdminStudyGroupListUseCase getAdminStudyGroupListUseCase;

    @Operation(operationId = "DASHBOARD-001", summary = "운영진 대시보드 요약 조회")
    @GetMapping("summary")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminDashboardSummaryResponse getSummary(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminDashboardRequest request
    ) {
        return AdminDashboardSummaryResponse.from(
            getAdminDashboardSummaryUseCase.getSummary(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-002", summary = "운영진 대시보드 액션 큐 조회")
    @GetMapping("action-queue")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminDashboardActionQueueResponse getActionQueue(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminDashboardActionQueueRequest request
    ) {
        return AdminDashboardActionQueueResponse.from(
            getAdminDashboardActionQueueUseCase.getActionQueue(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-004", summary = "운영진 대시보드 권한 컨텍스트 조회")
    @GetMapping("context")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminDashboardContextResponse getContext(
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        return AdminDashboardContextResponse.from(
            getAdminDashboardContextUseCase.getContext(memberPrincipal.getMemberId())
        );
    }

    @Operation(operationId = "DASHBOARD-005", summary = "운영 현황 집계 조회", deprecated = true)
    @Deprecated
    @GetMapping("operations")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsOverviewResponse getOperationsOverview(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsOverviewRequest request
    ) {
        return AdminOperationsOverviewResponse.from(
            getAdminOperationsOverviewUseCase.getOperationsOverview(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-006", summary = "운영 현황 - 지부별 학교/챌린저 현황 조회")
    @GetMapping("operations/schools")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsSchoolsResponse getOperationsSchools(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsSchoolsRequest request
    ) {
        return AdminOperationsSchoolsResponse.from(
            getAdminOperationsSchoolsUseCase.getOperationsSchools(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-007", summary = "운영 현황 - 지부 내 파트별 상벌점 부여 현황 조회")
    @GetMapping("operations/points")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsPointsResponse getOperationsPoints(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsPointsRequest request
    ) {
        return AdminOperationsPointsResponse.from(
            getAdminOperationsPointsUseCase.getOperationsPoints(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-008", summary = "운영 현황 - 일정 및 출석 생성 현황 조회")
    @GetMapping("operations/attendance")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsAttendanceResponse getOperationsAttendance(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsAttendanceRequest request
    ) {
        return AdminOperationsAttendanceResponse.from(
            getAdminOperationsAttendanceUseCase.getOperationsAttendance(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-011", summary = "운영 현황 - 지부별 출석률 현황 조회")
    @GetMapping("operations/attendance/chapters")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsAttendanceChaptersResponse getOperationsAttendanceByChapters(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsAttendanceChaptersRequest request
    ) {
        return AdminOperationsAttendanceChaptersResponse.from(
            getAdminOperationsAttendanceChaptersUseCase.getOperationsAttendanceByChapters(
                request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-012", summary = "운영 현황 - 일정 태그별 출석률 현황 조회")
    @GetMapping("operations/attendance/tags")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsAttendanceTagsResponse getOperationsAttendanceByTags(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsAttendanceTagsRequest request
    ) {
        return AdminOperationsAttendanceTagsResponse.from(
            getAdminOperationsAttendanceTagsUseCase.getOperationsAttendanceByTags(
                request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-013", summary = "운영 현황 - 파트별 출석률 현황 조회")
    @GetMapping("operations/attendance/parts")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsAttendancePartsResponse getOperationsAttendanceByParts(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsAttendancePartsRequest request
    ) {
        return AdminOperationsAttendancePartsResponse.from(
            getAdminOperationsAttendancePartsUseCase.getOperationsAttendanceByParts(
                request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-009", summary = "운영 현황 - 스터디 그룹 및 일정 생성 현황 조회")
    @GetMapping("operations/study-groups")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsStudyGroupsResponse getOperationsStudyGroups(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsStudyGroupsRequest request
    ) {
        return AdminOperationsStudyGroupsResponse.from(
            getAdminOperationsStudyGroupsUseCase.getOperationsStudyGroups(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-010", summary = "운영 현황 - 기간별 신규 가입자 현황 조회")
    @GetMapping("operations/signups")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsSignupsResponse getOperationsSignups(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsSignupsRequest request
    ) {
        return AdminOperationsSignupsResponse.from(
            getAdminOperationsSignupsUseCase.getOperationsSignups(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-015", summary = "기수별 지부 간 상벌점 형평성 조회")
    @GetMapping("gisu/points")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminGisuPointsResponse getGisuPoints(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminGisuPointsRequest request
    ) {
        return AdminGisuPointsResponse.from(
            getAdminGisuPointsUseCase.getGisuPoints(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-014", summary = "기수별 핵심 지표 요약 조회")
    @GetMapping("gisu/summary")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminGisuSummaryResponse getGisuSummary(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminGisuSummaryRequest request
    ) {
        return AdminGisuSummaryResponse.from(
            getAdminGisuSummaryUseCase.getGisuSummary(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-020", summary = "커뮤니티 활성도 - 게시글/댓글 주간, 월간 추이 조회")
    @GetMapping("community/activity")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminOperationsCommunityActivityResponse getCommunityActivity(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminOperationsCommunityActivityRequest request
    ) {
        return AdminOperationsCommunityActivityResponse.from(
            getAdminOperationsCommunityActivityUseCase.getCommunityActivity(
                request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-022", summary = "스터디 그룹 활성도 - 파트별 집계 조회")
    @GetMapping("study-groups/activity")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminStudyGroupActivityResponse getStudyGroupActivity(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminStudyGroupActivityRequest request
    ) {
        return AdminStudyGroupActivityResponse.from(
            getAdminStudyGroupActivityUseCase.getStudyGroupActivity(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-023", summary = "스터디 그룹 목록 - 학교별 파트 구분 조회")
    @GetMapping("study-groups/groups")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public AdminStudyGroupListResponse getStudyGroupList(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject AdminStudyGroupListRequest request
    ) {
        return AdminStudyGroupListResponse.from(
            getAdminStudyGroupListUseCase.getStudyGroupList(request.toQuery(memberPrincipal.getMemberId()))
        );
    }

    @Operation(operationId = "DASHBOARD-003", summary = "운영진 대시보드 위험군 챌린저 조회")
    @GetMapping("risk-challengers")
    @CheckAccess(resourceType = ResourceType.ANALYTICS, permission = PermissionType.READ)
    public PageResponse<AdminRiskChallengerResponse> getRiskChallengers(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject Pageable pageable,
        @ParameterObject AdminRiskChallengerRequest request
    ) {
        return PageResponse.of(
            getAdminRiskChallengerUseCase.getRiskChallengers(request.toQuery(memberPrincipal.getMemberId(), pageable)),
            AdminRiskChallengerResponse::from
        );
    }
}
