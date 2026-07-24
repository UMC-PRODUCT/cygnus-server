# Feedback JSON Policy

Feedback의 template target type 판정과 response 제출 자격을 `feedback-1.0` bundle에서
평가한다.

## Action과 outcome

- `feedback-template:resolve`
  - outcome `feedback.targetType`
  - dominance: `ADMIN > EXPERIENCED_CHALLENGER > NEW_CHALLENGER`
- `feedback-response:submit`
  - 현재 사용자의 계산된 target type과 template의 target type이 같아야 ALLOW

## Relation

- `relation.activeCentralMemberInTargetGisu`
- `relation.activeChallengerInTargetGisu`
- `relation.hasPreviousGisu`
- `relation.generationTenPlanChallenger`
- `resource.feedback.targetType`

중앙 운영진은 target Gisu와 role tuple을 함께 비교하고 `[startAt, endAt)` 기간을 적용한다.
10기 PLAN 특례는 기존 분류 규칙을 별도 relation으로 보존한다.

## Rollout 주의

기존 응답 제출은 template target type을 다시 확인하지 않았으므로 legacy는 항상 ALLOW다.
Target의 불일치 DENY는 SHADOW에서 `UNEXPECTED_DIFFERENCE`로 관측하고 별도 정책 검토를 거쳐야
ENFORCE할 수 있다. 만료 중앙 운영진 제거만 승인된 expected difference다.
