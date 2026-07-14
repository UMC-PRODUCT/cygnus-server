# Project Policy Generated Review Artifact

> 이 파일은 생성 산출물입니다. 직접 수정하지 마세요.

## Contract

- schemaVersion: `1.0`
- contextSchemaVersion: `project-1.0`
- policyVersion: `1.1.0`
- policyFingerprint: `7cb5dd0453429ae766788952ae1972c9e24ae2c8f137dfa983a8bbe28a3f5e2e`
- compiled statements: `89`
- runtime surfaces: `46` (REST 37, GraphQL 8, scheduler 1)

## Raw Policy Source SHA-256

| Resource | SHA-256 |
|---|---|
| application-resource.policy.json | `4476774fc20dc727211772cd8e49782e70422130baf536adc34ed4df399fd1d0` |
| application-scope.policy.json | `1553fa56d7f53785bf23d5839d804b88d94e338a3063d88787e4c51b9e96298d` |
| bundle.json | `d85ceff4aa47a65ddcd602f0bb2efd34eb0680670e714676525d200650c1cf74` |
| form.policy.json | `cf90367fe5424a5d5ba0f86851bd593e6f5065bc6d0c366485e4652ad8e1457f` |
| matching-round.policy.json | `d62d7799d97bab654efa32e574a25157e1a4367f6ca3e0987b385af75d3cdf25` |
| project-resource.policy.json | `0d1c33f7dcaaf43b8800277431840ba7e73b7e8449cb196dd8dd6559555f124f` |
| project-scope.policy.json | `b28f283653533d480b49eaab1a601fad4d0d7649694a001ce8b4fb0aa7cbb911` |
| statistics.policy.json | `e79c5f4946c173e03d45769a0aa8774147697a30a90211b7918724c3945a258d` |

