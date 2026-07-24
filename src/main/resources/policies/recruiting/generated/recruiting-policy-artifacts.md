# Recruiting Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `recruiting-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `82bad66822ae4874d327283991a892036e799a1d9992e49a11bb27feeaa0fac9`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| capability:ResourceType.RECRUITMENT/MANAGE | com.umc.product.recruiting.application.service.evaluator.RecruitingPermissionEvaluator#evaluate | INTERNAL_BATCH | recruiting:manage-all | recruiting-resource | CAPABILITY |
| capability:ResourceType.RECRUITMENT/READ-WRITE-EDIT-APPROVE | com.umc.product.recruiting.application.service.evaluator.RecruitingPermissionEvaluator#evaluate | INTERNAL_BATCH | recruiting:operate-school | recruiting-resource | CAPABILITY |
| rest:DELETE /api/v1/recruiting/admin/applications/{applicationId}/registration/ready | com.umc.product.recruiting.adapter.in.web.RecruitingAdminController#cancelRegistration | REST | recruiting-registration:manage | recruiting-resource | DIRECT |
| rest:GET /api/v1/recruiting/admin/statistics.csv | com.umc.product.recruiting.adapter.in.web.RecruitingAdminController#exportCsv | REST | recruiting:export | recruiting-resource | DIRECT |
| rest:GET /api/v1/recruiting/admin/summary | com.umc.product.recruiting.adapter.in.web.RecruitingAdminController#getSummary | REST | recruiting-summary:read | recruiting-resource | DIRECT |
| rest:PATCH /api/v1/recruiting/admin/applications/{applicationId}/document-decision | com.umc.product.recruiting.adapter.in.web.RecruitingAdminController#decideDocument | REST | recruiting-application:decide | recruiting-resource | DIRECT |
| rest:PATCH /api/v1/recruiting/admin/applications/{applicationId}/final-decision | com.umc.product.recruiting.adapter.in.web.RecruitingAdminController#decideFinal | REST | recruiting-application:decide | recruiting-resource | DIRECT |
| rest:POST /api/v1/recruiting/admin/applications/{applicationId}/interview/skip | com.umc.product.recruiting.adapter.in.web.RecruitingAdminController#skipInterview | REST | recruiting-application:decide | recruiting-resource | DIRECT |
| rest:POST /api/v1/recruiting/admin/applications/{applicationId}/registration/ready | com.umc.product.recruiting.adapter.in.web.RecruitingAdminController#prepareRegistration | REST | recruiting-registration:manage | recruiting-resource | DIRECT |
| rest:POST /api/v1/recruiting/admin/applications/{applicationId}/registration/registered | com.umc.product.recruiting.adapter.in.web.RecruitingAdminController#confirmRegistration | REST | recruiting-registration:manage | recruiting-resource | DIRECT |
| rest:POST /api/v1/recruiting/admin/seasons | com.umc.product.recruiting.adapter.in.web.RecruitingSeasonAdminController#createSeason | REST | recruiting-season:create | recruiting-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| recruiting-resource | recruiting.manage.target-central | ALLOW | recruiting:manage-all | ALL(EQ(ATTRIBUTE(relation.activeCentralCoreInTargetGisu), true), EQ(ATTRIBUTE(resource.specified), true)) |  |
| recruiting-resource | recruiting.manage.unspecified-central | ALLOW | recruiting:manage-all | ALL(EQ(ATTRIBUTE(relation.activeAnyCentralCore), true), EQ(ATTRIBUTE(resource.specified), false)) |  |
| recruiting-resource | recruiting.operate.target-central | ALLOW | recruiting:operate-school | ALL(EQ(ATTRIBUTE(relation.activeCentralCoreInTargetGisu), true), EQ(ATTRIBUTE(resource.specified), true)) |  |
| recruiting-resource | recruiting.operate.target-school | ALLOW | recruiting:operate-school | ALL(EQ(ATTRIBUTE(relation.activeSchoolCoreForTarget), true), EQ(ATTRIBUTE(resource.specified), true)) |  |
| recruiting-resource | recruiting.operate.unspecified-central | ALLOW | recruiting:operate-school | ALL(EQ(ATTRIBUTE(relation.activeAnyCentralCore), true), EQ(ATTRIBUTE(resource.specified), false)) |  |
| recruiting-resource | recruiting.operate.unspecified-school | ALLOW | recruiting:operate-school | ALL(EQ(ATTRIBUTE(relation.activeAnySchoolCore), true), EQ(ATTRIBUTE(resource.specified), false)) |  |
| recruiting-resource | recruiting.super-admin | ALLOW | recruiting-application:decide, recruiting-registration:manage, recruiting-season:create, recruiting-summary:read, recruiting:export, recruiting:manage-all, recruiting:operate-school | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
| recruiting-resource | recruiting.target-central-operator | ALLOW | recruiting-application:decide, recruiting-registration:manage, recruiting-season:create, recruiting-summary:read, recruiting:export | EQ(ATTRIBUTE(relation.activeCentralCoreInTargetGisu), true) |  |
| recruiting-resource | recruiting.target-school-operator | ALLOW | recruiting-application:decide, recruiting-season:create | EQ(ATTRIBUTE(relation.activeSchoolCoreForTarget), true) |  |
