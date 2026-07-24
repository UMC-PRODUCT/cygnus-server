# Maintenance Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `maintenance-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `a7d22532161181fb132099381f215c93285d651a06bc84cf7a352ed2e25ff714`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| filter:maintenance-bypass | com.umc.product.maintenance.adapter.in.web.filter.MaintenanceFilter#doFilterInternal | INTERNAL_BATCH | maintenance:bypass | maintenance-resource | ACTOR |
| rest:GET /api/v1/maintenance/admin | com.umc.product.maintenance.adapter.in.web.AdminMaintenanceController#listAll | REST | maintenance:bypass | maintenance-resource | DIRECT |
| rest:GET /api/v1/maintenance/admin/{windowId} | com.umc.product.maintenance.adapter.in.web.AdminMaintenanceController#getOne | REST | maintenance:bypass | maintenance-resource | DIRECT |
| rest:PATCH /api/v1/maintenance/admin/{windowId}/end | com.umc.product.maintenance.adapter.in.web.AdminMaintenanceController#forceEnd | REST | maintenance:bypass | maintenance-resource | DIRECT |
| rest:POST /api/v1/maintenance/admin | com.umc.product.maintenance.adapter.in.web.AdminMaintenanceController#start | REST | maintenance:bypass | maintenance-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| maintenance-resource | maintenance.bypass.super-admin | ALLOW | maintenance:bypass | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
