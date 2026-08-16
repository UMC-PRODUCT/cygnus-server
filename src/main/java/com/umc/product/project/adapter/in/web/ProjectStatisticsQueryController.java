package com.umc.product.project.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.project.adapter.in.web.assembler.ProjectResponseAssembler;
import com.umc.product.project.adapter.in.web.dto.response.statistics.ChapterProjectMatchingStatisticsResponse;
import com.umc.product.project.adapter.in.web.dto.response.statistics.ChapterProjectStatisticsResponse;
import com.umc.product.project.adapter.in.web.dto.response.statistics.ProjectStatisticsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project | 프로젝트 통계 Query", description = "프로젝트 지원·매칭 현황 조회")
public class ProjectStatisticsQueryController {

    private final ProjectResponseAssembler assembler;

    @GetMapping("/{projectId}/statistics")
    @Operation(
        operationId = "PROJECT-STAT-001",
        summary = "단건 프로젝트 지원/매칭 현황 조회 (Deprecated)",
        deprecated = true,
        description = """
            deprecated: `/api/v1/projects/statistics?projectIds={projectId}`를 사용해주세요.

            프로젝트 ID와 함께 멤버 목록을 포함하고 있고, FE단 재가공을 최소화해드리기 위해서 `roundApplicationStatistics` 및 `schoolApplicationStatistics` 필드를 두고 있습니다.

            각 항목이 매칭 차수별 지원률, N차 매칭에서의 학교별 및 총 지원자 수를 포함하고 있습니다.
            프로젝트 멤버 목록의 경우 각 멤버가 해당 프로젝트에 작성한 지원 이력을 포함하고 있으며,
            없거나 (강제배정/랜덤매칭) 여러 건 (떨어지고 재 지원하는 경우) 이 존재할 수 있어 배열로 구성되어 있습니다.

            지원자 목록은(특정 프로젝트에 대한 지원서 조회) `/api/v1/projects/{projectId}/applications`를 호출하셔서 활용하셔야 합니다.

            권한: 해당 프로젝트의 PO/Sub-PM(본인 프로젝트), 총괄단, 해당 지부장, 해당 지부 소속 학교 회장/부회장만 조회할 수 있습니다. 그 외에는 403.
            """
    )
    @Deprecated(since = "v2.0.0", forRemoval = true)
    public ProjectStatisticsResponse getProjectStatistics(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId
    ) {
        return assembler.statisticsForProject(projectId, memberPrincipal.getMemberId());
    }

