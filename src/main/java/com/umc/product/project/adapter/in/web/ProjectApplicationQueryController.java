package com.umc.product.project.adapter.in.web;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.project.adapter.in.web.assembler.ProjectApplicationResponseAssembler;
import com.umc.product.project.adapter.in.web.dto.response.MyProjectApplicationResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectApplicantResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectApplicationDetailResponse;
import com.umc.product.project.application.port.in.query.dto.GetMyProjectApplicationsQuery;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsBatchQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectApplicationsQuery;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project | 프로젝트 지원서 Query", description = "프로젝트 지원서, 지원자, 내 지원 내역을 조회합니다.")
public class ProjectApplicationQueryController {

    private final ProjectApplicationResponseAssembler assembler;

    @GetMapping("/me/applications")
    @Operation(
        operationId = "APPLY-004",
        summary = "본인 지원 내역 목록 조회",
        description = """
            요청자의 챌린저 파트에 맞는 매칭 종류로 본인 지원 내역을 조회합니다.
            <p>
            응답에는 다음 카드가 포함될 수 있습니다.
            <ul>
              <li>지원서 카드: 본인이 제출한 지원서입니다.
                  `matchingRound.id`, `matchingRound.type`, `matchingRound.phase` 에 실제 매칭 차수 정보를 담습니다.</li>
              <li>랜덤 매칭 카드: 지원서 없이 프로젝트 멤버로 확정된 경우입니다.
                  `matchingRound.id` 는 `null`, `matchingRound.phase` 는 `RANDOM_MATCHING`,
                  `status` 는 `APPROVED` 입니다. 한 기수에서 최대 1건입니다.</li>
            </ul>
            <p>
            정렬은 매칭 차수 시작일 오름차순, 지원서 갱신일 내림차순입니다. 랜덤 매칭 카드는 목록 마지막에 둡니다.
            <p>
            `status` 파라미터 :
            <ul>
              <li>보내지 않으면 임시저장(DRAFT)을 제외한 지원서와 랜덤 매칭 카드를 함께 조회합니다.</li>
              <li>보내면 해당 상태를 확인할 수 있는 지원서만 조회합니다.
                  제출 이후 상태는 해당 매칭 차수의 `decisionDeadline` 이 지난 뒤 확인할 수 있습니다.
                  랜덤 매칭 카드는 포함하지 않습니다.</li>
            </ul>
            <p>
            요청자가 해당 기수 챌린저가 아니거나 지원 대상 파트가 아니면 빈 목록을 반환합니다.
            """
    )
    public List<MyProjectApplicationResponse> getMyApplications(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam Long gisuId,
        @Parameter(description = """
            지원 상태 필터입니다. 보내지 않으면 임시저장(DRAFT)을 제외한 지원서와 랜덤 매칭 카드를 조회합니다.
            허용 값: DRAFT / SUBMITTED / APPROVED / REJECTED.
            """)
        @RequestParam(required = false) ProjectApplicationStatus status
    ) {
        GetMyProjectApplicationsQuery query = GetMyProjectApplicationsQuery.builder()
            .requesterMemberId(memberPrincipal.getMemberId())
            .gisuId(gisuId)
            .status(status)
            .build();

        return assembler.myApplicationsFor(query);
    }

    @GetMapping("/applications")
    @Operation(
        operationId = "APPLY-101-BATCH",
        summary = "PM/운영진 복수 프로젝트 지원자 목록 조회",
        description = """
            복수 프로젝트의 제출된 지원자 목록을 프로젝트별로 조회합니다. 임시저장(DRAFT) 지원서는 포함하지 않습니다.
            권한이 없거나 존재하지 않는 프로젝트는 해당 projectId 의 값을 빈 목록으로 반환합니다.
            <p>
            필터:
            <ul>
              <li>projectIds: 프로젝트 ID 목록. 최대 100개까지 조회할 수 있습니다.</li>
              <li>matchingRoundId: 매칭 차수 ID</li>
              <li>part: 지원자(챌린저) 파트</li>
              <li>status: 지원 상태. SUBMITTED, APPROVED, REJECTED 만 사용할 수 있습니다.
                  DRAFT 를 보내면 APPLICATION_DRAFT_FILTER_NOT_ALLOWED 오류를 반환합니다.</li>
            </ul>
            """
    )
    public Map<Long, List<ProjectApplicantResponse>> getProjectApplicantsBatch(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam
        @NotEmpty(message = "projectIds는 비어 있을 수 없습니다.") @Size(max = 100, message = "projectIds는 최대 100개까지 조회할 수 있습니다.") List<@NotNull Long> projectIds,
        @RequestParam(required = false) Long matchingRoundId,
        @RequestParam(required = false) ChallengerPart part,
        @Parameter(description = """
            지원 상태 필터입니다. DRAFT 는 지원자 목록에서 사용할 수 없습니다.
            허용 값: SUBMITTED / APPROVED / REJECTED.
            """)
        @RequestParam(required = false) ProjectApplicationStatus status
    ) {
        List<Long> deduplicatedProjectIds = new ArrayList<>(new LinkedHashSet<>(projectIds));
        SearchProjectApplicationsBatchQuery query = SearchProjectApplicationsBatchQuery.builder()
            .requesterMemberId(memberPrincipal.getMemberId())
            .projectIds(deduplicatedProjectIds)
            .matchingRoundId(matchingRoundId)
            .part(part)
            .status(status)
            .build();

        return assembler.applicantsForBatch(query);
    }

