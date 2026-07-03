package com.umc.product.maintenance.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.maintenance.adapter.in.web.dto.request.StartMaintenanceRequest;
import com.umc.product.maintenance.adapter.in.web.dto.response.MaintenanceWindowResponse;
import com.umc.product.maintenance.application.port.in.command.ManageMaintenanceUseCase;
import com.umc.product.maintenance.application.port.in.query.GetMaintenanceStatusUseCase;
import com.umc.product.maintenance.application.port.out.MaintenanceBypassPolicy;
import com.umc.product.maintenance.exception.MaintenanceDomainException;
import com.umc.product.maintenance.exception.MaintenanceErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/maintenance/admin")
@RequiredArgsConstructor
@Tag(name = "Maintenance | 점검 관리 (어드민)", description = "SUPER_ADMIN이 점검 윈도우를 관리합니다.")
public class AdminMaintenanceController {

    private final ManageMaintenanceUseCase manageMaintenanceUseCase;
    private final GetMaintenanceStatusUseCase getMaintenanceStatusUseCase;
    private final MaintenanceBypassPolicy bypassPolicy;

    @PostMapping
    @Operation(
        operationId = "MAINT-001",
        summary = "점검 윈도우 생성",
        description = "즉시 또는 예약 점검 윈도우를 생성합니다. SUPER_ADMIN 권한이 필요합니다."
    )
    public MaintenanceWindowResponse start(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody StartMaintenanceRequest request
    ) {
        Long memberId = requireSuperAdmin(memberPrincipal);
        Long windowId = manageMaintenanceUseCase.start(request.toCommand(memberId));
        return MaintenanceWindowResponse.from(getMaintenanceStatusUseCase.getById(windowId));
    }

    @PatchMapping("/{windowId}/end")
    @Operation(
        operationId = "MAINT-002",
        summary = "점검 윈도우 강제 종료",
        description = "지정한 점검 윈도우를 즉시 종료합니다. SUPER_ADMIN 권한이 필요합니다."
    )
    public MaintenanceWindowResponse forceEnd(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long windowId
    ) {
        Long memberId = requireSuperAdmin(memberPrincipal);
        manageMaintenanceUseCase.forceEnd(windowId, memberId);
        return MaintenanceWindowResponse.from(getMaintenanceStatusUseCase.getById(windowId));
    }

    @GetMapping
    @Operation(
        operationId = "MAINT-003",
        summary = "점검 윈도우 전체 목록",
        description = "활성, 예약, 종료 상태의 모든 점검 윈도우를 최신순으로 조회합니다."
    )
    public List<MaintenanceWindowResponse> listAll(@CurrentMember MemberPrincipal memberPrincipal) {
        requireSuperAdmin(memberPrincipal);
        return getMaintenanceStatusUseCase.listAll().stream()
            .map(MaintenanceWindowResponse::from)
            .toList();
    }

    @GetMapping("/{windowId}")
    @Operation(
        operationId = "MAINT-004",
        summary = "점검 윈도우 단건 조회"
    )
    public MaintenanceWindowResponse getOne(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long windowId
    ) {
        requireSuperAdmin(memberPrincipal);
        return MaintenanceWindowResponse.from(getMaintenanceStatusUseCase.getById(windowId));
    }

    private Long requireSuperAdmin(MemberPrincipal principal) {
        if (principal == null) {
            throw new MaintenanceDomainException(MaintenanceErrorCode.NOT_SUPER_ADMIN);
        }
        Long memberId = principal.getMemberId();
        if (!bypassPolicy.shouldBypass(memberId)) {
            throw new MaintenanceDomainException(MaintenanceErrorCode.NOT_SUPER_ADMIN);
        }
        return memberId;
    }
}
