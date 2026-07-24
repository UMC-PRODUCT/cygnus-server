# Organization Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `organization-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `db8d6cb41422234635fa77c90098e09dd67f099b7825b382b30b3b501212150a`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:DELETE /api/v1/chapters/{chapterId} | com.umc.product.organization.adapter.in.web.ChapterCommandController#deleteChapter | REST | chapter:delete | organization-resource | DIRECT |
| rest:DELETE /api/v1/gisu/{gisuId} | com.umc.product.organization.adapter.in.web.GisuCommandController#deleteGisu | REST | gisu:delete | organization-resource | DIRECT |
| rest:DELETE /api/v1/schools | com.umc.product.organization.adapter.in.web.SchoolCommandController#deleteSchools | REST | school:delete | organization-resource | DIRECT |
| rest:DELETE /api/v1/study-groups/{studyGroupId} | com.umc.product.organization.adapter.in.web.StudyGroupCommandController#delete | REST | study-group:delete | organization-resource | DIRECT |
| rest:DELETE /api/v1/study-groups/{studyGroupId}/members/{memberId} | com.umc.product.organization.adapter.in.web.StudyGroupCommandController#deleteMember | REST | study-group:update | organization-resource | DIRECT |
| rest:DELETE /api/v1/study-groups/{studyGroupId}/mentors/{mentorId} | com.umc.product.organization.adapter.in.web.StudyGroupCommandController#deleteMentor | REST | study-group:update | organization-resource | DIRECT |
| rest:GET /api/v1/study-groups/{studyGroupId} | com.umc.product.organization.adapter.in.web.StudyGroupQueryController#getStudyGroupInfo | REST | study-group:read | organization-resource | DIRECT |
| rest:PATCH /api/v1/schools/{schoolId} | com.umc.product.organization.adapter.in.web.SchoolCommandController#updateSchool | REST | school:update | organization-resource | DIRECT |
| rest:PATCH /api/v1/schools/{schoolId}/assign | com.umc.product.organization.adapter.in.web.SchoolCommandController#assignToChapter | REST | school:update | organization-resource | DIRECT |
| rest:PATCH /api/v1/schools/{schoolId}/unassign | com.umc.product.organization.adapter.in.web.SchoolCommandController#unassignFromChapter | REST | school:update | organization-resource | DIRECT |
| rest:PATCH /api/v1/study-groups/{studyGroupId} | com.umc.product.organization.adapter.in.web.StudyGroupCommandController#update | REST | study-group:update | organization-resource | DIRECT |
| rest:PATCH /api/v1/study-groups/{studyGroupId}/members/{memberId} | com.umc.product.organization.adapter.in.web.StudyGroupCommandController#addMember | REST | study-group:update | organization-resource | DIRECT |
| rest:PATCH /api/v1/study-groups/{studyGroupId}/mentors/{mentorId} | com.umc.product.organization.adapter.in.web.StudyGroupCommandController#addMentor | REST | study-group:update | organization-resource | DIRECT |
| rest:POST /api/v1/chapters | com.umc.product.organization.adapter.in.web.ChapterCommandController#createChapter | REST | chapter:create | organization-resource | DIRECT |
| rest:POST /api/v1/chapters/bulk | com.umc.product.organization.adapter.in.web.ChapterCommandController#createChapterBulk | REST | chapter:create | organization-resource | DIRECT |
| rest:POST /api/v1/gisu | com.umc.product.organization.adapter.in.web.GisuCommandController#createGisu | REST | gisu:create | organization-resource | DIRECT |
| rest:POST /api/v1/gisu/{gisuId}/active | com.umc.product.organization.adapter.in.web.GisuCommandController#updateActiveGisu | REST | gisu:update | organization-resource | DIRECT |
| rest:POST /api/v1/schools | com.umc.product.organization.adapter.in.web.SchoolCommandController#createSchool | REST | school:create | organization-resource | DIRECT |
| rest:POST /api/v1/study-groups | com.umc.product.organization.adapter.in.web.StudyGroupCommandController#create | REST | study-group:create | organization-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| organization-resource | organization.structure.active-central-core | ALLOW | chapter:create, chapter:delete, gisu:create, gisu:delete, gisu:update, school:create, school:delete, school:update | EQ(ATTRIBUTE(relation.activeCentralCore), true) |  |
| organization-resource | study-group.manage.active-school-core | ALLOW | study-group:create, study-group:delete, study-group:update | EQ(ATTRIBUTE(relation.activeSchoolCoreForSubjectSchool), true) |  |
| organization-resource | study-group.read.active-school-admin | ALLOW | study-group:read | EQ(ATTRIBUTE(relation.activeSchoolAdminForSubjectSchool), true) |  |
