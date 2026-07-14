# Project Authorization Rollout Runbook

이 문서는 Project policy engine의 배포·관찰·rollback 절차를 정의한다. 자동 ENFORCE 전환은 없으며,
현재 배포 artifact는 `SHADOW`, 빈 receipt, ENFORCE action 0개 상태다. 24시간 및 7일 조건은 운영에서
실제로 관찰한 뒤 별도 승인을 받아야 하며, 이 문서는 해당 조건을 충족했다고 주장하지 않는다.

## Machine-readable rollout contract

아래 JSON은 테스트가 직접 읽는 운영 계약이다.

```json
{
  "schemaVersion": "project-rollout-runbook-1.0",
  "defaultMode": "SHADOW",
  "receiptResource": "policies/project/rollout/enforcement-receipts.json",
  "metrics": {
    "decision": "project.authorization.decision.total",
    "evaluation": "project.authorization.evaluation.seconds",
    "prometheusDecision": "project_authorization_decision_total",
    "prometheusEvaluationBucket": "project_authorization_evaluation_seconds_bucket",
    "tags": [
      "action",
      "mode",
      "classification",
      "schemaVersion",
      "contextSchemaVersion",
      "policyVersion",
      "policyFingerprint",
      "evaluationPoint"
    ]
  },
  "promotion": {
    "observationHours": 24,
    "unexpectedDifferenceMax": 0,
    "targetFailureMax": 0,
    "unauthorizedExpansionMax": 0,
    "p95IncreasePercentMax": 10,
    "legacyRetentionDaysAfterFullEnforce": 7,
    "automaticEnforceTransition": false
  },
  "currentShippedState": {
    "mode": "SHADOW",
    "receiptCount": 0,
    "enforcedActionCount": 0,
    "observationSatisfied": false,
    "legacyRemovalEligible": false
  },
  "receiptContract": {
    "fields": [
      "policyVersion",
      "policyFingerprint",
      "artifactSha256",
      "wave",
      "actions",
      "approver",
      "approvedAt",
      "expiresAt"
    ],
    "wildcardAllowed": false,
    "externalPathAllowed": false,
    "expiryRequired": true,
    "staleReceiptRejected": true,
    "expiryValidation": "STARTUP_ADMISSION_ONLY",
    "cryptographicallySigned": false
  },
  "rollback": {
    "startupCompileFailureAction": "PREVIOUS_IMAGE_ROLLBACK",
    "behaviorRegressionAction": "CONFIG_ROLLBACK_TO_SHADOW_OR_LEGACY",
    "owner": "Project backend on-call and authorization policy owner",
    "configCommand": "kubectl -n umc-product set env deployment/umc-product-server SPRING_APPLICATION_JSON='{\"app\":{\"project\":{\"authorization-rollout\":{\"default-mode\":\"SHADOW\",\"action-overrides\":[]}}}}'",
    "previousImageCommand": "kubectl -n umc-product set image deployment/umc-product-server app=<previous-image-reference>"
  },
  "queries": {
    "expectedDifference": "sum(increase(project_authorization_decision_total{classification=\"EXPECTED_DIFFERENCE\"}[24h]))",
    "unexpectedDifference": "sum(increase(project_authorization_decision_total{classification=\"UNEXPECTED_DIFFERENCE\"}[24h]))",
    "targetFailure": "sum(increase(project_authorization_decision_total{classification=\"TARGET_FAILURE\"}[24h]))",
    "p95": "histogram_quantile(0.95, sum by (le,action,evaluationPoint) (rate(project_authorization_evaluation_seconds_bucket[5m])))",
    "unauthorizedExpansionLog": "sum(count_over_time({app=\"umc-product-server\"} | json | message=\"project_authorization_evaluated\" | targetPrivilegeExpansion=\"YES\" | classification!=\"EXPECTED_DIFFERENCE\" [24h]))"
  },
  "waves": [
    {
      "wave": "READ_CAPABILITY",
      "actions": [
        "project:read",
        "project-member:list",
        "project-member:batch",
        "project-application:read",
        "project-form:read",
        "project:capability-list"
      ]
    },
    {
      "wave": "LIST_GRAPHQL",
      "actions": [
        "project:list-public",
        "project:list-managed",
        "project:list-own-drafts",
        "project-application:list-self",
        "project-application:list-project-batch",
        "project-application:list-project",
        "project-application:list-management"
      ]
    },
    {
      "wave": "COMMAND_STATISTICS",
      "actions": [
        "project:create",
        "project:update-info",
        "project:submit-review",
        "project:transfer-ownership",
        "project-member:add",
        "project:publish",
        "project:update-part-quota",
        "project:delete",
        "project:abort",
        "project-member:remove",
        "project-member:change-status",
        "project-application:create",
        "project-application:update",
        "project-application:submit",
        "project-application:decide",
        "project-application:cancel",
        "project-form:update",
        "project-statistics:read-project",
        "project-statistics:read-chapter",
        "project-statistics:read-public-matching"
      ]
    },
    {
      "wave": "MATCHING_SCHEDULER",
      "actions": [
        "project-matching-round:list",
        "project-matching-round:create",
        "project-matching-round:update",
        "project-matching-round:delete",
        "project-matching-round:human-auto-decide",
        "project-matching-round:system-auto-decide"
      ]
    }
  ]
}
```

