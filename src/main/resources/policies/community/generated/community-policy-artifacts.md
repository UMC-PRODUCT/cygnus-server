# Community Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `community-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `f1acc2553aa4df302a4909a03b64eed8f5f6d2c63425fd66173e999d850834a6`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| capability:ResourceType.COMMUNITY_COMMENT | com.umc.product.community.application.service.evaluator.CommunityCommentPermissionEvaluator#evaluate | INTERNAL_BATCH | community-comment:read | community-resource | CAPABILITY |
| capability:ResourceType.COMMUNITY_POST | com.umc.product.community.application.service.evaluator.CommunityPostPermissionEvaluator#evaluate | INTERNAL_BATCH | community-post:read | community-resource | CAPABILITY |
| rest:DELETE /api/v1/posts/{postId} | com.umc.product.community.adapter.in.web.PostController#deletePost | REST | community-post:delete | community-resource | RESOURCE |
| rest:DELETE /api/v1/posts/{postId}/comments/{commentId} | com.umc.product.community.adapter.in.web.CommentController#deleteComment | REST | community-comment:delete | community-resource | RESOURCE |
| rest:PATCH /api/v1/posts/{postId} | com.umc.product.community.adapter.in.web.PostController#updatePost | REST | community-post:update | community-resource | RESOURCE |
| rest:PATCH /api/v1/posts/{postId}/lightning | com.umc.product.community.adapter.in.web.PostController#updateLightningPost | REST | community-post:update | community-resource | RESOURCE |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| community-resource | community.content.active-central-delete | ALLOW | community-comment:delete, community-post:delete | EQ(ATTRIBUTE(relation.activeCentralCore), true) |  |
| community-resource | community.content.authenticated-read | ALLOW | community-comment:read, community-post:read | EQ(ATTRIBUTE(relation.authenticated), true) |  |
| community-resource | community.content.author-delete | ALLOW | community-comment:delete, community-post:delete | EQ(ATTRIBUTE(relation.isAuthor), true) |  |
| community-resource | community.content.author-update | ALLOW | community-comment:update, community-post:update | EQ(ATTRIBUTE(relation.isAuthor), true) |  |
| community-resource | community.content.legacy-write | ALLOW | community-comment:write, community-post:write | EQ(ATTRIBUTE(relation.authorHasChallengerHistory), true) |  |
| community-resource | community.content.super-admin-delete | ALLOW | community-comment:delete, community-post:delete | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
