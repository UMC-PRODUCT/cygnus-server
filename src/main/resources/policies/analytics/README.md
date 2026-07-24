# Analytics JSON Policy

Analytics의 REST 진입 권한을 `analytics-1.0` typed context와 JSON policy로 평가한다.

## Action

- `analytics:read-dashboard`: 운영진 대시보드 조회
- `analytics:read-school`: 학교별 운영 현황 조회

## Attribute

- `relation.activeSuperAdmin`: 전역 `SUPER_ADMIN`
- `relation.activeCentralMember`: 평가 시점에 유효한 중앙 운영진
- `relation.activeChapterPresident`: 평가 시점에 유효한 지부장
- `relation.activeSchoolOperator`: 평가 시점에 유효한 학교 운영진

모든 ChallengerRole relation은 연결된 Gisu의 `[startAt, endAt)` 안에서만 `true`다.

## 현재 rollout 범위

REST entry evaluator는 공용 rollout에서 SHADOW 평가한다. `AdminAnalyticsScopeResolver`의
Gisu·지부·학교·파트 scope는 별도 outcome 정책으로 전환하기 전까지 legacy가 authoritative다.
따라서 domain coverage는 아직 `PLANNED`로 유지한다.
