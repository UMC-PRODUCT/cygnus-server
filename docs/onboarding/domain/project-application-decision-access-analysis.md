# Project Application Decision Access Analysis

작성일: 2026-07-14

## 요약

보고 내용은 다음 두 경우를 분리해서 봐야 한다.

1. 지원자가 `CHAPTER_PRESIDENT`이면서 챌린저인 경우
2. 상태 변경을 시도한 요청자가 `CHAPTER_PRESIDENT`인 경우

현재 target policy 기준 결론은 다르다.

- 지원자가 `CHAPTER_PRESIDENT`인 것만으로 상태 변경이 막히는 로직은 확인되지 않았다.
- 상태 변경 요청자는 프로젝트 PO이거나 active `SUPER_ADMIN`이어야 한다.
- 지부장, 중앙 운영진, 학교 운영진, PLAN Sub-PM은 이 action에 포함되지 않는다.

따라서 "지부장인 지원자의 지원서를 PO가 변경하지 못했다"는 보고라면 요청 계정, applicationId, 상태 및 차수 시간을 확인해야 한다. 반대로 "지부장이 직접 상태 변경을 시도했다"는 보고라면 target DENY가 정상이다.

## 상태 변경 권한 경로

지원서 상태 변경 API는 다음 컨트롤러 경로를 사용한다.

```http
PATCH /api/v1/projects/{projectId}/applications/{applicationId}/decision
```

컨트롤러의 semantic access-control은 `project-application:decide` action으로 연결된다. Command service도 같은
action의 decision을 사용하므로 capability와 실제 enforcement가 분리되지 않는다.

Target JSON은 다음 조건을 통과시킨다.

1. 지원서 상태가 `SUBMITTED`, `APPROVED`, `REJECTED` 중 하나여야 한다.
2. 요청자 `memberId`가 부모 프로젝트의 `productOwnerMemberId`와 같거나 active `SUPER_ADMIN`이어야 한다.
3. `SUPER_ADMIN`의 `ChallengerRole`은 연결된 Gisu의 `startAt <= evaluatedAt < endAt` 안에 있어야 한다.

PO decision에는 outcome이 없고 일반 차수 시간 검증을 적용한다. Active `SUPER_ADMIN` decision에는
`application.forceDecision=true` outcome이 합성되어, 상태 전이 규칙은 유지하면서 차수 시간 검증만 건너뛴다.
만료된 `SUPER_ADMIN`은 권한과 force outcome을 모두 갖지 않는다.

## 지원자가 CHAPTER_PRESIDENT인 경우

상태 변경 서비스인 `ProjectApplicationCommandService.decide()`는 지원자의 역할을 확인하지 않는다.

서비스와 target policy가 확인하는 것은 다음이다.

- 대상 지원서 존재 여부
- `APPROVED` 변경 시 잔여 quota
- `REJECTED` 변경 시 최소 선발 규정
- 도메인 상태 전이 가능 여부
- 매칭 차수 잠금 여부

지원자의 `CHAPTER_PRESIDENT` 역할 여부는 이 흐름에 없다. 지원자가 지부장 역할을 가진 챌린저여도,
요청자가 프로젝트 PO 또는 active `SUPER_ADMIN`이고 위 조건을 통과하면 상태 변경은 가능해야 한다.

## 요청자가 CHAPTER_PRESIDENT인 경우

요청자가 지부장이고 프로젝트 PO가 아니라면 현재 BE 정책상 상태 변경은 불가능하다.

관련 target 정책도 같은 방향이다.

- 부모 PO의 decision은 허용된다.
- active `SUPER_ADMIN`의 decision은 force outcome과 함께 허용된다.
- 만료된 `SUPER_ADMIN`, 보조 PM, 중앙 운영진, 지부장의 decision은 거부된다.
- 지부장은 같은 지부의 비-DRAFT 지원서 `READ`에는 허용될 수 있지만 decision action에는 포함되지 않는다.

따라서 지부장이 운영진 화면에서 직접 합격/불합격을 누르는 UX가 필요하다면, 이는 클라이언트 오류가 아니라 BE 권한 정책 변경 요구사항이다.

## 확인해야 할 로그/재현 조건

보고를 확정하려면 다음 값을 확인해야 한다.

- 상태 변경을 누른 요청자 `memberId`
- 해당 프로젝트의 `productOwnerMemberId`
- 대상 지원서의 `applicantMemberId`
- 요청자의 `CHAPTER_PRESIDENT` 역할 `gisuId`, `organizationId(chapterId)`
- 요청자의 `SUPER_ADMIN` 역할 `gisuId`와 연결 Gisu의 `startAt`, `endAt`
- authorization log의 `policyVersion`, `policyFingerprint`, `mode`, `classification`
- 응답 HTTP status와 error code

판정 기준은 다음이다.

- 응답이 403 `RESOURCE_ACCESS_DENIED`이고 요청자가 PO도 active `SUPER_ADMIN`도 아니면 target 정책상 정상 차단이다.
- 요청자가 PO 또는 active `SUPER_ADMIN`인데 403이면 typed subject/context, rollout mode, project/application 매핑을 추가 조사한다.
- 응답이 409라면 권한 문제가 아니라 quota 또는 최소 선발 규정 문제일 가능성이 높다.
- PO 응답이 400 `PROJECT_MATCHING_ROUND_LOCKED`라면 차수 결정 마감 이후 변경 시도다. Active `SUPER_ADMIN`의
  force decision에서는 이 시간 제한을 건너뛰므로 같은 오류가 발생하면 outcome 합성을 조사한다.

## 검증

다음 테스트를 실행해 현재 권한 정책과 상태 변경 서비스 동작을 확인했다.

```bash
./gradlew test --tests 'com.umc.product.project.application.authorization.ProjectTargetPolicyTest' --tests 'com.umc.product.project.application.service.command.ProjectApplicationCommandServiceTest'
```

결과:

```text
두 테스트가 모두 성공해야 한다.
```
