# Authorization Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `authorization-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `becdb4192ab08a2fba5a4cce1146c76afb2690e426db420befa3774fffbc4454`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:DELETE /api/v1/authorization/challenger-role/{challengerRoleId} | com.umc.product.authorization.adapter.in.web.ChallengerRoleController#deleteChallengerRole | REST | challenger-role:delete | challenger-role-resource | DIRECT |
| rest:GET /api/v1/authorization/challenger-role/{challengerRoleId} | com.umc.product.authorization.adapter.in.web.ChallengerRoleController#getChallengerRole | REST | challenger-role:read | challenger-role-resource | DIRECT |
| rest:POST /api/v1/authorization/challenger-role | com.umc.product.authorization.adapter.in.web.ChallengerRoleController#createChallengerRole | REST | challenger-role:create | challenger-role-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| challenger-role-resource | challenger-role.manage.active-central-core | ALLOW | challenger-role:create, challenger-role:delete | EQ(ATTRIBUTE(relation.activeCentralCore), true) |  |
| challenger-role-resource | challenger-role.read.authenticated | ALLOW | challenger-role:read | EQ(ATTRIBUTE(subject.authenticated), true) |  |