## Receipt와 startup gate

receipt 경로는 `policies/project/rollout/enforcement-receipts.json`으로 고정된다. 환경변수, URL, 요청 값,
외부 파일 경로로 바꿀 수 없다. 파일이 존재하면 mode와 무관하게 strict parse하며, unknown/duplicate/null,
scalar coercion, float, comment, trailing comma, single quote, wildcard/unknown/duplicate action을 거부한다.

각 receipt는 정확히 한 wave의 전체 action set을 가져야 한다. `policyVersion`, `policyFingerprint`,
`artifactSha256`은 startup에 compile한 bundle과 고정 generated artifact bytes에 정확히 일치해야 한다.
시간 조건은 `approvedAt <= now < expiresAt`이며 future approval, 역전된 구간, 만료된 stale receipt를 거부한다.
동일 wave 중복과 wave 간 action overlap도 거부한다.

현재 shipped receipt는 다음과 같고, 따라서 ENFORCE는 startup에서 fail-closed한다.

```json
{
  "schemaVersion": "1.0",
  "receipts": []
}
```

receipt가 승인한 action은 ENFORCE 가능한 상한이다. 설정 rollback으로 일부 또는 전체 action을 SHADOW/LEGACY로
내리는 것은 허용한다. receipt가 승인하지 않은 action을 ENFORCE하는 것은 허용하지 않는다.

`expiresAt`은 pod startup 시 ENFORCE 진입을 허용할지 판단하는 admission 경계다. 실행 중인 pod를 매 요청마다
재검증하는 lease가 아니므로, pod가 시작한 뒤 receipt가 만료되어도 자동으로 SHADOW로 바뀌지 않는다. 긴급 회수는
action override 또는 default mode를 SHADOW/LEGACY로 내리고 rollout한 뒤, receipt를 제거하거나 교체한 새 artifact를
배포한다. 만료된 receipt를 가진 새 pod는 startup에서 거부된다.

receipt는 source-controlled classpath JSON이며 암호학적으로 서명되지 않는다. `approver`는 감사용 label이지 runtime이
인증한 신원이 아니다. 따라서 source/release pipeline 쓰기 권한을 가진 공격자가 policy, generated artifact, receipt를 함께
변조할 위험은 runtime gate만으로 막을 수 없다. branch protection, required code-owner review, CI artifact provenance와 배포
권한 분리가 전제다. Runtime gate는 승인된 exact action set과 현재 artifact의 일관성을 검증하지만 승인자의 신원을 증명하지
않는다.

## Wave별 receipt 예시

아래 값의 approver와 시간은 예시다. 실제 반영 시 검토자, 배포 시각, 만료 시각을 승인 기록에서 가져오고,
현재 compile 결과인 `policyVersion=1.1.0`, fingerprint와 artifact SHA를 재검증한다.

### READ_CAPABILITY

