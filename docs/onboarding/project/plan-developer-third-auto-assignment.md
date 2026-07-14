# PLAN_DEVELOPER 3차 종료 후 잔여 TO 자동 배정

## 목적

`PLAN_DEVELOPER` 매칭은 `THIRD` 차수의 결정 마감 이후에도 개발 파트 TO가 남을 수 있다. 현재 구현은 별도의 랜덤 매칭 라운드를 만들지 않고, `THIRD` 자동 선발 처리 안에서 남은 TO를 같은 지부의 ACTIVE 개발 챌린저에게 랜덤 배정한다.

랜덤 배정으로 생성된 팀원은 지원서 기반 합격자가 아니므로 `ProjectMember.application = null`로 저장된다. 사용자 지원 내역 화면에서는 이 케이스를 `RANDOM_MATCHING` 카드로 합성한다.

## 실행 진입점

자동 선발 진입점은 `AutoDecideProjectMatchingRoundUseCase.autoDecide(matchingRoundId, actor)` 하나다.

- 스케줄러 호출: `decisionDeadline + app.project.matching-round.deadline-buffer-minutes` 시점에 실행한다. 기본 buffer는 1분이다.
- 운영진 수동 호출: `POST /api/v1/project/matching-rounds/{matchingRoundId}/auto-decide`.
- REST adapter는 인증된 member만 `AutoDecisionActor.Member(memberId)`로 만든다.
- Scheduler adapter만 `AutoDecisionActor.SystemActor("matching-round-scheduler")`를 만든다.
- MEMBER 호출은 active `SUPER_ADMIN`, round gisu의 active 중앙 운영진, round chapter의 active 지부장만 허용한다.
- SYSTEM 호출은 `subject.kind == SYSTEM`이고 `subject.systemId == "matching-round-scheduler"`인 target policy만 허용한다.
- 운영진 role은 연결 Gisu의 `startAt <= evaluatedAt < endAt` 동안만 active다.
- 실행 이력의 `executedByMemberId == null`은 승인된 SYSTEM 실행의 저장 결과일 뿐, 권한 검증을 생략하는 입력 신호가 아니다.
- `autoDecisionExecutedAt`이 이미 있으면 멱등 no-op으로 종료한다.
- `decisionDeadline`이 아직 지나지 않았으면 `PROJECT_MATCHING_ROUND_NOT_FINALIZABLE` 예외가 발생한다.

관련 구현:

- `ProjectMatchingRoundFinalizationCommandService`
- `MatchingRoundDeadlineScheduler`
- `MatchingRoundDeadlineHandler`
- `ProjectMatchingRoundController`

## 전체 처리 순서

자동 선발은 두 단계로 동작한다.

1. 해당 매칭 차수에 접수된 지원서를 최종 합격/불합격으로 확정한다.
2. 매칭 차수가 `PLAN_DEVELOPER` + `THIRD`인 경우에만, 남은 개발 파트 TO를 지원서 없이 랜덤 배정한다.

두 단계에서 만들어지는 `ProjectMember`는 한 번에 저장된다. 1단계 합격자는 `application`이 연결되고, 2단계 랜덤 배정자는 `application = null`이다.

## 1단계: THIRD 차수 지원서 최종화

지원서 자동 선발은 모든 차수에서 공통으로 실행된다. 단, 여기서는 `PLAN_DEVELOPER` 기준만 설명한다.

### 입력 지원서

대상 지원서는 해당 `matchingRoundId`의 지원서 중 아래 상태만 포함한다.

- `SUBMITTED`
- `APPROVED`
- `REJECTED`

`DRAFT`와 `CANCELLED`는 정책 입력에서 제외하고 상태도 변경하지 않는다.

### 그룹 기준

지원자는 챌린저 도메인에서 `memberId + gisuId`로 파트를 조회한 뒤, 아래 키로 그룹화한다.

- `projectId`
- `ChallengerPart`

즉, 같은 프로젝트라도 `WEB`, `ANDROID`, `IOS`, `NODEJS`, `SPRINGBOOT` 파트는 서로 독립적으로 정책을 적용한다.

### 남은 TO 산정

정책에 넘기는 `quota`는 전체 TO가 아니라 남은 TO다.

```text
남은 TO = ProjectPartQuota.quota - 해당 프로젝트/파트의 ACTIVE ProjectMember 수
```

음수가 되면 0으로 처리한다.

### PLAN_DEVELOPER 최소 선발 규칙

`DeveloperMatchingPolicy.minimumRequired(applicantsCount, quota)`는 다음 규칙을 사용한다.

| 조건 | 최소 선발 인원 |
| --- | --- |
| 지원자 수 >= 남은 TO | `ceil(남은 TO * 0.5)` |
| 지원자 수 > 남은 TO * 0.5 | `ceil(남은 TO * 0.25)` |
| 지원자 수 <= 남은 TO * 0.5 | `0` |

예시:

- 남은 TO 6, 지원자 6명: 최소 3명
- 남은 TO 6, 지원자 4명: 최소 2명
- 남은 TO 6, 지원자 3명: 최소 0명
- 남은 TO 0: 최소 0명

### 자동 합불 결정 순서

정책은 다음 순서로 최종 합격자를 정한다.