    @GetMapping("/statistics")
    @Operation(
        operationId = "PROJECT-STAT-002",
        summary = "프로젝트 지원 현황 조회",
        description = """
            프로젝트 지원 현황 조회 API입니다.
            projectIds 또는 chapterId 중 정확히 하나만 query parameter로 제공해야 합니다.

            - projectIds 제공: 요청한 프로젝트의 지원 현황을 `projects` 배열로 반환합니다. 단건은 1개 값으로 요청하고, 다건은 같은 지부 프로젝트만 요청할 수 있습니다.
            - chapterId 제공: 지부 전체 프로젝트 지원 현황을 반환합니다.

            지부 내 모든 프로젝트 목록 및 각 프로젝트에 대한 프로젝트 멤버를 포함하며,
            이는 `/api/v1/projects/{projectId}/statistics` 에서 제공하는 것과 동일한 형태입니다.
            추가로 BFF 패턴을 적용하여 FE단 데이터 가공 책임을 줄이기 위해 `summary` 필드를 제공합니다.

            응답 필드 설명:
            - projects: 프로젝트별 지원 현황 목록입니다.
            - projects[].projectMembers: 해당 프로젝트에 최종 합류한 ACTIVE ProjectMember 목록입니다.
            - projects[].projectMembers[].applications: 해당 멤버가 해당 프로젝트에 작성한 지원 이력입니다. 강제 배정처럼 지원서 없이 합류한 경우 빈 배열입니다.
            - projects[].roundApplicationStatistics: 프로젝트 단위의 매칭 차수별 지원서 수와 지원 가능 인원 수입니다.
            - projects[].schoolApplicationStatistics: 프로젝트 단위의 매칭 차수별 학교 지원자 수입니다. 같은 차수 안에서는 memberId 기준으로 중복 제거합니다.
            - summary.roundApplicationStatistics: 조회 범위 전체의 매칭 차수별 지원서 수와 지원 가능 인원 수입니다. `appliedMemberCount`는 지원서 수 합계이고, `availableMemberCount`는 지원 가능 챌린저 총원에서 이전 차수까지 이미 ProjectMember로 합류한 인원을 제외한 값입니다.
            - summary.roundSchoolRankings: 조회 범위 전체의 매칭 차수별 학교 지원자 수입니다. 같은 차수 안에서는 memberId 기준으로 중복 제거합니다.
            - summary.schoolMatchingStatistics: 학교별 지원 가능 총원, 매칭 완료 인원, 한 번이라도 지원한 인원 수입니다. `totalMemberCount`는 지원 가능한 ACTIVE 챌린저 수, `matchedMemberCount`는 ACTIVE ProjectMember 수, `appliedMemberCount`는 조회 범위에서 한 번이라도 지원한 memberId 기준 unique 인원 수입니다.
            - summary.projectRoundStatistics: 프로젝트별 매칭 차수 지원 현황입니다. `appliedMemberCount`는 프로젝트와 차수 안에서 memberId 기준으로 중복 제거한 지원자 수이고, `matchedMemberCount`는 해당 프로젝트와 차수에서 매칭 완료된 인원 수입니다.

            프로젝트별 지원자 목록은 `/api/v1/projects/{projectId}/applications`를 호출해서 조회해야 합니다.
            공개 매칭 요약은 `/api/v1/projects/statistics/matchings`를 호출해서 조회해야 합니다.

            권한: projectIds 조회는 각 프로젝트의 PO/Sub-PM(본인 프로젝트), 총괄단, 해당 지부장, 해당 지부 소속 학교 회장/부회장만 조회할 수 있습니다.
            chapterId 조회는 총괄단(모든 지부), 해당 지부장, 해당 지부 소속 학교 회장/부회장만 조회할 수 있습니다. 그 외에는 403.
            """
    )
    public ChapterProjectStatisticsResponse getStatistics(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "프로젝트 ID 목록. chapterId와 동시에 제공할 수 없습니다. 단건 조회는 값 1개로 요청합니다.")
        @RequestParam(required = false) List<Long> projectIds,
        @Parameter(description = "지부 ID. projectIds와 동시에 제공할 수 없습니다.")
        @RequestParam(required = false) Long chapterId
    ) {
        validateSingleStatisticsTarget(projectIds, chapterId);
        if (projectIds != null) {
            return assembler.statisticsForProjects(projectIds, memberPrincipal.getMemberId());
        }
        return assembler.statisticsForChapter(chapterId, memberPrincipal.getMemberId());
    }

    @GetMapping("/statistics/matchings")
    @Operation(
        operationId = "PROJECT-STAT-003",
        summary = "지부 공개 프로젝트 매칭 요약 조회",
        description = """
            로그인 사용자 누구나 조회할 수 있는 지부 내 공개 프로젝트 매칭 요약입니다.
            멤버 ID와 지원서 ID는 노출하지 않고 ProjectMember 기준 집계 숫자만 반환합니다.

            집계 대상 프로젝트는 공개 프로젝트 목록과 동일하게 IN_PROGRESS, COMPLETED 상태만 포함합니다.
            차수 귀속은 각 ProjectMember가 해당 프로젝트에 작성한 APPROVED 지원서 중 가장 오래된 매칭 차수를 사용합니다.
            합격 지원서가 없으면 차수별 집계에서 제외하고 unclassifiedMatchingStatistics에 별도 집계합니다.
            """
    )
    public ChapterProjectMatchingStatisticsResponse getPublicMatchingStatistics(
        @Parameter(description = "지부 ID", required = true) @RequestParam Long chapterId
    ) {
        return assembler.matchingStatisticsForChapter(chapterId);
    }

    private void validateSingleStatisticsTarget(List<Long> projectIds, Long chapterId) {
        boolean hasProjectIds = projectIds != null && !projectIds.isEmpty();
        boolean hasChapterId = chapterId != null;
        int targetCount = (hasProjectIds ? 1 : 0) + (hasChapterId ? 1 : 0);

        if (targetCount != 1) {
            throw new CommonException(
                CommonErrorCode.BAD_REQUEST,
                "projectIds 또는 chapterId 중 하나만 제공해주세요."
            );
        }
    }
}
