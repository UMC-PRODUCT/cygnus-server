# Notification Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `notification-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `379981e6f68e56b90255e0a653e6d25db3cfeedd7201853a21ced64c1ce8050a`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| capability:ResourceType.FCM/DELETE | com.umc.product.notification.application.service.evaluator.FcmPermissionEvaluator#evaluate | INTERNAL_BATCH | notification:delete-token | notification-resource | TRANSITIVE |
| rest:DELETE /api/v1/notifications/fcm/installations/{installationId} | com.umc.product.notification.adapter.in.web.FcmController#unregisterFcmInstallation | REST | notification:delete-token | notification-resource | DIRECT |
| rest:POST /api/v1/notifications/admin/fcm/messages | com.umc.product.notification.adapter.in.web.FcmAdminController#send | REST | notification:send-fcm | notification-resource | ACTOR |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| notification-resource | notification.delete-token.active-central-core | ALLOW | notification:delete-token | EQ(ATTRIBUTE(relation.activeCentralCore), true) |  |
| notification-resource | notification.delete-token.owner | ALLOW | notification:delete-token | EQ(ATTRIBUTE(relation.isTokenOwner), true) |  |
| notification-resource | notification.send-fcm.active-central-core | ALLOW | notification:send-fcm | EQ(ATTRIBUTE(relation.activeCentralCore), true) |  |