## Runtime Surface Catalog

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| graphql:Project.applicationForm | com.umc.product.project.adapter.in.graphql.ProjectGraphQlController#applicationFormByProject | GRAPHQL | project-form:read | form | DIRECT |
| graphql:Project.coProductOwners | com.umc.product.project.adapter.in.graphql.ProjectGraphQlController#coProductOwnersByProject | GRAPHQL | project:read | project-resource | TRANSITIVE |
| graphql:Project.members | com.umc.product.project.adapter.in.graphql.ProjectGraphQlController#membersByProject | GRAPHQL | project-member:list | project-resource | DIRECT |
| graphql:Project.productOwner | com.umc.product.project.adapter.in.graphql.ProjectGraphQlController#productOwnerByProject | GRAPHQL | project:read | project-resource | TRANSITIVE |
| graphql:ProjectMember.application | com.umc.product.project.adapter.in.graphql.ProjectGraphQlController#applicationByProjectMember | GRAPHQL | project-application:read | application-resource | DIRECT |
| graphql:ProjectMember.member | com.umc.product.project.adapter.in.graphql.ProjectGraphQlController#memberByProjectMember | GRAPHQL | project:read | project-resource | TRANSITIVE |
| graphql:Query.project | com.umc.product.project.adapter.in.graphql.ProjectGraphQlController#project | GRAPHQL | project:read | project-resource | DIRECT |
| graphql:Query.projects | com.umc.product.project.adapter.in.graphql.ProjectGraphQlController#projects | GRAPHQL | project:list-public | project-scope | DIRECT |
| rest:DELETE /api/v1/project/matching-rounds/{matchingRoundId} | com.umc.product.project.adapter.in.web.ProjectMatchingRoundController#delete | REST | project-matching-round:delete | matching-round | DIRECT |
| rest:DELETE /api/v1/projects/{projectId} | com.umc.product.project.adapter.in.web.ProjectCommandController#delete | REST | project:delete | project-resource | RESOURCE |
| rest:DELETE /api/v1/projects/{projectId}/applications/{applicationId} | com.umc.product.project.adapter.in.web.ProjectApplicationController#cancel | REST | project-application:cancel | application-resource | RESOURCE |
| rest:DELETE /api/v1/projects/{projectId}/members/{memberId} | com.umc.product.project.adapter.in.web.ProjectCommandController#removeMember | REST | project-member:remove | project-resource | RESOURCE |
| rest:GET /api/v1/project/matching-rounds | com.umc.product.project.adapter.in.web.ProjectMatchingRoundController#list | REST | project-matching-round:list | matching-round | DIRECT |
| rest:GET /api/v1/projects | com.umc.product.project.adapter.in.web.ProjectQueryController#searchProjects | REST | project:list-public | project-scope | ACTOR |
| rest:GET /api/v1/projects/applications | com.umc.product.project.adapter.in.web.ProjectApplicationQueryController#getProjectApplicantsBatch | REST | project-application:list-project-batch | application-scope | DIRECT |
| rest:GET /api/v1/projects/me/applications | com.umc.product.project.adapter.in.web.ProjectApplicationQueryController#getMyApplications | REST | project-application:list-self | application-scope | DIRECT |
| rest:GET /api/v1/projects/me/draft | com.umc.product.project.adapter.in.web.ProjectQueryController#getMyDraft | REST | project:list-own-drafts | project-scope | DIRECT |
| rest:GET /api/v1/projects/me/managed | com.umc.product.project.adapter.in.web.ProjectQueryController#searchManaged | REST | project:list-managed | project-scope | DIRECT |
| rest:GET /api/v1/projects/members | com.umc.product.project.adapter.in.web.ProjectQueryController#getBatchMembers | REST | project-member:batch | project-resource | ACTOR |
| rest:GET /api/v1/projects/permissions | com.umc.product.project.adapter.in.web.ProjectPermissionController#getPermissions | REST | project:capability-list | project-resource | DIRECT |
| rest:GET /api/v1/projects/statistics | com.umc.product.project.adapter.in.web.ProjectStatisticsQueryController#getStatistics | REST | project-statistics:read-chapter | statistics | DIRECT |
| rest:GET /api/v1/projects/statistics/matchings | com.umc.product.project.adapter.in.web.ProjectStatisticsQueryController#getPublicMatchingStatistics | REST | project-statistics:read-public-matching | statistics | PUBLIC |
| rest:GET /api/v1/projects/{projectId} | com.umc.product.project.adapter.in.web.ProjectQueryController#getDetail | REST | project:read | project-resource | RESOURCE |
| rest:GET /api/v1/projects/{projectId}/application-form | com.umc.product.project.adapter.in.web.ProjectApplicationFormController#get | REST | project-form:read | form | RESOURCE |
| rest:GET /api/v1/projects/{projectId}/applications | com.umc.product.project.adapter.in.web.ProjectApplicationQueryController#getProjectApplicants | REST | project-application:list-project | application-scope | DIRECT |
| rest:GET /api/v1/projects/{projectId}/applications/{applicationId} | com.umc.product.project.adapter.in.web.ProjectApplicationQueryController#getApplicationDetail | REST | project-application:read | application-resource | RESOURCE |
| rest:GET /api/v1/projects/{projectId}/members | com.umc.product.project.adapter.in.web.ProjectQueryController#getMembers | REST | project-member:list | project-resource | RESOURCE |
| rest:GET /api/v1/projects/{projectId}/statistics | com.umc.product.project.adapter.in.web.ProjectStatisticsQueryController#getProjectStatistics | REST | project-statistics:read-project | statistics | DIRECT |
| rest:PATCH /api/v1/project/matching-rounds/{matchingRoundId} | com.umc.product.project.adapter.in.web.ProjectMatchingRoundController#update | REST | project-matching-round:update | matching-round | DIRECT |
| rest:PATCH /api/v1/projects/{projectId} | com.umc.product.project.adapter.in.web.ProjectCommandController#update | REST | project:update-info | project-resource | RESOURCE |
| rest:PATCH /api/v1/projects/{projectId}/applications/{applicationId}/decision | com.umc.product.project.adapter.in.web.ProjectApplicationController#decide | REST | project-application:decide | application-resource | DIRECT |
| rest:PATCH /api/v1/projects/{projectId}/members/{memberId}/status | com.umc.product.project.adapter.in.web.ProjectCommandController#changeMemberStatus | REST | project-member:change-status | project-resource | RESOURCE |
| rest:POST /api/v1/project/matching-rounds | com.umc.product.project.adapter.in.web.ProjectMatchingRoundController#create | REST | project-matching-round:create | matching-round | DIRECT |
| rest:POST /api/v1/project/matching-rounds/{matchingRoundId}/auto-decide | com.umc.product.project.adapter.in.web.ProjectMatchingRoundController#autoDecide | REST | project-matching-round:human-auto-decide | matching-round | DIRECT |
| rest:POST /api/v1/projects | com.umc.product.project.adapter.in.web.ProjectCommandController#createDraft | REST | project:create | project-resource | DIRECT |
| rest:POST /api/v1/projects/{projectId}/abort | com.umc.product.project.adapter.in.web.ProjectCommandController#abort | REST | project:abort | project-resource | RESOURCE |
| rest:POST /api/v1/projects/{projectId}/applications | com.umc.product.project.adapter.in.web.ProjectApplicationController#createDraft | REST | project-application:create | application-resource | RESOURCE |
| rest:POST /api/v1/projects/{projectId}/applications/{applicationId}/submit | com.umc.product.project.adapter.in.web.ProjectApplicationController#submit | REST | project-application:submit | application-resource | RESOURCE |
| rest:POST /api/v1/projects/{projectId}/members | com.umc.product.project.adapter.in.web.ProjectCommandController#addMember | REST | project-member:add | project-resource | RESOURCE |
| rest:POST /api/v1/projects/{projectId}/publish | com.umc.product.project.adapter.in.web.ProjectCommandController#publish | REST | project:publish | project-resource | RESOURCE |
| rest:POST /api/v1/projects/{projectId}/submit | com.umc.product.project.adapter.in.web.ProjectCommandController#submit | REST | project:submit-review | project-resource | RESOURCE |
| rest:POST /api/v1/projects/{projectId}/transfer-ownership | com.umc.product.project.adapter.in.web.ProjectCommandController#transferOwnership | REST | project:transfer-ownership | project-resource | RESOURCE |
| rest:PUT /api/v1/projects/{projectId}/application-form | com.umc.product.project.adapter.in.web.ProjectApplicationFormController#upsert | REST | project-form:update | form | RESOURCE |
| rest:PUT /api/v1/projects/{projectId}/applications/{applicationId} | com.umc.product.project.adapter.in.web.ProjectApplicationController#updateDraft | REST | project-application:update | application-resource | RESOURCE |
| rest:PUT /api/v1/projects/{projectId}/part-quotas | com.umc.product.project.adapter.in.web.ProjectCommandController#updatePartQuotas | REST | project:update-part-quota | project-resource | RESOURCE |
| scheduler:matching-round-deadline | com.umc.product.project.adapter.in.scheduler.MatchingRoundDeadlineHandler#handle | SCHEDULER | project-matching-round:system-auto-decide | matching-round | SYSTEM |

