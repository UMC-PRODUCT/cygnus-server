# Curriculum JSON Policy

현재 실제 evaluator가 있는 Original Workbook 관리/배포와 Workbook Submission 조회를
`curriculum-1.0` bundle로 SHADOW 평가한다.

## Target

- 원본 워크북 관리·배포: active 중앙 운영진 또는 `SUPER_ADMIN`
- 제출 현황 조회: active 학교 운영진 또는 `SUPER_ADMIN`

ChallengerRole은 연결된 Gisu의 `[startAt, endAt)` 동안만 유효하다.

## 미전환 surface

- `ResourceType.CURRICULUM`에는 현행 evaluator가 없다.
- Challenger Workbook Mission controller에는 권한 TODO가 남아 있다.
- Challenger Workbook, mission submission/feedback, weekly best의 owner/mentor 관계를 별도
  resource snapshot으로 설계해야 한다.

이 항목은 legacy parity만으로 권한을 정할 수 없으므로 Target matrix 검토 전에는 자동으로
권한을 확대하지 않는다. 따라서 domain coverage는 `PLANNED`다.
