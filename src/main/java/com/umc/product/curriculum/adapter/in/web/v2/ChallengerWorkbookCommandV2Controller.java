package com.umc.product.curriculum.adapter.in.web.v2;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateBestWorkbookRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditChallengerWorkbookRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditWeeklyBestWorkbookReasonRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.ExcuseChallengerWorkbookRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.ChallengerWorkbookResponse;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.ChallengerWorkbookStatusResponse;
import com.umc.product.curriculum.application.port.in.command.ManageChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyBestWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeleteChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeployChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/curriculums/challenger-workbooks")
@RequiredArgsConstructor
@Tag(name = "Curriculum V2 | Challenger Workbook Command", description = "챌린저 워크북 배포, 수정, 베스트 워크북 지정을 다룹니다.")
public class ChallengerWorkbookCommandV2Controller {

    private final ManageChallengerWorkbookUseCase manageChallengerWorkbookUseCase;
    private final ManageWeeklyBestWorkbookUseCase manageWeeklyBestWorkbookUseCase;

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-001",
        summary = "챌린저용: 특정 원본 워크북 배포 요청",
        description = """
            커리큘럼 조회를 통해 받은 OriginalWorkbookId를 통해서 배포 요청을 합니다.

            여러 개의 원본 워크북을 한 번에 배포받는 경우
            (e.g. 각 주차에 있는 모든 원본 워크북을 배포받기)를 지원합니다.

            단, 제공받은 OriginalWorkbookId 중 한 개라도 배포가 불가능한 상태이거나 권한이 존재하지 않는 경우에는 전체 요청이 실패합니다.

            #### 배포 가능한 조건은 아래와 같습니다.
            - 원본 워크북이 배포 가능한 상태알 것
            - 배포를 요청한 사용자가 해당 기수에 유효한 챌린저일 것
            - 챌린저가 속한 스터디 그룹이 원본 워크북의 파트와 일치할 것

            > BFF 패턴을 적용하여, 생성된 ChallengerWorkbook에 대한 정보를 반환합니다.
            """
    )
    @PostMapping("/deploy")
    public List<ChallengerWorkbookResponse> requestSingleChallengerWorkbookDeploy(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam List<Long> originalWorkbookIds
    ) {
        return manageChallengerWorkbookUseCase.batchDeploy(DeployChallengerWorkbookCommand.builder()
                .originalWorkbookIds(originalWorkbookIds)
                .requestedMemberId(memberPrincipal.getMemberId())
                .build())
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-002",
        summary = "챌린저 워크북 수정",
        description = """
            자신의 워크북의 내용을 수정합니다.

            *근데 아마도 쓸 일 없을거에요.*
            """
    )
    @PatchMapping("/{challengerWorkbookId}")
    public void editChallengerWorkbook(
        @PathVariable Long challengerWorkbookId,
        @Valid @RequestBody EditChallengerWorkbookRequest request,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        manageChallengerWorkbookUseCase.edit(request.toCommand(challengerWorkbookId, memberPrincipal.getMemberId()));
    }

    // ============== 운영진용 ==============

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-003",
        summary = "챌린저 워크북 삭제",
        description = """
            부정적인 방법으로 배포된 챌린저 워크북을 강제로 삭제합니다.
            챌린저는 본인 워크북이라도 삭제할 수 없습니다.

            **중요**: 챌린저 워크북이 삭제된 이후, 해당 워크북과 연관된 모든 미션 제출 기록 및 피드백도 함께 삭제됩니다.
            따라서, 챌린저가 해당 워크북에 대해 제출했던 모든 미션 기록이 사라지게 됩니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "#challengerWorkbookId",
        permission = PermissionType.DELETE
    )
    @DeleteMapping("/{challengerWorkbookId}")
    public void deleteChallengerWorkbook(
        @PathVariable Long challengerWorkbookId,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        manageChallengerWorkbookUseCase.delete(DeleteChallengerWorkbookCommand.builder()
            .challengerWorkbookId(challengerWorkbookId)
            .requestedMemberId(memberPrincipal.getMemberId())
            .build());
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-004",
        summary = "회장단 워크북 인정",
        description = """
            특정 워크북에 대해 미션을 제출하지 않아도 벌점이 부과되지 않도록 인정 처리합니다.
            인정 처리에 대한 철회는 제공하지 않습니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "#challengerWorkbookId",
        permission = PermissionType.WRITE
    )
    @PostMapping("/{challengerWorkbookId}/excuse")
    public void excuseChallengerWorkbook(
        @PathVariable Long challengerWorkbookId,
        @Valid @RequestBody ExcuseChallengerWorkbookRequest request,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        manageChallengerWorkbookUseCase.excuse(request.toCommand(challengerWorkbookId, memberPrincipal.getMemberId()));
    }

    // TODO: 언젠가 시간이 남는다면 인정 처리 철회를 만드시길,,, 근데 그러면 Rollback Policy 모두 정하고 하셔야 해요!

    // ============== 운영진용: 베스트 워크북 처리 ==============

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-005",
        summary = "베스트 워크북 선정",
        description = """
            스터디 그룹에서 특정 주차의 베스트 워크북을 선정합니다.

            - 주차별로 스터디 그룹 당 한 명만 지정이 가능합니다.
                - 이미 베스트 워크북으로 선정된 사람이 있는 경우 요청은 실패합니다.
            - 해당 주차의 워크북 중 하나라도 EXCUSED 처리가 되어 있는 사용자는 베스트 워크북으로 선정되지 못합니다.
            - 해당 추자의 워크북에 대한 모든 미션은 (선택 미션 제외) PASS 처리가 되어 있어야 합니다.
            - 스터디 그룹에 다중 파트장이 있는 경우, 모든 사람에게 선정 권한이 있습니다.
                - 단, 사유 란은 하나이므로 N명이 사유를 작성하고자 하는 경우 해당 란에 같이 작성해주세요.
            - 다른 사람을 베스트 워크북으로 선정하고자 하는 경우, 철회 후 다시 선정해야 합니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "#request.weeklyCurriculumId",
        permission = PermissionType.WRITE
    )
    @PostMapping("/weekly-best")
    public void createWeeklyBestWorkbook(
        @Valid @RequestBody CreateBestWorkbookRequest request,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        manageWeeklyBestWorkbookUseCase.selectBest(request.toCommand(memberPrincipal.getMemberId()));
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-006",
        summary = "베스트 워크북 선정 사유 수정",
        description = """
            베스트 워크북으로 선정한 사유를 수정합니다.

            **중요**: Path Param의 weeklyBestWorkbookId는 WeeklyBestWorkbook Entity의 PK 값입니다.

            `BestWorkbookResponse`에서 제공되는 `weeklyBestWorkbookEntityId`를 제공하여야 합니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "#weeklyBestWorkbookId",
        permission = PermissionType.WRITE
    )
    @PatchMapping("/weekly-best/{weeklyBestWorkbookId}")
    public void editWeeklyBestWorkbookReason(
        @PathVariable Long weeklyBestWorkbookId,
        @Valid @RequestBody EditWeeklyBestWorkbookReasonRequest request,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        manageWeeklyBestWorkbookUseCase.editReason(
            request.toCommand(weeklyBestWorkbookId, memberPrincipal.getMemberId())
        );
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-007",
        summary = "베스트 워크북 선정 철회",
        description = """
            베스트 워크북 선정을 철회합니다.
            해당 주차가 종료된 이후 1주일 뒤까지만 철회가 가능합니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "#weeklyBestWorkbookId",
        permission = PermissionType.DELETE
    )
    @DeleteMapping("/weekly-best/{weeklyBestWorkbookId}")
    public void deleteWeeklyBestWorkbook(
        @PathVariable Long weeklyBestWorkbookId,
        @CurrentMember MemberPrincipal memberPrincipal
    ) {
        manageWeeklyBestWorkbookUseCase.withdraw(weeklyBestWorkbookId);
    }

    private ChallengerWorkbookResponse toResponse(ChallengerWorkbookInfo info) {
        return ChallengerWorkbookResponse.builder()
            .challengerWorkbookId(info.challengerWorkbookId())
            .originalWorkbookId(info.originalWorkbookId())
            .receivedStudyGroupId(info.receivedStudyGroupId())
            .memberId(info.challengerId())
            .isExcused(info.isExcused())
            .excusedReason(info.excusedReason())
            .content(info.content())
            .isBestWorkbook(info.isBestWorkbook())
            .status(ChallengerWorkbookStatusResponse.IN_PROGRESS)
            .hasSubmission(false)
            .submission(null)
            .build();
    }

}