## Compiled Target Policy

- schemaVersion: `1.0`
- contextSchemaVersion: `project-1.0`
- policyVersion: `1.1.0`
- policyFingerprint: `7cb5dd0453429ae766788952ae1972c9e24ae2c8f137dfa983a8bbe28a3f5e2e`

| Module | Statement | Effect | Actions | Outcomes |
|---|---|---|---|---|
| application-resource | application.cancel.applicant | ALLOW | project-application:cancel |  |
| application-resource | application.create.challenger | ALLOW | project-application:create |  |
| application-resource | application.decide.owner | ALLOW | project-application:decide |  |
| application-resource | application.decide.super-admin | ALLOW | project-application:decide | application.forceDecision |
| application-resource | application.edit.applicant-draft | ALLOW | project-application:submit, project-application:update |  |
| application-resource | application.read.applicant | ALLOW | project-application:read |  |
| application-resource | application.read.nondraft-central | ALLOW | project-application:read |  |
| application-resource | application.read.nondraft-chapter | ALLOW | project-application:read |  |
| application-resource | application.read.nondraft-owner | ALLOW | project-application:read |  |
| application-resource | application.read.nondraft-sub-pm | ALLOW | project-application:read |  |
| application-resource | application.read.nondraft-super-admin | ALLOW | project-application:read |  |
| application-resource | application.read.super-admin-draft | ALLOW | project-application:read |  |
| application-scope | application.list-management.central | ALLOW | project-application:list-management | application.scope.gisuIds |
| application-scope | application.list-management.chapter | ALLOW | project-application:list-management | application.scope.chapterIds |
| application-scope | application.list-management.super-admin | ALLOW | project-application:list-management | application.scope.all |
| application-scope | application.list-project.central | ALLOW | project-application:list-project, project-application:list-project-batch | application.includeOngoingRounds, application.scope.projectIds |
| application-scope | application.list-project.chapter | ALLOW | project-application:list-project, project-application:list-project-batch | application.scope.projectIds |
| application-scope | application.list-project.owner | ALLOW | project-application:list-project, project-application:list-project-batch | application.scope.projectIds |
| application-scope | application.list-project.sub-pm | ALLOW | project-application:list-project, project-application:list-project-batch | application.scope.projectIds |
| application-scope | application.list-project.super-admin | ALLOW | project-application:list-project, project-application:list-project-batch | application.includeOngoingRounds, application.scope.projectIds |
| application-scope | application.list-self.owner | ALLOW | project-application:list-self | application.scope.ownerMemberIds |
| form | form.read.applicant | ALLOW | project-form:read | form.view |
| form | form.read.central | ALLOW | project-form:read | form.view |
| form | form.read.chapter | ALLOW | project-form:read | form.view |
| form | form.read.owner | ALLOW | project-form:read | form.view |
| form | form.read.super-admin | ALLOW | project-form:read | form.view |
| form | form.update.active-central | ALLOW | project-form:update |  |
| form | form.update.active-chapter | ALLOW | project-form:update |  |
| form | form.update.active-owner | ALLOW | project-form:update |  |
| form | form.update.active-super-admin | ALLOW | project-form:update |  |
| form | form.update.draft-creator | ALLOW | project-form:update |  |
| matching-round | matching.auto-decide.scheduler | ALLOW | project-matching-round:system-auto-decide |  |
| matching-round | matching.list.member | ALLOW | project-matching-round:list |  |
| matching-round | matching.manage.central | ALLOW | project-matching-round:create, project-matching-round:delete, project-matching-round:human-auto-decide, project-matching-round:update |  |
| matching-round | matching.manage.chapter | ALLOW | project-matching-round:create, project-matching-round:delete, project-matching-round:human-auto-decide, project-matching-round:update |  |
| matching-round | matching.manage.super-admin | ALLOW | project-matching-round:create, project-matching-round:delete, project-matching-round:human-auto-decide, project-matching-round:update |  |
| project-resource | project.abort.central | ALLOW | project:abort |  |
| project-resource | project.abort.chapter | ALLOW | project:abort |  |
| project-resource | project.abort.super-admin | ALLOW | project:abort |  |
| project-resource | project.capability-list.member | ALLOW | project:capability-list |  |
| project-resource | project.create.central-core | ALLOW | project:create |  |
| project-resource | project.create.chapter-president | ALLOW | project:create |  |
| project-resource | project.create.plan-challenger | ALLOW | project:create |  |
| project-resource | project.create.school-core | ALLOW | project:create |  |
| project-resource | project.create.super-admin | ALLOW | project:create |  |
| project-resource | project.delete.central | ALLOW | project:delete |  |
| project-resource | project.delete.chapter | ALLOW | project:delete |  |
| project-resource | project.delete.owner | ALLOW | project:delete |  |
| project-resource | project.delete.super-admin | ALLOW | project:delete |  |
| project-resource | project.edit.active-central | ALLOW | project-member:add, project-member:change-status, project-member:remove, project:transfer-ownership, project:update-info |  |
| project-resource | project.edit.active-chapter | ALLOW | project-member:add, project-member:change-status, project-member:remove, project:transfer-ownership, project:update-info |  |
| project-resource | project.edit.active-owner | ALLOW | project-member:add, project-member:change-status, project-member:remove, project:transfer-ownership, project:update-info |  |
| project-resource | project.edit.active-super-admin | ALLOW | project-member:add, project-member:change-status, project-member:remove, project:transfer-ownership, project:update-info |  |
| project-resource | project.edit.draft-creator | ALLOW | project-member:add, project-member:change-status, project-member:remove, project:transfer-ownership, project:update-info |  |
| project-resource | project.member-batch.member | ALLOW | project-member:batch |  |
| project-resource | project.publish.central | ALLOW | project:publish |  |
| project-resource | project.publish.chapter | ALLOW | project:publish |  |
| project-resource | project.publish.super-admin | ALLOW | project:publish |  |
| project-resource | project.quota.central | ALLOW | project:update-part-quota |  |
| project-resource | project.quota.chapter | ALLOW | project:update-part-quota |  |
| project-resource | project.quota.super-admin | ALLOW | project:update-part-quota |  |
| project-resource | project.read.central-review | ALLOW | project-member:list, project:read |  |
| project-resource | project.read.chapter-review | ALLOW | project-member:list, project:read |  |
| project-resource | project.read.owner-private | ALLOW | project-member:list, project:read |  |
| project-resource | project.read.public | ALLOW | project-member:list, project:read |  |
| project-resource | project.read.super-admin-draft | ALLOW | project-member:list, project:read |  |
| project-resource | project.read.super-admin-review | ALLOW | project-member:list, project:read |  |
| project-resource | project.submit-review.draft-creator | ALLOW | project:submit-review |  |
| project-scope | project.list-managed.central-gisu | ALLOW | project:list-managed | project.scope.gisuIds |
| project-scope | project.list-managed.chapter-president | ALLOW | project:list-managed | project.scope.chapterIds |
| project-scope | project.list-managed.owner | ALLOW | project:list-managed | project.scope.includeOwnDrafts, project.scope.ownerMemberIds |
| project-scope | project.list-managed.school-core | ALLOW | project:list-managed | project.scope.chapterIds |
| project-scope | project.list-managed.super-admin-all | ALLOW | project:list-managed | project.scope.all |
| project-scope | project.list-own-drafts.owner | ALLOW | project:list-own-drafts | project.scope.includeOwnDrafts, project.scope.ownerMemberIds |
| project-scope | project.list-public.central-gisu | ALLOW | project:list-public | project.scope.gisuIds |
| project-scope | project.list-public.chapter-president | ALLOW | project:list-public | project.scope.chapterIds |
| project-scope | project.list-public.member-public | ALLOW | project:list-public | project.scope.publicOnly |
| project-scope | project.list-public.super-admin-all | ALLOW | project:list-public | project.scope.all |
| statistics | statistics.chapter.central | ALLOW | project-statistics:read-chapter |  |
| statistics | statistics.chapter.chapter | ALLOW | project-statistics:read-chapter |  |
| statistics | statistics.chapter.school-core | ALLOW | project-statistics:read-chapter |  |
| statistics | statistics.chapter.super-admin | ALLOW | project-statistics:read-chapter |  |
| statistics | statistics.project.central | ALLOW | project-statistics:read-project |  |
| statistics | statistics.project.chapter | ALLOW | project-statistics:read-project |  |
| statistics | statistics.project.owner | ALLOW | project-statistics:read-project |  |
| statistics | statistics.project.school-core | ALLOW | project-statistics:read-project |  |
| statistics | statistics.project.sub-pm | ALLOW | project-statistics:read-project |  |
| statistics | statistics.project.super-admin | ALLOW | project-statistics:read-project |  |
| statistics | statistics.public-matching.member | ALLOW | project-statistics:read-public-matching |  |
