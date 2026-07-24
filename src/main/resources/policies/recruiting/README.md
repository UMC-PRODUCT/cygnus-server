# Recruiting JSON Policy

Recruiting의 운영진 capability와 실제 모집 대상에 대한 명령 권한을 `recruiting-1.0`
bundle로 평가한다.

## Action

- `recruiting:operate-school`: 학교 단위 운영 화면의 READ/WRITE/EDIT/APPROVE capability
- `recruiting:manage-all`: 중앙 운영진 전용 MANAGE capability
- `recruiting-season:create`: 요청의 Gisu·학교에 시즌을 생성
- `recruiting-application:decide`: 실제 지원서의 Gisu·학교를 기준으로 합불 결정
- `recruiting-registration:manage`: 실제 지원서의 Gisu를 기준으로 등록 처리
- `recruiting-summary:read`: 요청 Gisu의 지원 현황 집계 조회
- `recruiting:export`: 요청 Gisu의 지원 현황 CSV 내보내기

## Attribute와 relation

- `resource.specified`
- `relation.superAdmin`
- `relation.activeAnyCentralCore`
- `relation.activeAnySchoolCore`
- `relation.activeCentralCoreInTargetGisu`
- `relation.activeSchoolCoreForTarget`

target relation은 서버가 조회한 `RecruitingSeason` 또는 `RecruitingApplication`의
`gisuId`·`schoolId`와 하나의 role tuple을 함께 비교한다. 서로 다른 role의 Gisu와 학교를
결합하지 않으며, ChallengerRole은 연결된 Gisu의 `[startAt, endAt)` 동안만 활성이다.

## 이중 검증

`@CheckAccess(ResourceType.RECRUITMENT)`는 화면·resource 진입 capability를 검사한다. 생성
요청이나 application 명령처럼 annotation 시점에 실제 target이 없거나 다른 aggregate를
통해 찾아야 하는 경우에는 use case가 구체적인 action을 다시 평가한다. 후자는 단순 중복이
아니라 요청 값 위조와 다른 Gisu·학교 resource 접근을 막는 object-level authorization이다.

## 현재 rollout 범위

공용 rollout은 SHADOW로 시작한다. 모집 시즌·합불·등록·집계·CSV와 기존 evaluator를 먼저
전환했으며, applicant ownership, evaluator assignment, 질문·면접·round/application 목록
scope가 모두 action catalog에 연결되기 전까지 domain coverage는 `PLANNED`다.
