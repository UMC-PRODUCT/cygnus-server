# 운영진 대시보드 KPI 구현 보고서

> 작성일: 2026-05-13  
> 기준 문서: `docs/analysis/운영진_대시보드_KPI_후보_분석.md`  
> 구현 범위: `member`, `challenger`, `organization`, `authorization`, `schedule`

## 1. 범위 추출 기준

기준 문서의 KPI 후보 중 현재 요청 범위에 해당하는 도메인만 구현 대상으로 삼았다.

| 포함 도메인 | 반영 기준 |
| --- | --- |
| `member` | 신규 가입자 수, 전주 대비 증감률, 학교 기준 집계, 가입 추이 버킷 |
| `challenger` | 활동 챌린저 수, 상태 분포, 포인트 합계, 위험군, 수료 임박 인원 |
| `organization` | 기수, 지부, 학교, 지부-학교 매핑, 스터디 그룹 운영 현황 |
| `authorization` | 운영진 역할 기반 대시보드 조회 스코프 |
| `schedule` | 일정 수, 출석 필수 일정, 출석 상태 분포, 승인 대기 출석 액션 큐 |

`project`, `notice`, `curriculum`, `survey`, `community`, `notification`, `audit`, `figma`, `llm`, `storage`, `term` 기반 KPI는 이번 구현에서 제외했다. 특히 기존 후보 문서에 있던 진행 중 프로젝트 수, 처리 대기 지원서 수, 매칭 차수, 미발송 공지는 응답 계약과 집계 쿼리에서 제외하고, 동일한 액션 큐 성격의 `schedule` 기반 승인 대기 출석 수로 대체했다.

## 2. 구현 단계

### 2.1 계약 재정의

- `AdminDashboardSummaryInfo`에서 프로젝트/지원서 기반 필드를 제거했다.
- `AdminDashboardActionQueueInfo`를 출석 승인 대기, 신규 위험군, 수료 임박 인원 중심으로 재정의했다.
- Web Response DTO도 동일한 계약을 따르도록 수정했다.
- Controller 테스트에는 제거된 필드가 JSON 응답에 포함되지 않는 검증을 추가했다.

### 2.2 Hexagonal 구조 구현

- Inbound Port: 운영진 대시보드 요약, 액션 큐, 컨텍스트, 학교별 요약, 위험군, 운영 현황 조회 UseCase를 분리했다.
- Application Service: `AdminAnalyticsQueryService`가 Query 전용 서비스로 각 UseCase를 구현하고, `@Transactional(readOnly = true)`를 적용했다.
- Outbound Port: 대시보드, 학교별 현황, 위험군, 운영 현황 집계 포트를 분리했다.
- Persistence Adapter: QueryDSL 기반 집계 Repository를 어댑터 뒤에 배치해 Controller와 Repository의 직접 의존을 막았다.

### 2.3 권한 스코프 적용

- `ResourceType.ANALYTICS`를 추가하고 운영진 대시보드 API에 `READ` 권한을 적용했다.
- `AdminAnalyticsScopeResolver`가 `ChallengerRole`을 기준으로 중앙, 지부, 학교, 학교 파트장 스코프를 계산한다.
- 각 집계 Repository는 계산된 스코프의 `gisuId`, `chapterId`, `schoolId`, `part` 조건만 사용해 데이터를 필터링한다.

### 2.4 KPI 집계 구현

| API | 구현 KPI |
| --- | --- |
| `GET /api/v1/admin/dashboard/summary` | 활동 챌린저 수, 신규 가입자 수, 신규 가입자 전주 대비 증감률, 활동 학교 수, 활동 지부 수, 월간 포인트 합계, 챌린저 상태 분포 |
| `GET /api/v1/admin/dashboard/action-queue` | 출석 승인 대기 수, 이번 주 신규 위험군 수, 수료 임박 인원 |
| `GET /api/v1/admin/dashboard/context` | 운영진 역할 기반 대시보드 스코프 |
| `GET /api/v1/admin/dashboard/risk-challengers` | 위험군 챌린저 목록, 누적 포인트, 최근 감점 사유/일자 |
| `GET /api/v1/admin/dashboard/operations` | 지부-학교-파트별 챌린저 분포, 파트별 포인트 부여 현황, 일정 출석 현황, 스터디 그룹 현황, 가입 추이 버킷 |
| `GET /api/v1/admin/schools/summary` | 학교별 활동 챌린저 수, 회장/부회장, 파트장 배치율, 평균 포인트, 위험군 수, 이번 주 신규 인원 |

## 3. 테스트 기준

| 검증 항목 | 기준 |
| --- | --- |
| Controller 계약 | 운영진 API가 `ApiResponse`를 직접 감싸지 않고 Response DTO를 반환하며, 제외 도메인 필드를 응답하지 않아야 한다. |
| Application Service | Query UseCase별로 스코프를 계산하고 올바른 Outbound Port를 호출해야 한다. |
| Persistence Convention | Analytics Query Repository가 `@OneToMany`를 새로 도입하지 않고, Adapter/Port 분리를 유지해야 한다. |
| Schedule 액션 큐 | `PRESENT_PENDING`, `LATE_PENDING`, `EXCUSED_PENDING`, `ABSENT_EXCUSE_PENDING`, `LATE_EXCUSE_PENDING` 상태만 승인 대기 건수로 계산해야 한다. |

## 4. 검증 결과

| 명령 | 결과 |
| --- | --- |
| `./gradlew compileJava` | 성공 |
| `./gradlew compileTestJava` | 성공 |
| `./gradlew test --tests "com.umc.product.analytics.adapter.out.persistence.AdminDashboardAnalyticsQueryRepositoryTest"` | 실패: Testcontainers가 Docker 클라이언트를 찾지 못해 `DockerClientProviderStrategy` 초기화에서 중단 |

Repository 통합 테스트 실패는 구현 로직 실패가 아니라 현재 실행 환경의 Docker 접근 실패로 확인했다. 동일 테스트는 Testcontainers 의존성이 있으므로 Docker Desktop 또는 호환 Docker daemon이 동작하는 환경에서 재실행해야 한다.

## 5. 커밋 단위

구현 변경은 기능 구현과 보고서 작성의 책임을 분리해 다음 단위로 커밋한다.

1. `feat: add admin analytics dashboard kpis`
   - analytics 도메인 구현, 권한 리소스 추가, 테스트 추가
2. `docs: add admin analytics implementation report`
   - 구현 단계, 추출 기준, 검증 결과 보고서 반영
