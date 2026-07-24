# Member Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `member-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `a3d3994eaa2ba045f35fe7817b6d4c75650a655c7c79de5db2c29d1de4c52b0b`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:DELETE /api/v1/members/admin/{memberId} | com.umc.product.member.adapter.in.web.MemberCommandController#deleteMemberByAdmin | REST | member:delete | member-resource | DIRECT |
| rest:GET /api/v1/members/{memberId} | com.umc.product.member.adapter.in.web.MemberQueryController#getMember | REST | member:read | member-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| member-resource | member.delete.active-central-core | ALLOW | member:delete | EQ(ATTRIBUTE(relation.activeCentralCore), true) |  |
| member-resource | member.read.challenger | ALLOW | member:read | EQ(ATTRIBUTE(relation.challenger), true) |  |
