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
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.ChallengerWorkbookResponse;
import com.umc.product.curriculum.application.port.in.command.ManageChallengerWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyBestWorkbookUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeleteChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.DeployChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditChallengerWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.EditWeeklyBestWorkbookCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.ExcuseChallengerWorkbookCommand;
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
        summary = "챌린저용: 원본 워크북 배포 요청",
        description = "활성 챌린저의 기수·파트와 일치하는 스터디 그룹을 확인한 뒤 배포합니다."
    )
    @PostMapping("/deploy")
    public List<ChallengerWorkbookResponse> deploy(
        @CurrentMember MemberPrincipal principal,
        @RequestParam List<Long> originalWorkbookIds
    ) {
        return manageChallengerWorkbookUseCase.batchDeploy(DeployChallengerWorkbookCommand.builder()
                .originalWorkbookIds(originalWorkbookIds)
                .requestedMemberId(principal.getMemberId())
                .build())
            .stream()
            .map(ChallengerWorkbookResponse::from)
            .toList();
    }

    @Operation(operationId = "CHALLENGER-WORKBOOK-002", summary = "챌린저 워크북 수정")
    @PatchMapping("/{challengerWorkbookId}")
    public void edit(
        @PathVariable Long challengerWorkbookId,
        @RequestBody String content,
        @CurrentMember MemberPrincipal principal
    ) {
        manageChallengerWorkbookUseCase.edit(EditChallengerWorkbookCommand.builder()
            .challengerWorkbookId(challengerWorkbookId)
            .requestedMemberId(principal.getMemberId())
            .content(content)
            .build());
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-003",
        summary = "운영진용: 챌린저 워크북 삭제",
        description = "관련 미션 제출 또는 피드백이 존재하면 삭제하지 않고 409 Conflict를 반환합니다."
    )
    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_WORKBOOK,
        resourceId = "#challengerWorkbookId",
        permission = PermissionType.DELETE
    )
    @DeleteMapping("/{challengerWorkbookId}")
    public void delete(
        @PathVariable Long challengerWorkbookId,
        @RequestBody(required = false) String reason,
        @CurrentMember MemberPrincipal principal
    ) {
        manageChallengerWorkbookUseCase.delete(DeleteChallengerWorkbookCommand.builder()
            .challengerWorkbookId(challengerWorkbookId)
            .requestedMemberId(principal.getMemberId())
            .build());
    }

    @Operation(operationId = "CHALLENGER-WORKBOOK-004", summary = "운영진용: 워크북 인정 처리")
    @CheckAccess(
        resourceType = ResourceType.CHALLENGER_WORKBOOK,
        resourceId = "#challengerWorkbookId",
        permission = PermissionType.WRITE
    )
    @PostMapping("/{challengerWorkbookId}/excuse")
    public void excuse(
        @PathVariable Long challengerWorkbookId,
        @RequestBody String reason,
        @CurrentMember MemberPrincipal principal
    ) {
        manageChallengerWorkbookUseCase.excuse(ExcuseChallengerWorkbookCommand.builder()
            .challengerWorkbookId(challengerWorkbookId)
            .excuseApprovedMemberId(principal.getMemberId())
            .reason(reason)
            .build());
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-005",
        summary = "운영진용: 베스트 워크북 선정",
        description = "그룹·주차당 한 명만 선정하며 모든 필수 미션에 PASS 피드백이 있어야 합니다."
    )
    @CheckAccess(
        resourceType = ResourceType.WEEKLY_BEST_WORKBOOK,
        resourceId = "#request.studyGroupId",
        permission = PermissionType.WRITE
    )
    @PostMapping("/weekly-best")
    public void selectBest(
        @Valid @RequestBody CreateBestWorkbookRequest request,
        @CurrentMember MemberPrincipal principal
    ) {
        manageWeeklyBestWorkbookUseCase.selectBest(request.toCommand(principal.getMemberId()));
    }

    @Operation(operationId = "CHALLENGER-WORKBOOK-006", summary = "운영진용: 베스트 워크북 선정 사유 수정")
    @CheckAccess(
        resourceType = ResourceType.WEEKLY_BEST_WORKBOOK,
        resourceId = "#weeklyBestWorkbookId",
        permission = PermissionType.EDIT
    )
    @PatchMapping("/weekly-best/{weeklyBestWorkbookId}")
    public void editBestReason(
        @PathVariable Long weeklyBestWorkbookId,
        @RequestBody String newReason,
        @CurrentMember MemberPrincipal principal
    ) {
        manageWeeklyBestWorkbookUseCase.editReason(EditWeeklyBestWorkbookCommand.builder()
            .weeklyBestWorkbookId(weeklyBestWorkbookId)
            .requestedMemberId(principal.getMemberId())
            .newReason(newReason)
            .build());
    }

    @Operation(operationId = "CHALLENGER-WORKBOOK-007", summary = "운영진용: 베스트 워크북 선정 철회")
    @CheckAccess(
        resourceType = ResourceType.WEEKLY_BEST_WORKBOOK,
        resourceId = "#weeklyBestWorkbookId",
        permission = PermissionType.DELETE
    )
    @DeleteMapping("/weekly-best/{weeklyBestWorkbookId}")
    public void withdrawBest(
        @PathVariable Long weeklyBestWorkbookId,
        @CurrentMember MemberPrincipal principal
    ) {
        manageWeeklyBestWorkbookUseCase.withdraw(weeklyBestWorkbookId);
    }
}