    @GetMapping("/{projectId}/applications")
    @Operation(
        operationId = "APPLY-101",
        summary = "PM/운영진 단일 프로젝트 지원자 목록 조회",
        description = """
            단일 프로젝트의 제출된 지원자 목록을 조회합니다. 임시저장(DRAFT) 지원서는 포함하지 않습니다.

            정렬은 매칭 차수, 지원 파트, 제출 시각 순입니다. 같은 차수와 파트 안에서는 먼저 제출한 지원서를 먼저 보여줍니다.
            <p>
            필터:
            <ul>
              <li>matchingRoundId: 매칭 차수 ID</li>
              <li>part: 지원자(챌린저) 파트</li>
              <li>status: 지원 상태. SUBMITTED, APPROVED, REJECTED 만 사용할 수 있습니다.
                  DRAFT 를 보내면 APPLICATION_DRAFT_FILTER_NOT_ALLOWED 오류를 반환합니다.</li>
            </ul>
            <p>
            다음 권한이 있으면 조회할 수 있습니다. 권한이 없으면 빈 목록을 반환합니다.
            <ul>
              <li>해당 프로젝트의 PO</li>
              <li>해당 프로젝트의 보조 PM (ACTIVE PLAN 멤버)</li>
              <li>SUPER_ADMIN</li>
              <li>해당 프로젝트 기수의 Central Core (총괄/부총괄)</li>
              <li>해당 프로젝트 지부의 지부장 (같은 기수)</li>
              <li>해당 프로젝트 지부에 속한 학교 회장단 (SCHOOL_PRESIDENT/SCHOOL_VICE_PRESIDENT, 같은 기수)</li>
            </ul>
            """
    )
    public List<ProjectApplicantResponse> getProjectApplicants(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @RequestParam(required = false) Long matchingRoundId,
        @RequestParam(required = false) ChallengerPart part,
        @Parameter(description = """
            지원 상태 필터입니다. DRAFT 는 지원자 목록에서 사용할 수 없습니다.
            허용 값: SUBMITTED / APPROVED / REJECTED.
            """)
        @RequestParam(required = false) ProjectApplicationStatus status
    ) {
        SearchProjectApplicationsQuery query = SearchProjectApplicationsQuery.builder()
            .requesterMemberId(memberPrincipal.getMemberId())
            .projectId(projectId)
            .matchingRoundId(matchingRoundId)
            .part(part)
            .status(status)
            .build();

        return assembler.applicantsFor(query);
    }

    @CheckAccess(
        resourceType = ResourceType.PROJECT_APPLICATION,
        resourceId = "#applicationId",
        permission = PermissionType.READ,
        message = "지원서 상세는 지원자 본인, PO, Sub-PO, 해당 기수 Central Core, 해당 기수 지부장만 볼 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @GetMapping("/{projectId}/applications/{applicationId}")
    @Operation(
        operationId = "APPLY-102",
        summary = "지원서 단건 상세 조회",
        description = """
            지원자, 매칭 차수, 상태, 제출/처리 시각, 지원 폼, 답변, 첨부 파일을 함께 조회합니다.
            지원자 본인이 조회할 때 해당 매칭 차수의 `decisionDeadline` 전이면 제출 이후 상태의 `status` 는 `null` 입니다.
            `decisionDeadline` 이 지나면 상태와 무관하게 `status` 를 반환합니다.
            지원서 내용과 답변은 그대로 포함합니다.
            <p>
            조회 권한:
            <ul>
              <li>SUBMITTED/APPROVED/REJECTED: 다음 호출자가 조회할 수 있습니다.
                <ul>
                  <li>지원자 본인</li>
                  <li>해당 프로젝트의 PO</li>
                  <li>해당 프로젝트의 Sub-PO (ACTIVE PLAN 멤버)</li>
                  <li>해당 프로젝트 기수의 Central Core (총괄/부총괄/SUPER_ADMIN)</li>
                  <li>해당 프로젝트 지부의 지부장 (같은 기수)</li>
                </ul>
              </li>
              <li>DRAFT(임시저장): 지원자 본인만 조회할 수 있습니다.</li>
            </ul>
            권한이 없으면 403(AUTHORIZATION-0002)을 반환합니다.
            <p>
            경로의 `projectId` 와 지원서의 프로젝트가 다르면 404(PROJECT-0021)를 반환합니다.
            """
    )
    public ProjectApplicationDetailResponse getApplicationDetail(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @PathVariable Long applicationId
    ) {
        GetProjectApplicationDetailQuery query = GetProjectApplicationDetailQuery.builder()
            .projectId(projectId)
            .applicationId(applicationId)
            .requesterMemberId(memberPrincipal.getMemberId())
            .build();

        return assembler.detailFor(query);
    }
}
