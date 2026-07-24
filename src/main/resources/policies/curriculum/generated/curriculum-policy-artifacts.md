# Curriculum Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `curriculum-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `ae495d779a1b012a23ebe1f346f17bc41aa626c0164104231476badcb88332e7`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:DELETE /api/v2/curriculums/original-workbooks/missions/{originalWorkbookMissionId} | com.umc.product.curriculum.adapter.in.web.v2.OriginalWorkbookMissionCommandV2Controller#deleteOriginalMission | REST | original-workbook:manage | curriculum-resource | DIRECT |
| rest:DELETE /api/v2/curriculums/original-workbooks/{originalWorkbookId} | com.umc.product.curriculum.adapter.in.web.v2.OriginalWorkbookCommandV2Controller#deleteOriginalWorkbook | REST | original-workbook:manage | curriculum-resource | DIRECT |
| rest:PATCH /api/v2/curriculums/original-workbooks/missions/{originalWorkbookMissionId} | com.umc.product.curriculum.adapter.in.web.v2.OriginalWorkbookMissionCommandV2Controller#editOriginalMission | REST | original-workbook:manage | curriculum-resource | DIRECT |
| rest:PATCH /api/v2/curriculums/original-workbooks/status | com.umc.product.curriculum.adapter.in.web.v2.OriginalWorkbookCommandV2Controller#changeOriginalWorkbookStatus | REST | original-workbook:release | curriculum-resource | DIRECT |
| rest:PATCH /api/v2/curriculums/original-workbooks/{originalWorkbookId} | com.umc.product.curriculum.adapter.in.web.v2.OriginalWorkbookCommandV2Controller#editOriginalWorkbook | REST | original-workbook:manage | curriculum-resource | DIRECT |
| rest:POST /api/v2/curriculums/original-workbooks | com.umc.product.curriculum.adapter.in.web.v2.OriginalWorkbookCommandV2Controller#createOriginalWorkbook | REST | original-workbook:manage | curriculum-resource | DIRECT |
| rest:POST /api/v2/curriculums/original-workbooks/draft | com.umc.product.curriculum.adapter.in.web.v2.OriginalWorkbookCommandV2Controller#createOriginalWorkbookAsDraft | REST | original-workbook:manage | curriculum-resource | DIRECT |
| rest:POST /api/v2/curriculums/original-workbooks/missions | com.umc.product.curriculum.adapter.in.web.v2.OriginalWorkbookMissionCommandV2Controller#createOriginalWorkbookMission | REST | original-workbook:manage | curriculum-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| curriculum-resource | original-workbook.manage-release.active-central | ALLOW | original-workbook:manage, original-workbook:release | EQ(ATTRIBUTE(relation.activeCentralMember), true) |  |
| curriculum-resource | original-workbook.manage-release.super-admin | ALLOW | original-workbook:manage, original-workbook:release | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
| curriculum-resource | workbook-submission.read.active-school-admin | ALLOW | workbook-submission:read | EQ(ATTRIBUTE(relation.activeSchoolAdmin), true) |  |
| curriculum-resource | workbook-submission.read.super-admin | ALLOW | workbook-submission:read | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
