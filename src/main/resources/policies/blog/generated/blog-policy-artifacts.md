# Blog Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `blog-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `28eb119c44808cee9e6021695c64c4f448a695abb47274700dfe22b62edf3dba`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| internal:blog-admin-view | com.umc.product.blog.application.authorization.BlogPolicyAuthorizationService#isSuperAdminViewer | INTERNAL_BATCH | blog:admin-view | blog-resource | TRANSITIVE |
| rest:DELETE /api/v1/blog/contents/{contentId} | com.umc.product.blog.adapter.in.web.BlogContentController#delete | REST | blog-content:delete | blog-resource | RESOURCE |
| rest:DELETE /api/v1/blog/series/{seriesId} | com.umc.product.blog.adapter.in.web.BlogSeriesController#delete | REST | blog-series:delete | blog-resource | RESOURCE |
| rest:DELETE /api/v1/blog/{type}/{slug}/comments/{commentId} | com.umc.product.blog.adapter.in.web.BlogInteractionController#deleteComment | REST | blog-comment:delete | blog-resource | RESOURCE |
| rest:GET /api/v1/blog/contents/{contentId}/preview | com.umc.product.blog.adapter.in.web.BlogContentController#getPreview | REST | blog-content:read | blog-resource | RESOURCE |
| rest:GET /api/v1/blog/series/{seriesId}/preview | com.umc.product.blog.adapter.in.web.BlogSeriesController#getPreview | REST | blog-series:read | blog-resource | RESOURCE |
| rest:PATCH /api/v1/blog/contents/{contentId} | com.umc.product.blog.adapter.in.web.BlogContentController#update | REST | blog-content:update | blog-resource | RESOURCE |
| rest:PATCH /api/v1/blog/series/{seriesId} | com.umc.product.blog.adapter.in.web.BlogSeriesController#update | REST | blog-series:update | blog-resource | RESOURCE |
| rest:PATCH /api/v1/blog/{type}/{slug}/comments/{commentId} | com.umc.product.blog.adapter.in.web.BlogInteractionController#updateComment | REST | blog-comment:update | blog-resource | RESOURCE |
| rest:POST /api/v1/blog/contents | com.umc.product.blog.adapter.in.web.BlogContentController#create | REST | blog-content:create | blog-resource | ACTOR |
| rest:POST /api/v1/blog/series | com.umc.product.blog.adapter.in.web.BlogSeriesController#create | REST | blog-series:create | blog-resource | ACTOR |
| rest:PUT /api/v1/blog/series/{seriesId}/contents | com.umc.product.blog.adapter.in.web.BlogSeriesController#replaceContents | REST | blog-series:update | blog-resource | RESOURCE |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| blog-resource | blog.resource.author | ALLOW | blog-comment:delete, blog-comment:update, blog-content:delete, blog-content:read, blog-content:update, blog-series:delete, blog-series:read, blog-series:update | EQ(ATTRIBUTE(relation.isAuthor), true) |  |
| blog-resource | blog.resource.super-admin | ALLOW | blog-comment:delete, blog-content:create, blog-content:delete, blog-content:read, blog-series:create, blog-series:delete, blog-series:read, blog:admin-view | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
