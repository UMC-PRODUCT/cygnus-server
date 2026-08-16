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
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateOriginalWorkbookMissionRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditOriginalWorkbookMissionRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.response.CreateOriginalWorkbookMissionResponse;
import com.umc.product.curriculum.application.port.in.command.ManageOriginalWorkbookMissionUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/curriculums/original-workbooks/missions")
@RequiredArgsConstructor
@Tag(name = "Curriculum V2 | Original Workbook Mission Command", description = "교육국 파트장이 원본 워크북의 미션을 관리합니다.")
public class OriginalWorkbookMissionCommandV2Controller {

    private final ManageOriginalWorkbookMissionUseCase manageOriginalWorkbookMissionUseCase;

    @Operation(
        operationId = "ORIGINAL-WORKBOOK-MISSION-001",
        summary = "중앙파트장용: 원본 워크북에 미션 추가",
        description = """
            미션을 추가합니다.

            - 이미 배포된 OriginalWorkbook에 대해서는, 필수가 아닌 미션만 추가가 가능합니다.
            - 배포되지 않은 OriginalWorkbook에 대해서는 따로 제한이 없습니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.ORIGINAL_WORKBOOK,
        permission = PermissionType.MANAGE,
        message = "원본 워크북 미션 추가는 중앙파트장 이상 권한이 필요합니다."
    )
    @PostMapping
    public CreateOriginalWorkbookMissionResponse createOriginalWorkbookMission(
        @Valid @RequestBody CreateOriginalWorkbookMissionRequest request
    ) {
        return CreateOriginalWorkbookMissionResponse.from(
            manageOriginalWorkbookMissionUseCase.create(request.toCommand())
        );
    }

    @Operation(
        operationId = "ORIGINAL-WORKBOOK-MISSION-002",
        summary = "중앙파트장용: 원본 워크북의 미션 수정",
        description = """
            미션을 수정합니다.

            관련된 모든 필드 (제목, 설명, 제출 유형 등)을 수정할 수 있으며, 제공되지 않은 값은 유지되는 것으로 간주합니다.
            단, OriginalWorkbook이 배포된 경우 미션의 필수 진행 여부는 필수->선택 방향으로만 변경이 가능합니다.

            모든 변경으로 인한 책임은 변경한 중앙 파트장에게 있습니다. 주의하세요!
            """
    )
    @CheckAccess(
        resourceType = ResourceType.ORIGINAL_WORKBOOK,
        permission = PermissionType.MANAGE,
        message = "원본 워크북 미션 수정은 중앙파트장 이상 권한이 필요합니다."
    )
    @PatchMapping("/{originalWorkbookMissionId}")
    public void editOriginalMission(
        @PathVariable Long originalWorkbookMissionId,
        @Valid @RequestBody EditOriginalWorkbookMissionRequest request
    ) {
        manageOriginalWorkbookMissionUseCase.edit(request.toCommand(originalWorkbookMissionId));
    }

    @Operation(
        operationId = "ORIGINAL-WORKBOOK-MISSION-003",
        summary = "중앙파트장용: 원본 워크북의 미션 삭제",
        description = """
            미션을 삭제합니다.

            이미 제출한 사람이 있는 미션을 제외하고는 시점과 관계없이 삭제할 수 있습니다.

            수정과 마찬가지로 모든 변경에 대한 책임은 중앙 파트장에게 있습니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.ORIGINAL_WORKBOOK,
        permission = PermissionType.MANAGE,
        message = "원본 워크북 미션 삭제는 중앙파트장 이상 권한이 필요합니다."
    )
    @DeleteMapping("/{originalWorkbookMissionId}")
    public void deleteOriginalMission(
        @PathVariable Long originalWorkbookMissionId
    ) {
        manageOriginalWorkbookMissionUseCase.delete(originalWorkbookMissionId);
    }
}