```json
{
  "policyVersion": "1.1.0",
  "policyFingerprint": "7cb5dd0453429ae766788952ae1972c9e24ae2c8f137dfa983a8bbe28a3f5e2e",
  "artifactSha256": "2dd6403da68c43d3ccd8a864fe50f8d597a513f4c3f7080948ae29ac29205564",
  "wave": "READ_CAPABILITY",
  "actions": ["project:read", "project-member:list", "project-member:batch", "project-application:read", "project-form:read", "project:capability-list"],
  "approver": "authorization-policy-owner@umc.example",
  "approvedAt": "2026-07-14T00:00:00Z",
  "expiresAt": "2026-07-15T00:00:00Z"
}
```

### LIST_GRAPHQL

```json
{
  "policyVersion": "1.1.0",
  "policyFingerprint": "7cb5dd0453429ae766788952ae1972c9e24ae2c8f137dfa983a8bbe28a3f5e2e",
  "artifactSha256": "2dd6403da68c43d3ccd8a864fe50f8d597a513f4c3f7080948ae29ac29205564",
  "wave": "LIST_GRAPHQL",
  "actions": ["project:list-public", "project:list-managed", "project:list-own-drafts", "project-application:list-self", "project-application:list-project-batch", "project-application:list-project", "project-application:list-management"],
  "approver": "authorization-policy-owner@umc.example",
  "approvedAt": "2026-07-14T00:00:00Z",
  "expiresAt": "2026-07-15T00:00:00Z"
}
```

### COMMAND_STATISTICS

```json
{
  "policyVersion": "1.1.0",
  "policyFingerprint": "7cb5dd0453429ae766788952ae1972c9e24ae2c8f137dfa983a8bbe28a3f5e2e",
  "artifactSha256": "2dd6403da68c43d3ccd8a864fe50f8d597a513f4c3f7080948ae29ac29205564",
  "wave": "COMMAND_STATISTICS",
  "actions": ["project:create", "project:update-info", "project:submit-review", "project:transfer-ownership", "project-member:add", "project:publish", "project:update-part-quota", "project:delete", "project:abort", "project-member:remove", "project-member:change-status", "project-application:create", "project-application:update", "project-application:submit", "project-application:decide", "project-application:cancel", "project-form:update", "project-statistics:read-project", "project-statistics:read-chapter", "project-statistics:read-public-matching"],
  "approver": "authorization-policy-owner@umc.example",
  "approvedAt": "2026-07-14T00:00:00Z",
  "expiresAt": "2026-07-15T00:00:00Z"
}
```

### MATCHING_SCHEDULER

```json
{
  "policyVersion": "1.1.0",
  "policyFingerprint": "7cb5dd0453429ae766788952ae1972c9e24ae2c8f137dfa983a8bbe28a3f5e2e",
  "artifactSha256": "2dd6403da68c43d3ccd8a864fe50f8d597a513f4c3f7080948ae29ac29205564",
  "wave": "MATCHING_SCHEDULER",
  "actions": ["project-matching-round:list", "project-matching-round:create", "project-matching-round:update", "project-matching-round:delete", "project-matching-round:human-auto-decide", "project-matching-round:system-auto-decide"],
  "approver": "authorization-policy-owner@umc.example",
  "approvedAt": "2026-07-14T00:00:00Z",
  "expiresAt": "2026-07-15T00:00:00Z"
}
```

## Wave별 exact override 예시

각 예시는 base mode SHADOW에서 해당 wave만 ENFORCE한다. 실제 배포에서는 동일 wave receipt를 먼저 artifact에
포함하고 startup gate를 통과해야 한다.

### READ_CAPABILITY override

```yaml
app:
  project:
    authorization-rollout:
      default-mode: SHADOW
      action-overrides:
        - { action: "project:read", mode: ENFORCE }
        - { action: "project-member:list", mode: ENFORCE }
        - { action: "project-member:batch", mode: ENFORCE }
        - { action: "project-application:read", mode: ENFORCE }
        - { action: "project-form:read", mode: ENFORCE }
        - { action: "project:capability-list", mode: ENFORCE }
```

### LIST_GRAPHQL override

```yaml
app:
  project:
    authorization-rollout:
      default-mode: SHADOW
      action-overrides:
        - { action: "project:list-public", mode: ENFORCE }
        - { action: "project:list-managed", mode: ENFORCE }
        - { action: "project:list-own-drafts", mode: ENFORCE }
        - { action: "project-application:list-self", mode: ENFORCE }
        - { action: "project-application:list-project-batch", mode: ENFORCE }
        - { action: "project-application:list-project", mode: ENFORCE }
        - { action: "project-application:list-management", mode: ENFORCE }
```

