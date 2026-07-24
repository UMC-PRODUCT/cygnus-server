# Certificate Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `certificate-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `4b916e60cf047c925aab1719a0db88ed5ed0c3cbcfbb8edd4c48752a7466cf95`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:PATCH /api/v1/certificates/admin/{certificateId}/revoke | com.umc.product.certificate.adapter.in.web.AdminCertificateController#revoke | REST | certificate:revoke | certificate-resource | DIRECT |
| rest:POST /api/v1/certificates/admin | com.umc.product.certificate.adapter.in.web.AdminCertificateController#issue | REST | certificate:issue-admin | certificate-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| certificate-resource | certificate.manage.active-central-core-in-target-gisu | ALLOW | certificate:issue-admin, certificate:revoke | EQ(ATTRIBUTE(relation.activeCentralCoreInTargetGisu), true) |  |
| certificate-resource | certificate.manage.super-admin | ALLOW | certificate:issue-admin, certificate:revoke | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
