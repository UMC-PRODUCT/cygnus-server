# Term Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `term-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `dc949f4a6b13b9f5c515c823e22efe9bb8eac2e0466c31d6017baa28ff661867`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:POST /api/v1/terms | com.umc.product.term.adapter.in.web.TermController#createTerms | REST | term:create | term-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| term-resource | term.create.super-admin | ALLOW | term:create | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
