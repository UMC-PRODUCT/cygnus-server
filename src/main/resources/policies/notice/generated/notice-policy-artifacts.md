# Notice Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `notice-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `f5e781b1a969179e54f6a5becd463ba9f2dddd35c2cf36b0a8012cda65dd9d05`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:DELETE /api/v1/notices/{noticeId} | com.umc.product.notice.adapter.in.web.NoticeCommandController#deleteNotice | REST | notice:delete | notice-resource | DIRECT |
| rest:GET /api/v1/notices/{noticeId} | com.umc.product.notice.adapter.in.web.NoticeQueryController#getNotice | REST | notice:read | notice-resource | DIRECT |
| rest:GET /api/v1/notices/{noticeId}/read-statics | com.umc.product.notice.adapter.in.web.NoticeQueryController#getNoticeReadStatics | REST | notice:check-recipients | notice-resource | DIRECT |
| rest:GET /api/v1/notices/{noticeId}/read-status | com.umc.product.notice.adapter.in.web.NoticeQueryController#getNoticeReadStatus | REST | notice:check-recipients | notice-resource | DIRECT |
| rest:PATCH /api/v1/notices/{noticeId} | com.umc.product.notice.adapter.in.web.NoticeCommandController#updateNotice | REST | notice:update | notice-resource | DIRECT |
| rest:POST /api/v1/notices | com.umc.product.notice.adapter.in.web.NoticeCommandController#createNotice | REST | notice:create | notice-resource | DIRECT |
| rest:POST /api/v1/notices/{noticeId}/read | com.umc.product.notice.adapter.in.web.NoticeCommandController#recordNoticeRead | REST | notice:read | notice-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| notice-resource | notice.create.active-target-creator | ALLOW | notice:create | EQ(ATTRIBUTE(relation.activeCreatorForTarget), true) |  |
| notice-resource | notice.read.active-central-core | ALLOW | notice:read | EQ(ATTRIBUTE(relation.activeCentralCore), true) |  |
| notice-resource | notice.read.active-role-target | ALLOW | notice:read | EQ(ATTRIBUTE(relation.activeRoleCanReadTarget), true) |  |
| notice-resource | notice.read.target-challenger | ALLOW | notice:read | EQ(ATTRIBUTE(relation.isTargetChallenger), true) |  |
| notice-resource | notice.recipients.active-manager | ALLOW | notice:check-recipients, notice:read-recipients | EQ(ATTRIBUTE(relation.activeManagerForTarget), true) |  |
| notice-resource | notice.update-delete.author | ALLOW | notice:delete, notice:update | EQ(ATTRIBUTE(relation.isNoticeAuthor), true) |  |
| notice-resource | notice.update-delete.super-admin | ALLOW | notice:delete, notice:update | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