1. 기존 `APPROVED` 지원자는 그대로 합격 유지.
2. 기존 합격자 수가 최소 선발 인원보다 적으면 `SUBMITTED` 풀에서 랜덤 보충.
3. 그래도 부족하면 `REJECTED` 풀에서 랜덤 보충한다. 이 경우 PM의 기존 불합격 결정을 override한다.
4. 합격자로 선정되지 않은 `SUBMITTED`와 `REJECTED`는 모두 최종 `REJECTED`.

상태가 변경되는 지원서에는 다음 메타데이터가 기록된다.

- `statusChangedMemberId = executedByMemberId`
- `statusChangeReason = "auto-decide"`
- `statusChangedAt = now`

## 2단계: THIRD 종료 후 잔여 TO 랜덤 배정

지원서 최종화가 끝난 뒤, 아래 조건을 만족할 때만 잔여 TO 랜덤 배정을 실행한다.

```java
round.getType() == MatchingType.PLAN_DEVELOPER
    && round.getPhase() == MatchingPhase.THIRD
```

`PLAN_DESIGN`에는 적용하지 않는다. `PLAN_DEVELOPER`라도 `FIRST`, `SECOND` 차수에서는 실행하지 않는다.

### 대상 프로젝트

대상 프로젝트는 현재 구현 기준으로 아래 조건을 만족하는 모든 프로젝트다.

- `chapterId == round.chapterId`
- `status == IN_PROGRESS`

주의: 대상 프로젝트 조회에는 `gisuId` 필터가 없다. 후보 챌린저 조회에 사용할 `gisuId`는 조회된 프로젝트 목록의 첫 번째 프로젝트에서 가져온다.

### 대상 파트

잔여 TO 랜덤 배정은 개발 파트만 대상으로 한다.

- `WEB`
- `ANDROID`
- `IOS`
- `NODEJS`
- `SPRINGBOOT`

`PLAN`, `DESIGN`, `ADMIN`은 배정 대상에서 제외한다.

### 잔여 TO 산정

각 프로젝트/파트별 잔여 TO는 다음처럼 계산한다.

```text
잔여 TO = ProjectPartQuota.quota
       - 기존 ACTIVE ProjectMember 수
       - 이번 자동 선발 1단계에서 저장 예정인 ProjectMember 수
```

0 이하인 프로젝트/파트는 배정 후보에서 제외한다. 모든 프로젝트/파트의 잔여 TO가 0이면 랜덤 배정 단계는 종료한다.

### 후보자 풀

후보자는 챌린저 검색 UseCase로 조회한다.

검색 조건:

- `chapterId = round.chapterId`
- `gisuId = 대상 프로젝트 목록 첫 번째 프로젝트의 gisuId`
- `statuses = [ACTIVE]`
- 페이지 크기 500으로 cursor pagination 조회

조회된 후보 목록은 먼저 전체 셔플된다. 이후 순회하면서 아래 후보는 제외한다.

- 이미 대상 프로젝트 목록의 ACTIVE `ProjectMember`인 멤버
- 이번 자동 선발 1단계에서 `ProjectMember`로 생성 예정인 멤버
- 개발 파트가 아닌 멤버
- 후보자의 파트와 같은 잔여 TO가 더 이상 없는 경우

### 배정 방식

후보자를 셔플된 순서대로 순회한다.

1. 후보자의 파트와 같은 잔여 TO가 있는 `(projectId, part)` 목록을 만든다.
2. 그 목록 중 하나를 랜덤으로 선택한다.
3. `ProjectMember.create(project, candidate.memberId(), candidate.part(), executedByMemberId)`로 팀원을 생성한다.
4. 해당 `(projectId, part)`의 잔여 TO를 1 감소시킨다.
5. 모든 잔여 TO가 0이 되거나 후보자를 모두 소진하면 종료한다.

프로젝트 선택은 잔여 TO 수로 가중치를 주지 않는다. 가능한 `(projectId, part)` key 중 하나를 균등 랜덤으로 선택한다.

## 운영상 유의사항

- 잔여 TO 랜덤 배정은 점수, 지원 이력, 선호 프로젝트, 학교, 역할을 고려하지 않는다.
- 후보자가 부족하면 남은 TO는 그대로 남는다.
- 랜덤 배정자는 지원서가 없으므로 `ProjectApplication` 상태가 변경되지 않는다.
- 랜덤 배정자는 `ProjectMember.application = null`로 저장되며, 일반 지원서 합격자와 구분된다.
- 중복 제외 기준은 대상 프로젝트 목록의 ACTIVE 멤버와 이번 실행에서 저장 예정인 멤버다.
- 스케줄러는 기본 활성화되어 있으며 `scheduler.matching-round-deadline.enabled=false`로 끌 수 있다.

## 테스트 기준

관련 단위 테스트는 `ProjectMatchingRoundFinalizationCommandServiceTest`에 있다.

주요 케이스:

- `PLAN_DEVELOPER_3차_종료_후_남은_TO를_ACTIVE_개발_챌린저로_랜덤_배정한다`
- `PLAN_DEVELOPER_2차_종료_후에는_잔여_TO_랜덤_배정을_하지_않는다`
- `이전_차수_active_멤버가_있으면_남은_TO_기준으로_정책이_적용된다`
- `남은_TO가_0이면_의무_없음으로_처리되어_SUBMITTED는_모두_REJECTED`
