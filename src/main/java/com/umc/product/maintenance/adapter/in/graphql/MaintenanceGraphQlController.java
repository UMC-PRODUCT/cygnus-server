package com.umc.product.maintenance.adapter.in.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.maintenance.adapter.in.graphql.dto.StartMaintenanceGraphQlRequest;
import com.umc.product.maintenance.application.port.in.command.ManageMaintenanceUseCase;
import com.umc.product.maintenance.application.port.in.query.GetMaintenanceStatusUseCase;
import com.umc.product.maintenance.application.port.in.query.dto.MaintenanceStatusInfo;
import com.umc.product.maintenance.application.port.in.query.dto.MaintenanceWindowInfo;
import com.umc.product.maintenance.application.port.out.MaintenanceBypassPolicy;
import com.umc.product.maintenance.exception.MaintenanceDomainException;
import com.umc.product.maintenance.exception.MaintenanceErrorCode;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MaintenanceGraphQlController {

    private final GetMaintenanceStatusUseCase getMaintenanceStatusUseCase;
    private final ManageMaintenanceUseCase manageMaintenanceUseCase;
    private final MaintenanceBypassPolicy maintenanceBypassPolicy;

    @QueryMapping
    public MaintenanceStatusInfo maintenanceStatus() {
        return getMaintenanceStatusUseCase.getStatus();
    }

    @QueryMapping
    public List<MaintenanceWindowInfo> maintenanceWindows(@CurrentMember MemberPrincipal principal) {
        requireSuperAdmin(principal);
        return getMaintenanceStatusUseCase.listAll();
    }

    @QueryMapping
    public MaintenanceWindowInfo maintenanceWindow(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        requireSuperAdmin(principal);
        return getMaintenanceStatusUseCase.getById(id);
    }

    @MutationMapping
    public MaintenanceWindowInfo startMaintenance(
        @CurrentMember MemberPrincipal principal,
        @Argument StartMaintenanceGraphQlRequest input
    ) {
        Long memberId = requireSuperAdmin(principal);
        Long windowId = manageMaintenanceUseCase.start(input.toCommand(memberId));
        return getMaintenanceStatusUseCase.getById(windowId);
    }

    @MutationMapping
    public MaintenanceWindowInfo endMaintenance(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        Long memberId = requireSuperAdmin(principal);
        manageMaintenanceUseCase.forceEnd(id, memberId);
        return getMaintenanceStatusUseCase.getById(id);
    }

    private Long requireSuperAdmin(MemberPrincipal principal) {
        if (principal == null || !maintenanceBypassPolicy.shouldBypass(principal.getMemberId())) {
            throw new MaintenanceDomainException(MaintenanceErrorCode.NOT_SUPER_ADMIN);
        }
        return principal.getMemberId();
    }
}
