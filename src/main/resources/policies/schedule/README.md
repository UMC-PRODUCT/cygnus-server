# Schedule JSON Policy

Schedule와 Attendance의 entry authorization을 `schedule-1.0` bundle에서 평가한다.

## Action

- `schedule:read/create/update/delete/force-delete`
- `attendance:submit/read/approve`

## Relation

- `relation.challengerHistory`
- `relation.isAuthor`
- `relation.isParticipant`
- `relation.superAdmin`
- `relation.activeOperatingStaff`
- `relation.activeOperatingStaffInTargetGisu`

Schedule resource와 참가자 관계는 한 번 조회해 legacy/target에 함께 전달한다. Attendance의
target Gisu는 Schedule 시작 시각으로 서버가 해석하며, active staff relation은 Gisu의
`[startAt, endAt)` 경계를 적용한다.

## 현재 rollout 범위

두 `ResourcePermissionEvaluator`는 공용 SHADOW rollout을 사용한다. 다만
`ScheduleCapabilitiesService`와 운영진 목록 scope가 아직 직접 role 판단을 사용하므로
domain coverage는 `PLANNED`다.
