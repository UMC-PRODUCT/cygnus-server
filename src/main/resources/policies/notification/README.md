# Notification policy

Notification의 actor 권한을 `notification-1.0` typed context와 JSON policy로 관리한다.
정책은 classpath의 `bundle.json`과 `notification-resource.policy.json`을 애플리케이션 시작 시
한 번 compile한 뒤 immutable registry에서 action index로 조회한다.

## Action

| Action | Runtime surface | Legacy 판단 | Target 판단 |
|---|---|---|---|
| `notification:send-fcm` | `POST /api/v1/notifications/admin/fcm/messages` | 어느 Gisu든 중앙 총괄단 이상 역할 이력 | 평가 시각에 활성인 중앙 총괄단 이상 역할 |
| `notification:delete-token` | 본인 installation 삭제, legacy FCM DELETE capability | token 소유자 또는 어느 Gisu든 중앙 총괄단 이상 역할 이력 | token 소유자 또는 평가 시각에 활성인 중앙 총괄단 이상 역할 |

`SUPER_ADMIN`은 현행 FCM evaluator가 허용하지 않으므로 migration 과정에서 자동으로 추가하지
않는다. 별도의 정책 변경 승인을 받은 경우에만 새 statement와 policyVersion 변경으로 반영한다.

## Attribute

| Attribute | Type | 작성자 | 의미 |
|---|---|---|---|
| `relation.activeCentralCore` | `BOOLEAN` | Notification context builder | 하나의 role tuple이 중앙 총괄단 이상이며 연결된 Gisu의 `[startAt, endAt)` 안에 있는지 |
| `relation.isTokenOwner` | `BOOLEAN` | FCM token command service | 서버가 조회한 token의 `memberId`와 인증된 command의 `memberId`가 같은지 |

두 relation은 요청 payload가 제공하지 않는다. Role은 공용
`AuthorizationSubjectSnapshot`의 tuple을 그대로 사용하며 role type, Gisu, organization 값을
서로 다른 role에서 조합하지 않는다. Token ownership은 잠금 조회한 persistence model에서
계산한다.

`notification:delete-token`의 두 attribute는 optional이다. 본인 installation 삭제 경로는
ownership만 제공하고, 호환 capability 경로는 active central role만 제공한다. 누락된 optional
attribute에 대한 `EQ`는 false이므로 다른 경로의 fact를 추측하지 않는다.

## Rollout

현재 mode는 `SHADOW`다. 같은 입력으로 legacy와 target effect만 비교하며 FCM 발송이나 token
변경 side effect를 두 번 실행하지 않는다. 만료 중앙 역할의 legacy ALLOW/target DENY만
`expected-differences.json`의 정확한 조건으로 분류한다. 이 파일은 target 결과를 바꾸는
allowlist가 아니다.

`generated/notification-policy-artifacts.md`는 compiled AST, surface, policy identity와
fingerprint를 사람이 검토하는 문서다. `rollout/enforcement-receipts.json`이 비어 있는 동안에는
어떤 Notification action도 ENFORCE로 시작할 수 없다.
