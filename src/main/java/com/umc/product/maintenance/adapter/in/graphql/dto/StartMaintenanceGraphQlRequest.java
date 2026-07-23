package com.umc.product.maintenance.adapter.in.graphql.dto;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

import com.umc.product.maintenance.application.port.in.command.dto.StartMaintenanceCommand;
import com.umc.product.maintenance.domain.MaintenanceDomain;
import com.umc.product.maintenance.domain.MaintenanceScope;

public record StartMaintenanceGraphQlRequest(
    MaintenanceScope scope,
    List<MaintenanceDomain> targetDomains,
    Instant startAt,
    Instant endAt,
    String title,
    String message
) {

    public StartMaintenanceCommand toCommand(Long createdBy) {
        return new StartMaintenanceCommand(
            scope,
            new LinkedHashSet<>(targetDomains),
            startAt,
            endAt,
            title,
            message,
            createdBy
        );
    }
}
