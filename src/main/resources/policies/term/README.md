# Term JSON Policy

Term 도메인의 actor-facing 권한을 `term-1.0` typed context와 JSON policy로 관리한다.

## Action

| Action | Resource surface | Required attributes | Target |
|---|---|---|---|
| `term:create` | `POST /api/v1/terms` | `relation.superAdmin: BOOLEAN` | `SUPER_ADMIN`만 ALLOW |

`relation.superAdmin`은 외부 요청에서 받지 않는다. 인증된 회원의 서버 측
`SystemRoleType.SUPER_ADMIN` fact로 context builder가 생성한다.

## Policy files

- `bundle.json`: namespace, schema/context/policy version, combining algorithm, module manifest
- `term-resource.policy.json`: `term:create` statement
- `expected-differences.json`: legacy와 target 사이에 승인된 차이. 현재는 비어 있다.
- `rollout/enforcement-receipts.json`: ENFORCE 승인 영수증. 현재는 비어 있다.

모든 파일은 고정된 UTF-8 classpath resource로만 읽으며 애플리케이션 시작 시
`CompiledPolicyRegistry`가 Project bundle과 함께 원자적으로 컴파일한다.

## Evaluation

```text
@CheckAccess(TERM, WRITE)
→ term:create
→ relation.superAdmin fact 생성
→ registry에서 (term, term-1.0) bundle 선택
→ action index의 term:create statement만 평가
→ LEGACY / SHADOW / ENFORCE coordinator
```

SHADOW에서는 기존 evaluator와 JSON target을 같은 `evaluatedAt`으로 각각 한 번
평가하고 기존 결과를 authoritative decision으로 사용한다. Command는 한 번만
실행한다.
