package com.umc.product.project.adapter.in.web;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.project.adapter.in.web.dto.request.CreateProjectMatchingRoundRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdateProjectMatchingRoundRequest;
import com.umc.product.project.adapter.in.web.dto.response.ProjectMatchingRoundCreateResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectMatchingRoundResponse;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding;
import com.umc.product.project.application.port.in.command.AutoDecideProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.AutoDecisionActor;
import com.umc.product.project.application.port.in.command.CreateProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.DeleteProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectMatchingRoundUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMatchingRoundUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/project/matching-rounds")
@RequiredArgsConstructor
@Tag(name = "Project | 매칭 차수", description = "프로젝트 매칭 차수를 조회하고 관리합니다.")
public class ProjectMatchingRoundController {

    private final GetProjectMatchingRoundUseCase getProjectMatchingRoundUseCase;
    private final CreateProjectMatchingRoundUseCase createProjectMatchingRoundUseCase;
    private final UpdateProjectMatchingRoundUseCase updateProjectMatchingRoundUseCase;
    private final DeleteProjectMatchingRoundUseCase deleteProjectMatchingRoundUseCase;
    private final AutoDecideProjectMatchingRoundUseCase autoDecideProjectMatchingRoundUseCase;

    @GetMapping
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_MATCHING_LIST)
    @Operation(
        operationId = "PROJECT-MATCHING-001",
        summary = "매칭 차수 목록 조회",
        description = """
            매칭 차수 목록을 조회합니다.
            - gisuId와 chapterId가 있으면 각 조건을 AND로 적용합니다.
            - time이 있으면 해당 시점에 지원 가능한 매칭 차수(startsAt <= time <= endsAt)만 반환합니다.
            - 모든 필터는 선택 사항이며, 필터가 없으면 전체 매칭 차수를 startsAt 오름차순으로 반환합니다.
            """
    )
    public List<ProjectMatchingRoundResponse> list(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam(required = false) Long gisuId,
        @RequestParam(required = false) Long chapterId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant time
    ) {
        return getProjectMatchingRoundUseCase.list(memberPrincipal.getMemberId(), gisuId, chapterId, time).stream()
            .map(ProjectMatchingRoundResponse::from)
            .toList();
    }

    @PostMapping
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_MATCHING_CREATE)
    @Operation(
        operationId = "PROJECT-MATCHING-101",
        summary = "매칭 차수 생성",
        description = """
            매칭 차수를 생성합니다.
            - gisuId는 필수이며 생성 후 변경할 수 없습니다.
            - chapterId는 gisuId에 속해야 합니다.
            - 중앙운영사무국 총괄단 이상은 모든 지부에 생성할 수 있습니다.
            - 지부장은 본인 지부에만 생성할 수 있습니다.
            - gisu.startAt <= startsAt < endsAt < decisionDeadline < gisu.endAt 순서를 만족해야 합니다.
            - 같은 지부 내 기존 매칭 차수와 startsAt ~ decisionDeadline 기간이 중복되면 409를 반환합니다.
            - 동일 지부 내 type + phase 조합은 DB Unique Key로도 중복을 제한합니다.
            """
    )
    public ProjectMatchingRoundCreateResponse create(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateProjectMatchingRoundRequest request
    ) {
        Long matchingRoundId = createProjectMatchingRoundUseCase.create(
            request.toCreateCommand(memberPrincipal.getMemberId()));
        return ProjectMatchingRoundCreateResponse.from(matchingRoundId);
    }

    @PatchMapping("/{matchingRoundId}")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_MATCHING_UPDATE)
    @Operation(
        operationId = "PROJECT-MATCHING-102",
        summary = "매칭 차수 수정",
        description = """
            매칭 차수 정보를 부분 수정합니다.
            - 중앙운영사무국 총괄단 이상은 모든 지부의 매칭 차수를 수정할 수 있습니다.
            - 지부장은 본인 지부의 매칭 차수만 수정할 수 있습니다.
            - 수정 요청 본문에는 gisuId와 chapterId를 포함할 수 없습니다.
            - 매칭 차수가 소속된 gisuId와 chapterId는 수정할 수 없습니다.
            - 요청 본문에 제공되지 않은 필드는 기존 매칭 차수 값을 유지합니다.
            - 기존 값과 요청 값을 병합한 최종 결과가 기수 기간 안에 있어야 합니다.
            - 같은 지부 내 다른 매칭 차수와 startsAt ~ decisionDeadline 기간이 중복되면 409를 반환합니다.
            """
    )
    public void update(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long matchingRoundId,
        @Valid @RequestBody UpdateProjectMatchingRoundRequest request
    ) {
        updateProjectMatchingRoundUseCase.update(
            request.toCommand(matchingRoundId, memberPrincipal.getMemberId()));
    }

    @DeleteMapping("/{matchingRoundId}")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_MATCHING_DELETE)
    @Operation(
        operationId = "PROJECT-MATCHING-103",
        summary = "매칭 차수 삭제",
        description = """
            매칭 차수를 삭제합니다.
            - 중앙운영사무국 총괄단 이상은 모든 지부의 매칭 차수를 삭제할 수 있습니다.
            - 지부장은 본인 지부의 매칭 차수만 삭제할 수 있습니다.
            - 해당 매칭 차수를 참조하는 지원서가 하나라도 있으면 삭제할 수 없으며 409를 반환합니다.
            """
    )
    public void delete(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long matchingRoundId
    ) {
        deleteProjectMatchingRoundUseCase.delete(matchingRoundId, memberPrincipal.getMemberId());
    }

    @PostMapping("/{matchingRoundId}/auto-decide")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_MATCHING_HUMAN_AUTO_DECIDE)
    @Operation(
        operationId = "PROJECT-MATCHING-201",
        summary = "매칭 차수 자동 선발 수동 실행",
        description = """
            결정 마감(decisionDeadline) 이후 매칭 차수의 자동 선발을 실행합니다.
            - 중앙운영사무국 총괄단 이상은 모든 지부의 매칭 차수에서 자동 선발을 실행할 수 있습니다.
            - 지부장은 본인 지부의 매칭 차수에서만 자동 선발을 실행할 수 있습니다.
            - 결정 마감 시각이 지나기 전에는 실행할 수 없으며 400을 반환합니다.
            - 이미 자동 선발이 실행된 매칭 차수에 대해서는 멱등 (no-op).
            - 정책 매트릭스(디자이너 / 개발자)에 따라 SUBMITTED 우선 random 보충 후 부족하면 REJECTED 까지 override 합니다.
            - 합격자에게는 ProjectMember 가 자동으로 생성됩니다.
            """
    )
    public void autoDecide(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long matchingRoundId
    ) {
        autoDecideProjectMatchingRoundUseCase.autoDecide(
            matchingRoundId,
            new AutoDecisionActor.Member(memberPrincipal.getMemberId())
        );
    }
}
