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
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateCurriculumRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateWeeklyCurriculumRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditCurriculumRequest;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.EditWeeklyCurriculumRequest;
import com.umc.product.curriculum.application.port.in.command.ManageCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.ManageWeeklyCurriculumUseCase;
import com.umc.product.curriculum.application.port.in.command.dto.curriculum.EditCurriculumCommand;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v2/curriculums")
@RequiredArgsConstructor
@Tag(
    name = "Curriculum V2 | Curriculum & WeeklyCurriculum Command",
    description = "해당 기수 중앙운영진 또는 SUPER_ADMIN이 커리큘럼과 주차별 내용을 관리합니다."
)
public class CurriculumCommandV2Controller {

    private final ManageCurriculumUseCase manageCurriculumUseCase;
    private final ManageWeeklyCurriculumUseCase manageWeeklyCurriculumUseCase;

    // ==== Curriculum CUD ====

    @Operation(
        operationId = "CURRICULUM-001",
        summary = "커리큘럼 생성",
        description = """
            기수의 학습 방식에 맞는 파트 또는 기본 트랙 커리큘럼을 생성합니다.

            동일 기수의 같은 파트 또는 트랙 커리큘럼은 중복 생성할 수 없습니다. PLUS는 지원하지 않습니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "'gisu:' + #request.gisuId",
        permission = PermissionType.WRITE
    )
    @PostMapping
    public Long createCurriculum(
        @Valid @RequestBody CreateCurriculumRequest request
    ) {
        return manageCurriculumUseCase.create(request.toCommand());
    }

    @Operation(
        operationId = "CURRICULUM-002",
        summary = "커리큘럼 수정",
        description = """
            상위 객체인 커리큘럼을 수정합니다.

            기수, 파트, 트랙은 수정이 불가능하며 커리큘럼의 이름만 수정 가능합니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "#curriculumId",
        permission = PermissionType.WRITE
    )
    @PatchMapping("/{curriculumId}")
    public void editCurriculum(
        @Valid @RequestBody EditCurriculumRequest request,
        @PathVariable Long curriculumId
    ) {
        manageCurriculumUseCase.edit(EditCurriculumCommand.builder()
            .curriculumId(curriculumId)
            .title(request.title())
            .build());
    }

    @Operation(
        operationId = "CURRICULUM-003",
        summary = "커리큘럼 삭제",
        description = """
            - 커리큘럼 내부에 포함된 주차별 커리큘럼이 존재하는 경우 삭제하지 못합니다.
            - 해당 기수 중앙운영진 또는 SUPER_ADMIN만 관리할 수 있습니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "#curriculumId",
        permission = PermissionType.DELETE
    )
    @DeleteMapping("/{curriculumId}")
    public void deleteCurriculum(
        @PathVariable Long curriculumId
    ) {
        manageCurriculumUseCase.delete(curriculumId);
    }

    // ==== Weekly Curriculum CUD ====

    @Operation(
        operationId = "CURRICULUM-004",
        summary = "각 커리큘럼에 새로운 주차 생성",
        description = """
            상위 객체인 커리큘럼에 각 주차별 커리큘럼을 생성합니다.

            각 커리큘럼에 대해서, 주차별 커리큘럼은 최대 2개 (MAIN, EXTRA) 생성 가능합니다.
            주차별 커리큘럼 내에 원본 워크북 수와는 다른 개념입니다!
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "#request.curriculumId",
        permission = PermissionType.WRITE
    )
    @PostMapping("/weekly")
    public Long createWeeklyCurriculum(
        @Valid @RequestBody CreateWeeklyCurriculumRequest request
    ) {
        return manageWeeklyCurriculumUseCase.create(request.toCommand());
    }

    @Operation(
        operationId = "CURRICULUM-005",
        summary = "주차별 커리큘럼 수정",
        description = """
            주차별 커리큘럼 자체를 수정합니다.

            - 제목은 상시로 수정이 가능합니다.
            - 포홤된 주차별 워크북이 하나라도 배포된 상태라면 시작/종료일은 수정이 불가능합니다.
            - 시작일 < 종료일
            - 주차는 이미 설정되지 않은 주차로만 수정이 가능합니다.
            - UK 제약을 어기는 수정은 불허합니다. (e.g. 정규, 부록 -> 정규, 정규 불가능)
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "'weekly:' + #weeklyCurriculumId",
        permission = PermissionType.WRITE
    )
    @PatchMapping("/weekly/{weeklyCurriculumId}")
    public void editWeeklyCurriculum(
        @PathVariable Long weeklyCurriculumId,
        @RequestBody EditWeeklyCurriculumRequest request
    ) {
        manageWeeklyCurriculumUseCase.edit(request.toCommand(weeklyCurriculumId));
    }

    @Operation(
        operationId = "CURRICULUM-006",
        summary = "주차별 커리큘럼 삭제",
        description = """
            주차별 커리큘럼을 삭제합니다.

            - 포함된 주차별 워크북이 하나라도 존재하면 삭제가 불가능합니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.CURRICULUM,
        resourceId = "'weekly:' + #weeklyCurriculumId",
        permission = PermissionType.DELETE
    )
    @DeleteMapping("/weekly/{weeklyCurriculumId}")
    public void deleteWeeklyCurriculum(
        @PathVariable Long weeklyCurriculumId
    ) {
        manageWeeklyCurriculumUseCase.delete(weeklyCurriculumId);
    }
}
