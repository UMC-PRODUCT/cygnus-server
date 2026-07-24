# Challenger Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `challenger-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `553b412ca723289ab9a62115e11207e95b384cf5b8f4f1bf207ca82aa9ae4de3`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:DELETE /api/v1/challengers/points/{challengerPointId} | com.umc.product.challenger.adapter.in.web.ChallengerPointCommandController#deleteChallengerPoint | REST | challenger-point:delete | challenger-resource | DIRECT |
| rest:DELETE /api/v1/challengers/{challengerId} | com.umc.product.challenger.adapter.in.web.ChallengerCommandController#deleteChallenger | REST | challenger:delete | challenger-resource | DIRECT |
| rest:GET /api/v1/challenger-records/code/{code} | com.umc.product.challenger.adapter.in.web.ChallengerRecordController#getChallengerRecordByCode | REST | challenger-record:read | challenger-resource | DIRECT |
| rest:GET /api/v1/challenger-records/id/{id} | com.umc.product.challenger.adapter.in.web.ChallengerRecordController#getChallengerRecordById | REST | challenger-record:read | challenger-resource | DIRECT |
| rest:PATCH /api/v1/challengers/points/{challengerPointId} | com.umc.product.challenger.adapter.in.web.ChallengerPointCommandController#editChallengerPoints | REST | challenger-point:update | challenger-resource | DIRECT |
| rest:PATCH /api/v1/challengers/{challengerId}/part | com.umc.product.challenger.adapter.in.web.ChallengerCommandController#editChallengerInfo | REST | challenger:update | challenger-resource | DIRECT |
| rest:POST /api/v1/challenger-records | com.umc.product.challenger.adapter.in.web.ChallengerRecordController#createChallengerRecord | REST | challenger-record:create | challenger-resource | DIRECT |
| rest:POST /api/v1/challenger-records/bulk | com.umc.product.challenger.adapter.in.web.ChallengerRecordController#createChallengerRecordBulk | REST | challenger-record:create | challenger-resource | DIRECT |
| rest:POST /api/v1/challengers | com.umc.product.challenger.adapter.in.web.ChallengerCommandController#createChallenger | REST | challenger:create | challenger-resource | DIRECT |
| rest:POST /api/v1/challengers/batch | com.umc.product.challenger.adapter.in.web.ChallengerCommandController#bulkCreateChallenger | REST | challenger:create | challenger-resource | DIRECT |
| rest:POST /api/v1/challengers/{challengerId}/deactivate | com.umc.product.challenger.adapter.in.web.ChallengerCommandController#deactivateChallenger | REST | challenger:update | challenger-resource | DIRECT |
| rest:POST /api/v1/challengers/{challengerId}/points | com.umc.product.challenger.adapter.in.web.ChallengerPointCommandController#grantChallengerPoints | REST | challenger-point:create | challenger-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| challenger-resource | challenger-point.target-gisu-central | ALLOW | challenger-point:create, challenger-point:update | EQ(ATTRIBUTE(relation.activeCentralMemberInTargetGisu), true) |  |
| challenger-resource | challenger-point.target-school-core | ALLOW | challenger-point:create, challenger-point:update | EQ(ATTRIBUTE(relation.activeSchoolCoreForTarget), true) |  |
| challenger-resource | challenger.central-core | ALLOW | challenger-point:delete, challenger-record:create, challenger-record:delete, challenger:delete, challenger:update | EQ(ATTRIBUTE(relation.activeCentralCore), true) |  |
| challenger-resource | challenger.school-core | ALLOW | challenger-record:read, challenger:create | EQ(ATTRIBUTE(relation.activeSchoolCore), true) |  |
