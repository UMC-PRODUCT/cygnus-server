# Audit Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `audit-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `021043833c198f543de621184b8c3e80bb8d4e6657b7e1c75b034e0044ef3c1d`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:GET /api/v1/audit/admin/audit-logs | com.umc.product.audit.adapter.in.web.AuditLogController#search | REST | audit-log:list | audit-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| audit-resource | audit-log.list.active-central-member | ALLOW | audit-log:list | EQ(ATTRIBUTE(relation.activeCentralMember), true) |  |