### COMMAND_STATISTICS override

```yaml
app:
  project:
    authorization-rollout:
      default-mode: SHADOW
      action-overrides:
        - { action: "project:create", mode: ENFORCE }
        - { action: "project:update-info", mode: ENFORCE }
        - { action: "project:submit-review", mode: ENFORCE }
        - { action: "project:transfer-ownership", mode: ENFORCE }
        - { action: "project-member:add", mode: ENFORCE }
        - { action: "project:publish", mode: ENFORCE }
        - { action: "project:update-part-quota", mode: ENFORCE }
        - { action: "project:delete", mode: ENFORCE }
        - { action: "project:abort", mode: ENFORCE }
        - { action: "project-member:remove", mode: ENFORCE }
        - { action: "project-member:change-status", mode: ENFORCE }
        - { action: "project-application:create", mode: ENFORCE }
        - { action: "project-application:update", mode: ENFORCE }
        - { action: "project-application:submit", mode: ENFORCE }
        - { action: "project-application:decide", mode: ENFORCE }
        - { action: "project-application:cancel", mode: ENFORCE }
        - { action: "project-form:update", mode: ENFORCE }
        - { action: "project-statistics:read-project", mode: ENFORCE }
        - { action: "project-statistics:read-chapter", mode: ENFORCE }
        - { action: "project-statistics:read-public-matching", mode: ENFORCE }
```

### MATCHING_SCHEDULER override

```yaml
app:
  project:
    authorization-rollout:
      default-mode: SHADOW
      action-overrides:
        - { action: "project-matching-round:list", mode: ENFORCE }
        - { action: "project-matching-round:create", mode: ENFORCE }
        - { action: "project-matching-round:update", mode: ENFORCE }
        - { action: "project-matching-round:delete", mode: ENFORCE }
        - { action: "project-matching-round:human-auto-decide", mode: ENFORCE }
        - { action: "project-matching-round:system-auto-decide", mode: ENFORCE }
```

## 관측과 promotion gate

Micrometer 이름과 Prometheus 이름은 다음과 같이 대응한다.

| Micrometer | Prometheus |
| --- | --- |
| `project.authorization.decision.total` | `project_authorization_decision_total` |
| `project.authorization.evaluation.seconds` | `project_authorization_evaluation_seconds`, `project_authorization_evaluation_seconds_bucket` |

decision/timer의 tag는 `action`, `mode`, `classification`, `schemaVersion`, `contextSchemaVersion`,
`policyVersion`, `policyFingerprint`, `evaluationPoint` 여덟 개뿐이다. `legacyEffect`, `targetEffect`,
`targetPrivilegeExpansion`, failure code, evaluatedAt은 PII 없는 structured log에만 기록한다.

운영 대시보드에서 다음 쿼리를 사용한다.

```promql
sum(increase(project_authorization_decision_total{classification="EXPECTED_DIFFERENCE"}[24h]))
```

```promql
sum(increase(project_authorization_decision_total{classification="UNEXPECTED_DIFFERENCE"}[24h]))
```

```promql
sum(increase(project_authorization_decision_total{classification="TARGET_FAILURE"}[24h]))
```

```promql
histogram_quantile(0.95, sum by (le, action, evaluationPoint) (rate(project_authorization_evaluation_seconds_bucket[5m])))
```

승인되지 않은 target 권한 확대는 bounded metric tag를 추가하지 않고 log에서 확인한다.

```logql
sum(count_over_time({app="umc-product-server"} | json | message="project_authorization_evaluated" | targetPrivilegeExpansion="YES" | classification!="EXPECTED_DIFFERENCE" [24h]))
```

각 wave는 실제 24시간 관찰 구간 전체에서 다음을 모두 만족해야 다음 승인을 요청할 수 있다.

1. `UNEXPECTED_DIFFERENCE = 0`
2. `TARGET_FAILURE = 0`
3. 승인되지 않은 `targetPrivilegeExpansion = YES`가 0
4. action/evaluationPoint별 p95가 동일 트래픽 baseline 대비 10% 이하 증가

