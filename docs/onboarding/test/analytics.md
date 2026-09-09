# Analytics 테스트 케이스

- 테스트 파일: 7개
- 테스트 케이스: 25개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 6 |
| UseCase / Application Service | 9 |
| Repository / Outbound Persistence | 10 |

## Controller / Inbound Adapter

### AdminDashboardControllerTest
- 테스트 설명: AdminDashboardController
- 위치: `src/test/java/com/umc/product/analytics/adapter/in/web/AdminDashboardControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [104](../../../src/test/java/com/umc/product/analytics/adapter/in/web/AdminDashboardControllerTest.java#L104) | AdminDashboardController / 대시보드 summary API 문서화 | HTTP GET /api/v1/analytics/admin/dashboard/summary; param gisuId="7" | 성공: HTTP 200 OK |
| [126](../../../src/test/java/com/umc/product/analytics/adapter/in/web/AdminDashboardControllerTest.java#L126) | AdminDashboardController / 대시보드 actionQueue API는 대상 도메인의 처리 대기 항목만 반환한다 | HTTP GET /api/v1/analytics/admin/dashboard/action-queue; param gisuId="7" | 성공: HTTP 200 OK |
| [143](../../../src/test/java/com/umc/product/analytics/adapter/in/web/AdminDashboardControllerTest.java#L143) | 대시보드 context API 응답 | HTTP GET /api/v1/analytics/admin/dashboard/context | 성공: HTTP 200 OK |
| [162](../../../src/test/java/com/umc/product/analytics/adapter/in/web/AdminDashboardControllerTest.java#L162) | 대시보드 riskChallengers API 응답 | HTTP GET /api/v1/analytics/admin/dashboard/risk-challengers; param gisuId="7" | 성공: HTTP 200 OK |
| [174](../../../src/test/java/com/umc/product/analytics/adapter/in/web/AdminDashboardControllerTest.java#L174) | 대시보드 operations API 응답 | HTTP GET /api/v1/analytics/admin/dashboard/operations; param gisuId="7"; param from="2026-05-01T00:00:00Z"; param to="2026-05-13T00:00:00Z" | 성공: HTTP 200 OK |

### AdminSchoolAnalyticsControllerTest
- 테스트 설명: AdminSchoolAnalyticsController
- 위치: `src/test/java/com/umc/product/analytics/adapter/in/web/AdminSchoolAnalyticsControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [57](../../../src/test/java/com/umc/product/analytics/adapter/in/web/AdminSchoolAnalyticsControllerTest.java#L57) | AdminSchoolAnalyticsController / 학교별 summary API 문서화 | HTTP GET /api/v1/analytics/admin/schools/summary; param gisuId="7" | 성공: HTTP 200 OK |

## UseCase / Application Service

### AdminAnalyticsQueryServiceTest
- 테스트 설명: AdminAnalyticsQueryService
- 위치: `src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [30](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsQueryServiceTest.java#L30) | summary 중앙 운영진은 전체 스코프의 KPI를 조회한다 | 호출 getSummary(query) | 성공: 검증 assertThat(actual).isEqualTo(expected); |
| [76](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsQueryServiceTest.java#L76) | AdminAnalyticsQueryService / summary 학교 운영진은 본인 학교 데이터만 조회한다 | 호출 getSummary(query) | 성공: 검증 assertThat(actual).isEqualTo(expected); |
| [98](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsQueryServiceTest.java#L98) | AdminAnalyticsQueryService / actionQueue 출석 승인 대기와 위험군, 수료 임박 항목을 조회한다 | 호출 getActionQueue(query) | 성공: 검증 assertThat(actual.pendingAttendanceDecisionCount()).isEqualTo(4L); assertThat(actual.newRiskMemberCountThisWeek()).isEqualTo(3L); assertThat(actual.upcomingGraduationCount()).isEqualTo(2L); |
| [115](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsQueryServiceTest.java#L115) | operationsOverview는 권한 스코프를 적용해 운영 현황을 조회한다 | 호출 getOperationsOverview(query) | 성공: 검증 assertThat(actual).isEqualTo(expected); |

### AdminAnalyticsScopeResolverTest
- 테스트 설명: AdminAnalyticsScopeResolver
- 위치: `src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [25](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java#L25) | 중앙 운영진은 요청한 지부와 학교 필터를 그대로 사용한다 | 호출 resolve(MEMBER_ID, GISU_ID, 10L, 20L, ChallengerPart.SPRINGBOOT) | 성공: 검증 assertThat(scope.type()).isEqualTo(AdminAnalyticsScopeType.CENTRAL); assertThat(scope.gisuId()).isEqualTo(GISU_ID); assertThat(scope.chapterId()).isEqualTo(10L); assertThat(scope.schoolId()).isEqualTo(20L); |
| [55](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java#L55) | AdminAnalyticsScopeResolver / 지부장은 다른 지부를 요청하면 거부된다 | 호출 resolve(MEMBER_ID, GISU_ID, 99L, null, null)) | 실패: 예외 AnalyticsDomainException; 에러코드 AnalyticsErrorCode.RESOURCE_ACCESS_DENIED; 검증 .isEqualTo(AnalyticsErrorCode.RESOURCE_ACCESS_DENIED); |
| [67](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java#L67) | AdminAnalyticsScopeResolver / 학교 운영진은 학교 필터가 없어도 본인 학교로 스코프가 고정된다 | 호출 resolve(MEMBER_ID, GISU_ID, null, null, null) | 성공: 검증 assertThat(scope.type()).isEqualTo(AdminAnalyticsScopeType.SCHOOL); assertThat(scope.schoolId()).isEqualTo(30L); assertThat(scope.chapterId()).isNull(); assertThat(scope.responsiblePart()).isNull(); |
| [82](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java#L82) | AdminAnalyticsScopeResolver / 학교 파트장은 담당 파트까지 스코프에 포함된다 | 호출 resolve(MEMBER_ID, GISU_ID, null, null, null) | 성공: 검증 assertThat(scope.type()).isEqualTo(AdminAnalyticsScopeType.SCHOOL_PART); assertThat(scope.schoolId()).isEqualTo(30L); assertThat(scope.responsiblePart()).isEqualTo(ChallengerPart.ANDROID); assertThat(scope.roleType())... |
| [98](../../../src/test/java/com/umc/product/analytics/application/service/query/AdminAnalyticsScopeResolverTest.java#L98) | 운영진 역할이 없으면 대시보드 접근이 거부된다 | 호출 resolve(MEMBER_ID, GISU_ID, null, null, null)) | 실패: 예외 AnalyticsDomainException; 에러코드 AnalyticsErrorCode.RESOURCE_ACCESS_DENIED; 검증 .isEqualTo(AnalyticsErrorCode.RESOURCE_ACCESS_DENIED); |

## Repository / Outbound Persistence

### AdminDashboardAnalyticsQueryRepositoryTest
- 테스트 설명: AdminDashboardAnalyticsQueryRepository
- 위치: `src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminDashboardAnalyticsQueryRepositoryTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [70](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminDashboardAnalyticsQueryRepositoryTest.java#L70) | AdminDashboardAnalyticsQueryRepository / summary 중앙 운영진은 전체 스코프의 KPI를 조회한다 | 호출 getSummary(centralScope()) | 성공: 검증 assertThat(result.activeChallengerCount()).isEqualTo(2L); assertThat(result.activeSchoolCount()).isEqualTo(2L); assertThat(result.activeChapterCount()).isEqualTo(1L); assertThat(result.monthlyPointSum().positive()).is... |
| [93](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminDashboardAnalyticsQueryRepositoryTest.java#L93) | summary 학교 운영진은 본인 학교 데이터만 조회한다 | 호출 getSummary(schoolScope(schoolAId)) | 성공: 검증 assertThat(result.activeChallengerCount()).isEqualTo(1L); assertThat(result.activeSchoolCount()).isEqualTo(1L); |
| [107](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminDashboardAnalyticsQueryRepositoryTest.java#L107) | actionQueue 출석 승인 대기 건수를 처리 대기로 계산한다 | 호출 getActionQueue(centralScope(), -8) | 성공: 검증 assertThat(result.pendingAttendanceDecisionCount()).isEqualTo(2L); |

### AdminRiskChallengerAnalyticsQueryRepositoryTest
- 테스트 설명: AdminRiskChallengerAnalyticsQueryRepository
- 위치: `src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminRiskChallengerAnalyticsQueryRepositoryTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [57](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminRiskChallengerAnalyticsQueryRepositoryTest.java#L57) | AdminRiskChallengerAnalyticsQueryRepository / pointSumLte가 있으면 포인트 합계가 임계치 이하인 챌린저만 조회한다 | 호출 getRiskChallengers(scope(), query(-8)) | 성공: 검증 assertThat(result.getContent()); .containsExactly("위험"); |
| [74](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminRiskChallengerAnalyticsQueryRepositoryTest.java#L74) | AdminRiskChallengerAnalyticsQueryRepository / includeLatestNegativePoint는 페이지 대상자의 최신 감점만 응답한다 | 호출 getRiskChallengers(scope(), query(-8)) | 성공: 검증 assertThat(result.getContent().getFirst().latestNegativePoint()).isNotNull(); assertThat(result.getContent().getFirst().latestNegativePoint().score()).isEqualTo(-10.0); |

### AdminSchoolAnalyticsQueryRepositoryTest
- 테스트 설명: AdminSchoolAnalyticsQueryRepository
- 위치: `src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminSchoolAnalyticsQueryRepositoryTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [68](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminSchoolAnalyticsQueryRepositoryTest.java#L68) | AdminSchoolAnalyticsQueryRepository / 학교 요약은 위험군 수 내림차순이 기본이다 | 조건 AdminSchoolAnalyticsQueryRepository / 학교 요약은 위험군 수 내림차순이 기본이다 | 성공: 검증 assertThat(result.getContent()); .containsExactly(schoolAId, schoolBId); |
| [90](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminSchoolAnalyticsQueryRepositoryTest.java#L90) | AdminSchoolAnalyticsQueryRepository / 지부 스코프에서는 해당 지부의 학교만 조회된다 | 조건 AdminSchoolAnalyticsQueryRepository / 지부 스코프에서는 해당 지부의 학교만 조회된다 | 성공: 검증 assertThat(result.getContent()); .containsExactly(schoolAId); |
| [108](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminSchoolAnalyticsQueryRepositoryTest.java#L108) | 검색어가 있으면 학교명 부분 일치만 조회된다 | 조건 검색어가 있으면 학교명 부분 일치만 조회된다 | 성공: 검증 assertThat(result.getContent()); .containsExactly("가천대학교"); |
| [126](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminSchoolAnalyticsQueryRepositoryTest.java#L126) | 파트장 배치율은 배치된 파트수와 운영중인 파트수로 계산된다 | 호출 getSchoolSummaries(centralScope(), query(null, null, null)) | 성공: 검증 assertThat(ratio.assigned()).isEqualTo(1L); assertThat(ratio.totalRunningParts()).isEqualTo(2L); |
| [148](../../../src/test/java/com/umc/product/analytics/adapter/out/persistence/AdminSchoolAnalyticsQueryRepositoryTest.java#L148) | 포인트가 없는 챌린저는 0점으로 평균에 포함된다 | 호출 getSchoolSummaries(centralScope(), query(null, null, null)) | 성공: 검증 assertThat(result.getContent().getFirst().averagePointSum()).isEqualTo(-5.0); |
