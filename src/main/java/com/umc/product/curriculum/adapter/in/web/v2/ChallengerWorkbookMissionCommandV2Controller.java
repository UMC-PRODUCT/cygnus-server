package com.umc.product.curriculum.adapter.in.web.v2;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateMissionFeedbackRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateMissionSubmissionRequest;
import com.umc.product.curriculum.application.port.in.command.ManageMissionFeedbackUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageMissionSubmissionUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.DeleteMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.DeleteMissionSubmissionCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionFeedbackCommand;
import com.umc.product.curriculum.application.port.in.command.dto.workbook.mission.EditMissionSubmissionCommand;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/curriculums/challenger-workbooks/missions")
@RequiredArgsConstructor
@Tag(name = "Curriculum V2 | Challenger Workbook Mission Command", description = "워크북 미션 제출과 운영진 피드백을 다룹니다.")
public class ChallengerWorkbookMissionCommandV2Controller {

    private final ManageMissionSubmissionUseCase manageMissionSubmissionUseCase;
    private final ManageMissionFeedbackUseCase manageMissionFeedbackUseCase;

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-MISSION-001",
        summary = "챌린저용: 워크북 내 미션 제출",
        description = "주차별 커리큘럼 종료 시각 전까지 제출할 수 있습니다. LATE 저장과 벌점 자동화는 후속 범위입니다."
    )
    @CheckAccess(
        resourceType = ResourceType.MISSION_SUBMISSION,
        resourceId = "#request.challengerMissionId",
        permission = PermissionType.WRITE
    )
    @PostMapping
    public void createMissionSubmission(
        @CurrentMember MemberPrincipal principal,
        @Valid @RequestBody CreateMissionSubmissionRequest request
    ) {
        manageMissionSubmissionUseCase.create(request.toCommand(principal.getMemberId()));
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-MISSION-002",
        summary = "챌린저용: 제출한 워크북 미션 수정",
        description = "스터디 일정이 있으면 일정 시작일 KST 00:00 전까지, 없으면 주차 종료 시각 전까지 수정할 수 있습니다."
    )
    @CheckAccess(
        resourceType = ResourceType.MISSION_SUBMISSION,
        resourceId = "#missionSubmissionId",
        permission = PermissionType.EDIT
    )
    @PatchMapping("/{missionSubmissionId}")
    public void editMissionSubmission(
        @CurrentMember MemberPrincipal principal,
        @PathVariable Long missionSubmissionId,
        @RequestBody String content
    ) {
        manageMissionSubmissionUseCase.edit(EditMissionSubmissionCommand.builder()
            .missionSubmissionId(missionSubmissionId)
            .requesterMemberId(principal.getMemberId())
            .content(content)
            .build());
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-MISSION-003",
        summary = "챌린저용: 제출한 워크북 미션 철회",
        description = "기간과 관계없이 한 번 철회할 수 있으며, 철회 후에는 재제출할 수 없습니다."
    )
    @CheckAccess(
        resourceType = ResourceType.MISSION_SUBMISSION,
        resourceId = "#missionSubmissionId",
        permission = PermissionType.DELETE
    )
    @DeleteMapping("/{missionSubmissionId}")
    public void withdrawMissionSubmission(
        @CurrentMember MemberPrincipal principal,
        @PathVariable Long missionSubmissionId
    ) {
        manageMissionSubmissionUseCase.withdraw(DeleteMissionSubmissionCommand.builder()
            .missionSubmissionId(missionSubmissionId)
            .requesterMemberId(principal.getMemberId())
            .build());
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-MISSION-004",
        summary = "운영진용: 제출된 미션에 대한 피드백 작성",
        description = "담당 그룹 mentor, 같은 학교·기수 회장단 또는 SUPER_ADMIN이 작성할 수 있습니다. 벌점 자동화는 후속 범위입니다."
    )
    @CheckAccess(
        resourceType = ResourceType.MISSION_FEEDBACK,
        resourceId = "#request.missionSubmissionId",
        permission = PermissionType.WRITE
    )
    @PostMapping("/feedback")
    public void createMissionFeedback(
        @CurrentMember MemberPrincipal principal,
        @Valid @RequestBody CreateMissionFeedbackRequest request
    ) {
        manageMissionFeedbackUseCase.create(request.toCommand(principal.getMemberId()));
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-MISSION-005",
        summary = "운영진용: 제출된 미션에 대한 피드백 수정",
        description = "작성자 본인이 작성 시각으로부터 14일 전까지 내용을 수정할 수 있습니다."
    )
    @CheckAccess(
        resourceType = ResourceType.MISSION_FEEDBACK,
        resourceId = "#missionFeedbackId",
        permission = PermissionType.EDIT
    )
    @PatchMapping("/feedback/{missionFeedbackId}")
    public void editMissionFeedback(
        @CurrentMember MemberPrincipal principal,
        @PathVariable Long missionFeedbackId,
        @RequestBody String content
    ) {
        manageMissionFeedbackUseCase.edit(EditMissionFeedbackCommand.builder()
            .missionFeedbackId(missionFeedbackId)
            .reviewerMemberId(principal.getMemberId())
            .content(content)
            .build());
    }

    @Operation(
        operationId = "CHALLENGER-WORKBOOK-MISSION-006",
        summary = "운영진용: 제출된 미션에 대한 피드백 삭제",
        description = "작성자 본인이 해당 기수 종료 시각 전까지 삭제할 수 있습니다."
    )
    @CheckAccess(
        resourceType = ResourceType.MISSION_FEEDBACK,
        resourceId = "#missionFeedbackId",
        permission = PermissionType.DELETE
    )
    @DeleteMapping("/feedback/{missionFeedbackId}")
    public void deleteMissionFeedback(
        @CurrentMember MemberPrincipal principal,
        @PathVariable Long missionFeedbackId
    ) {
        manageMissionFeedbackUseCase.delete(DeleteMissionFeedbackCommand.builder()
            .missionFeedbackId(missionFeedbackId)
            .operatorMemberId(principal.getMemberId())
            .build());
    }
}