전체 action이 ENFORCE된 뒤에도 7일 동안 같은 조건을 관찰해야 legacy adapter 제거 후보가 된다. 그때 별도 변경에서
legacy adapter, 직접 role check, compatibility mapping을 제거한다. 현재는 observation과 retention 조건 모두
충족되지 않은 상태다.

Matching-round DB compatibility 제거에서는
`trg_project_matching_round_legacy_gisu`와 `fill_project_matching_round_gisu_for_legacy_writer()`만 제거한다.
기간 및 application scope의 모든 쓰기 방향을 지키는 `trg_project_matching_round_period`,
`trg_project_application_matching_scope`, `trg_project_parent_application_scope`,
`trg_project_application_form_parent_scope`와 대응 함수는 영구 유지한다. 제거 전후 audit SQL과 세부 절차는
`project-matching-round-gisu-migration.md`를 따른다.

## 현재 역할 모델과 merge risk

현재 Project target의 유일한 규범은 `ChallengerRole` tuple과 연결된 Gisu의
`startAt <= evaluatedAt < endAt` 기간이다. `SUPER_ADMIN`도 이 기간 안에서만 active로 파생한다. 향후 #1049의
`SystemRole` 또는 별도 전역 SUPER_ADMIN 모델은 이 pilot에 통합하지 않는다. 해당 변경을 merge하면 active role의 의미,
scope 전역성, expected-difference 분류가 충돌하거나 권한이 중복 상승할 수 있다. 통합 전에는 별도 설계 검토를 거쳐
context schema version, target JSON, action matrix, shadow expected-difference를 함께 갱신해야 한다.

## Policy version history gate 한계

현재 CI gate는 policy source SHA, compiled AST fingerprint, generated artifact byte SHA가 서로 일치하지 않으면 실패한다.
그러나 PR base artifact를 안정적으로 제공하는 build input이 아직 없으므로, 과거 fingerprint와 현재 fingerprint가 달라졌는데
`policyVersion`을 그대로 둔 변경을 독립적으로 판별하는 history comparison은 구현하지 않았다. 임의의 local Git ref나
network remote에 의존하는 검사는 재현성과 rollback build를 깨뜨릴 수 있어 사용하지 않는다. 이 보완을 도입할 때는 CI가
검증된 base artifact의 `(policyVersion, fingerprint)`를 명시적 input으로 제공하고, current pair와 비교하는 별도 gate로
추가해야 한다. 그 전까지 policy owner review에서 version bump를 필수 확인한다.

## Rollback

Owner는 Project backend on-call과 authorization policy owner다. 다음 중 하나가 발생하면 즉시 rollback한다.

- startup compile/strict receipt/metadata gate failure
- `UNEXPECTED_DIFFERENCE > 0` 또는 `TARGET_FAILURE > 0`
- 승인되지 않은 target privilege expansion
- p95가 baseline 대비 10%를 초과
- HTTP/GraphQL/scheduler behavior regression 또는 side effect 중복

정상 compile된 artifact의 behavior 문제는 config를 SHADOW로 내린다.

```bash
kubectl -n umc-product set env deployment/umc-product-server SPRING_APPLICATION_JSON='{"app":{"project":{"authorization-rollout":{"default-mode":"SHADOW","action-overrides":[]}}}}'
kubectl -n umc-product rollout status deployment/umc-product-server
```

긴급한 특정 action rollback은 동일 action override의 mode를 `SHADOW` 또는 `LEGACY`로 바꾸되, duplicate action 없이
canonical action ID를 사용한다. 유효 receipt는 남아 있어도 rollback mode startup을 막지 않는다.

startup compile, resource duplicate, strict JSON, metadata SHA mismatch처럼 새 pod가 시작하지 못하면 설정 fallback에
의존하지 않고 이전 image로 되돌린다.

```bash
kubectl -n umc-product set image deployment/umc-product-server app=<previous-image-reference>
kubectl -n umc-product rollout status deployment/umc-product-server
```

Kubernetes rollout revision이 검증되어 있다면 다음 명령도 사용할 수 있다.

```bash
kubectl -n umc-product rollout undo deployment/umc-product-server --to-revision=<previous-revision>
kubectl -n umc-product rollout status deployment/umc-product-server
```
