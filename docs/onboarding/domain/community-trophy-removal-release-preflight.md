# Community Trophy 제거 릴리스 프리플라이트 (초안)

> 상태: **DRAFT — 운영 승인 전**
> 작성일: 2026-07-17
> 변경 범위: 커뮤니티의 레거시 `Trophy` 기능 제거, 커리큘럼의 `WeeklyBestWorkbook` 및 `Certificate` 기능 보존

이 문서는 릴리스 담당자가 운영 변경을 승인하기 전에 확인할 항목을 고정한다. 저장소 문서와 생성 카탈로그의 정합성만 다루며, 아래 운영 항목은 아직 실행하지 않았다. 이 문서만으로 production 접근, 데이터 조회, 데이터 삭제, 트래픽 전환을 수행하지 않는다.

## 1. 변경 경계

- 제거 대상: 커뮤니티 `Trophy`의 애플리케이션 표면, persistence 표면, 관련 에러 코드와 API 문서 항목.
- 보존 대상: 커리큘럼의 `WeeklyBestWorkbook`을 주차별 우수 워크북 선정 결과의 유일한 기준으로 유지한다.
- 보존 대상: `Certificate` 발급·검증·폐기 흐름과 인증서 에러 코드/운영 문서를 유지한다.
- 문서 원칙: 과거 Trophy 구조를 언급할 때는 **역사적 기록**으로 표시하며, 현재 API나 KPI로 소개하지 않는다.

## 2. 운영 승인 매트릭스

다음 표의 `미실행`, `미결정`, `승인 대기` 상태는 의도적인 릴리스 게이트다. 담당자와 증적이 채워지고 승인되기 전까지 production 작업을 시작하지 않는다. 특히 **production access-log**, **row count/date range**, **backup-or-discard approval**, **old-fleet drain/hard-cutover**, **rollback decision**은 모두 보류 상태다.

| 게이트 | 현재 상태 | 실행/결정해야 할 내용 | 증적·승인 |
|---|---|---|---|
| Production access-log 확인 | **미실행 · 승인 대기** | 실제 production에서 Trophy 경로 호출량·최근 호출 시각·주체를 확인할지 승인한다. 현재 access-log를 조회하지 않았다. | 담당자: 미정 / 조회 시각·쿼리·결과 링크: 미정 |
| Row count / date range 확인 | **미실행 · 승인 대기** | production Trophy 데이터의 행 수와 `createdAt`/최종 변경 시각 범위를 조회할지 승인한다. 현재 수치와 기간을 기록하지 않았다. | 담당자: 미정 / row count: 미기록 / date range: 미기록 |
| Backup 또는 discard 승인 | **미결정 · 승인 대기** | 보존이 필요하면 백업 위치·보존 기간·복원 검증을 정하고, 폐기하면 대상·범위·비가역성을 명시적으로 승인한다. 백업 생성과 discard/deletion은 실행하지 않았다. | 결정권자: 미정 / backup 승인: 미정 / discard 승인: 미정 |
| Old-fleet drain / hard cutover | **미실행 · 승인 대기** | 구버전 fleet를 drain한 뒤 전환할지, 단일 hard cutover를 할지 topology와 트래픽 게이트를 승인한다. fleet drain, ingress 차단, hard cutover를 실행하지 않았다. | 운영 승인: 미정 / cutover 시각: 미정 |
| Rollback 전략 | **미결정 · 승인 대기** | 애플리케이션만 되돌릴지, 백업 복원과 함께 되돌릴지, rollback window와 데이터 손실 허용 범위를 결정한다. rollback을 실행하거나 가능하다고 보장하지 않았다. | 결정권자: 미정 / 전략: 미정 / window: 미정 |

### DROP 이후 rollback 제약

Trophy 테이블과 시퀀스를 `DROP`한 뒤에는 구버전 Trophy 코드를 되돌리는 것이 **정상적인 애플리케이션 rollback이 아니다**. 해당 경로를 복구하려면 사전에 승인된 **hard cutover 또는 old-fleet drain** 절차와 **백업 복원 또는 forward fix**가 필요하다. 따라서 백업/폐기 승인, fleet 전환 방식, rollback window가 확정되기 전에는 DROP 이후 복구 가능성을 보장하지 않으며, 애플리케이션 이미지만 이전 버전으로 되돌리지 않는다.

## 3. 저장소 기준 확인 항목

- [x] 런타임 작업이 Trophy 코드/API를 제거한 뒤 `./gradlew generateDocumentationCatalogs`를 실행했다.
- [x] 생성 결과에서 Trophy 에러 코드가 제거되고, `WeeklyBestWorkbook` 관련 문서와 `Certificate` 에러 코드가 유지되는 것을 확인했다.
- [x] `./gradlew checkDocumentationCatalogs`와 `./gradlew validateDocumentationCatalogs -PstrictDocumentationCatalogs=true`가 성공했다.
- [x] 생성 카탈로그의 `totalCount`와 항목 차이는 소스 기반 생성 결과로 기록했다. 수동 숫자 추정이나 텍스트 존재 여부만으로 통과시키지 않았다.
- [ ] 이 문서의 승인 매트릭스 다섯 게이트가 모두 승인된 뒤에만 production runbook을 확정한다.

## 4. 실행하지 않은 작업의 명시

현재까지 다음 작업은 **실행하지 않았다**.

- production 접근 로그 확인 및 호출량 산출
- production Trophy row count와 date range 조회
- backup 생성, 백업 복원 검증, 데이터 discard/deletion
- old fleet drain, ingress 변경, hard cutover
- rollback 선택·실행 및 데이터 복구

위 항목은 운영 담당자와 서비스 오너의 승인 없이는 수행하지 않는다. 승인 이후에도 각 결과와 실행 시각을 이 문서의 표와 별도 릴리스 기록에 먼저 남긴다.

## 5. 서명란

| 역할 | 이름 | 승인 시각 | 서명/링크 |
|---|---|---|---|
| 서비스 오너 | 미정 | 미정 | 미정 |
| 운영 담당자 | 미정 | 미정 | 미정 |
| 데이터 보존 결정권자 | 미정 | 미정 | 미정 |
| 릴리스 담당자 | 미정 | 미정 | 미정 |
