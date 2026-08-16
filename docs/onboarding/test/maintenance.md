# Maintenance 테스트 케이스

- 테스트 파일: 8개
- 테스트 케이스: 47개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 2 |
| UseCase / Application Service | 9 |
| E2E / Integration | 14 |
| Domain | 22 |

## Controller / Inbound Adapter

### SystemStatusControllerTest
- 테스트 설명: SystemStatusController
- 위치: `src/test/java/com/umc/product/maintenance/adapter/in/web/SystemStatusControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [31](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/SystemStatusControllerTest.java#L31) | SystemStatusController | HTTP GET /api/v1/system/status | 성공: HTTP 200 OK |
| [55](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/SystemStatusControllerTest.java#L55) | SystemStatusController / 점검중일 때 current 채워짐 | HTTP GET /api/v1/system/status | 성공: HTTP 200 OK |

## UseCase / Application Service

### MaintenanceCommandServiceTest
- 위치: `src/test/java/com/umc/product/maintenance/application/service/MaintenanceCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [55](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceCommandServiceTest.java#L55) | 점검 시작 | 조건 점검 시작 | 성공: 점검 시작 |
| [73](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceCommandServiceTest.java#L73) | 점검 시작 / 겹치는 윈도우가 있으면 예외 | 조건 점검 시작 / 겹치는 윈도우가 있으면 예외 | 실패: 예외 MaintenanceDomainException; 에러코드 MaintenanceErrorCode.OVERLAPPING_WINDOW |
| [92](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceCommandServiceTest.java#L92) | 점검 시작 / 강제 종료 | 호출 forceEnd(999L, 1L)) | 실패: 예외 MaintenanceDomainException; 에러코드 MaintenanceErrorCode.MAINTENANCE_WINDOW_NOT_FOUND |
| [103](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceCommandServiceTest.java#L103) | 강제 종료 / 활성 윈도우를 강제 종료하고 캐시 갱신 | 조건 강제 종료 / 활성 윈도우를 강제 종료하고 캐시 갱신 | 성공: 검증 assertThat(window.getForcedEndedAt()).isEqualTo(duringMaintenance); |

### MaintenanceQueryServiceTest
- 위치: `src/test/java/com/umc/product/maintenance/application/service/MaintenanceQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [41](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceQueryServiceTest.java#L41) | getStatus | 호출 getStatus() | 성공: 검증 assertThat(status.inMaintenance()).isFalse(); assertThat(status.current()).isNull(); assertThat(status.upcoming()).isNull(); |
| [55](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceQueryServiceTest.java#L55) | getStatus / 활성 윈도우가 있으면 current 와 inMaintenance true | 호출 getStatus() | 성공: 검증 assertThat(status.inMaintenance()).isTrue(); assertThat(status.current()).isNotNull(); |
| [72](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceQueryServiceTest.java#L72) | getStatus / 활성 없고 예약만 있으면 upcoming 채워짐 | 호출 getStatus() | 성공: 검증 assertThat(status.inMaintenance()).isFalse(); assertThat(status.current()).isNull(); assertThat(status.upcoming()).isNotNull(); |
| [93](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceQueryServiceTest.java#L93) | getStatus / getById | 호출 getById(99L)) | 실패: 예외 MaintenanceDomainException; 에러코드 MaintenanceErrorCode.MAINTENANCE_WINDOW_NOT_FOUND |
| [107](../../../src/test/java/com/umc/product/maintenance/application/service/MaintenanceQueryServiceTest.java#L107) | getById / listAll | 호출 listAll()).isEmpty() | 성공: 검증 assertThat(sut.listAll()).isEmpty(); |

## E2E / Integration

### AdminMaintenanceControllerIntegrationTest
- 테스트 설명: AdminMaintenanceController 통합 테스트
- 위치: `src/test/java/com/umc/product/maintenance/adapter/in/web/AdminMaintenanceControllerIntegrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [64](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/AdminMaintenanceControllerIntegrationTest.java#L64) | AdminMaintenanceController 통합 테스트 / SUPER ADMIN 은 점검을 생성하고 종료할 수 있다 | HTTP POST /api/v1/maintenance/admin; HTTP PATCH /api/v1/maintenance/admin/ | 성공: HTTP 200 OK |
| [93](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/AdminMaintenanceControllerIntegrationTest.java#L93) | AdminMaintenanceController 통합 테스트 / 일반 사용자는 점검 생성 시도시 403 | HTTP POST /api/v1/maintenance/admin | 실패: HTTP 403 Forbidden |
| [110](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/AdminMaintenanceControllerIntegrationTest.java#L110) | 겹치는 시간대의 점검을 만들면 409 | HTTP POST /api/v1/maintenance/admin | 실패: HTTP 200 OK; HTTP 409 Conflict |
| [138](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/AdminMaintenanceControllerIntegrationTest.java#L138) | 종료가 시작보다 빠르면 400 | HTTP POST /api/v1/maintenance/admin | 실패: HTTP 400 Bad Request |
| [156](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/AdminMaintenanceControllerIntegrationTest.java#L156) | 윈도우 목록은 SUPER ADMIN 만 조회 가능 | HTTP GET /api/v1/maintenance/admin | 실패: HTTP 200 OK; HTTP 403 Forbidden |

### MaintenanceFilterIntegrationTest
- 테스트 설명: MaintenanceFilter 통합 테스트
- 위치: `src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [33](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java#L33) | MaintenanceFilter 통합 테스트 | HTTP GET /api/v1/system/status | 성공: HTTP 200 OK |
| [61](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java#L61) | MaintenanceFilter 통합 테스트 / FULL 점검중에도 시스템 상태 조회는 200 | HTTP GET /api/v1/system/status | 성공: HTTP 200 OK |
| [70](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java#L70) | MaintenanceFilter 통합 테스트 / FULL 점검중 비인증 요청은 503 과 점검 정보 반환 | HTTP GET /api/v1/challenger/me | 성공: isServiceUnavailable |
| [82](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java#L82) | MaintenanceFilter 통합 테스트 / FULL 점검중 SUPER ADMIN 토큰이면 필터를 통과한다 | HTTP GET /api/v1/challenger/me | 성공: 검증 assertThat(actualStatus).isNotEqualTo(503); |
| [101](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java#L101) | MaintenanceFilter 통합 테스트 / PER DOMAIN 점검은 지정되지 않은 도메인 요청을 차단하지 않는다 | HTTP GET /api/v1/challenger/me | 성공: 검증 assertThat(actualStatus).isNotEqualTo(503); |
| [112](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java#L112) | PER DOMAIN 점검은 지정된 도메인 요청을 503으로 차단 | HTTP GET /api/v1/notices/1 | 성공: isServiceUnavailable |
| [123](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java#L123) | 어드민 점검 관리 경로는 점검중에도 항상 통과 | HTTP GET /api/v1/maintenance/admin | 성공: 검증 assertThat(actualStatus).isNotEqualTo(503); |
| [135](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java#L135) | Actuator 경로는 점검중에도 항상 통과 | HTTP GET /actuator/health | 성공: 검증 assertThat(actualStatus).isNotEqualTo(503); |
| [145](../../../src/test/java/com/umc/product/maintenance/adapter/in/web/filter/MaintenanceFilterIntegrationTest.java#L145) | FULL 점검 중에도 약관 조회는 점검 필터를 통과한다 | HTTP GET /api/v1/terms | 성공: HTTP 200 OK |

## Domain

### MaintenanceDomainSetConverterTest
- 위치: `src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainSetConverterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [11](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainSetConverterTest.java#L11) | Set 을 정렬된 콤마 문자열로 변환 | 조건 Set 을 정렬된 콤마 문자열로 변환 | 성공: 검증 assertThat(result).isEqualTo("CHALLENGER,PROJECT"); |
| [20](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainSetConverterTest.java#L20) | 빈 Set 은 null | 호출 convertToDatabaseColumn(EnumSet.noneOf(MaintenanceDomain.class))); 호출 convertToDatabaseColumn(null)).isNull() | 성공: 검증 assertThat(sut.convertToDatabaseColumn(EnumSet.noneOf(MaintenanceDomain.class))); .isNull(); assertThat(sut.convertToDatabaseColumn(null)).isNull(); |
| [28](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainSetConverterTest.java#L28) | DB 문자열을 EnumSet 으로 복원 | 호출 convertToEntityAttribute("CHALLENGER,PROJECT")) | 성공: 검증 assertThat(sut.convertToEntityAttribute("CHALLENGER,PROJECT")); .containsExactlyInAnyOrder(MaintenanceDomain.CHALLENGER, MaintenanceDomain.PROJECT); |
| [35](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainSetConverterTest.java#L35) | null/공백 은 빈 EnumSet | 호출 convertToEntityAttribute(null)).isEmpty(); 호출 convertToEntityAttribute("")).isEmpty(); 호출 convertToEntityAttribute(" ")).isEmpty() | 성공: 검증 assertThat(sut.convertToEntityAttribute(null)).isEmpty(); assertThat(sut.convertToEntityAttribute("")).isEmpty(); assertThat(sut.convertToEntityAttribute(" ")).isEmpty(); |

### MaintenanceDomainTest
- 위치: `src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [9](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainTest.java#L9) | URI 가 challenger 경로면 CHALLENGER 매칭 | 조건 URI 가 challenger 경로면 CHALLENGER 매칭 | 성공: 검증 assertThat(MaintenanceDomain.fromUri("/api/v1/challenger/me")); .contains(MaintenanceDomain.CHALLENGER); assertThat(MaintenanceDomain.fromUri("/api/v1/challenger-record/123")); .contains(MaintenanceDomain.CHALLENGER); |
| [18](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainTest.java#L18) | URI 가 어느 도메인에도 속하지 않으면 empty | 조건 URI 가 어느 도메인에도 속하지 않으면 empty | 성공: 검증 assertThat(MaintenanceDomain.fromUri("/api/v1/system/status")); .isEqualTo(Optional.empty()); assertThat(MaintenanceDomain.fromUri("/random/path")); .isEqualTo(Optional.empty()); |
| [27](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainTest.java#L27) | study-groups 는 ORGANIZATION 으로 매칭 | 조건 study-groups 는 ORGANIZATION 으로 매칭 | 성공: 검증 assertThat(MaintenanceDomain.fromUri("/api/v1/study-groups/1")); .contains(MaintenanceDomain.ORGANIZATION); |
| [34](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceDomainTest.java#L34) | project 와 projects 모두 PROJECT 로 매칭 | 조건 project 와 projects 모두 PROJECT 로 매칭 | 성공: 검증 assertThat(MaintenanceDomain.fromUri("/api/v1/projects/1")); .contains(MaintenanceDomain.PROJECT); assertThat(MaintenanceDomain.fromUri("/api/v1/project/matching-rounds")); .contains(MaintenanceDomain.PROJECT); |

### MaintenanceWindowTest
- 위치: `src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [22](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L22) | 점검 윈도우 생성 | 조건 점검 윈도우 생성 | 성공: 검증 assertThat(window.getScope()).isEqualTo(MaintenanceScope.FULL); assertThat(window.getTargetDomains()).isEmpty(); assertThat(window.getStartAt()).isEqualTo(START); assertThat(window.getEndAt()).isEqualTo(END); |
| [43](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L43) | 점검 윈도우 생성 / PER DOMAIN 점검은 대상 도메인이 없으면 예외 | 조건 점검 윈도우 생성 / PER DOMAIN 점검은 대상 도메인이 없으면 예외 | 실패: 예외 MaintenanceDomainException; 에러코드 MaintenanceErrorCode.TARGET_DOMAINS_REQUIRED |
| [58](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L58) | 점검 윈도우 생성 / 종료 시각이 시작보다 빠르면 예외 | 조건 점검 윈도우 생성 / 종료 시각이 시작보다 빠르면 예외 | 실패: 예외 MaintenanceDomainException; 에러코드 MaintenanceErrorCode.INVALID_TIME_RANGE |
| [73](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L73) | 점검 윈도우 생성 / 시작 시각이 과거 60초보다 더 이전이면 예외 | 조건 점검 윈도우 생성 / 시작 시각이 과거 60초보다 더 이전이면 예외 | 실패: 예외 MaintenanceDomainException; 에러코드 MaintenanceErrorCode.START_AT_IN_PAST |
| [89](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L89) | 점검 윈도우 생성 / 시작 시각이 과거 60초 이내면 허용된다 | 조건 점검 윈도우 생성 / 시작 시각이 과거 60초 이내면 허용된다 | 성공: 검증 assertThat(window.getStartAt()).isEqualTo(graceWithin); |
| [108](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L108) | 활성/예약 상태 판정 | 조건 활성/예약 상태 판정 | 성공: 검증 assertThat(window.isActiveAt(NOW)).isFalse(); assertThat(window.isUpcomingAt(NOW)).isTrue(); |
| [120](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L120) | 활성/예약 상태 판정 / 시작과 종료 사이는 active | 조건 활성/예약 상태 판정 / 시작과 종료 사이는 active | 성공: 검증 assertThat(window.isActiveAt(middle)).isTrue(); assertThat(window.isUpcomingAt(middle)).isFalse(); |
| [131](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L131) | 활성/예약 상태 판정 / 종료 시각 이후는 비활성 | 조건 활성/예약 상태 판정 / 종료 시각 이후는 비활성 | 성공: 검증 assertThat(window.isActiveAt(after)).isFalse(); assertThat(window.isUpcomingAt(after)).isFalse(); |
| [142](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L142) | 활성/예약 상태 판정 / 강제 종료된 윈도우는 active 도 upcoming 도 아님 | 조건 활성/예약 상태 판정 / 강제 종료된 윈도우는 active 도 upcoming 도 아님 | 성공: 검증 assertThat(window.isActiveAt(middle)).isFalse(); assertThat(window.isUpcomingAt(middle)).isFalse(); assertThat(window.getForcedEndedAt()).isEqualTo(middle); assertThat(window.getForcedEndedBy()).isEqualTo(99L); |
| [159](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L159) | 활성/예약 상태 판정 / 강제 종료 | 조건 활성/예약 상태 판정 / 강제 종료 | 실패: 예외 MaintenanceDomainException; 에러코드 MaintenanceErrorCode.ALREADY_ENDED |
| [173](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L173) | 강제 종료 / 자연 종료된 윈도우 강제종료 시도시 예외 | 조건 강제 종료 / 자연 종료된 윈도우 강제종료 시도시 예외 | 실패: 예외 MaintenanceDomainException; 에러코드 MaintenanceErrorCode.ALREADY_ENDED |
| [184](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L184) | 강제 종료 / 강제 종료 시 요청자 memberId 가 기록된다 | 조건 강제 종료 / 강제 종료 시 요청자 memberId 가 기록된다 | 성공: 검증 assertThat(window.getForcedEndedAt()).isEqualTo(middle); assertThat(window.getForcedEndedBy()).isEqualTo(42L); |
| [200](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L200) | 강제 종료 / URI 차단 판정 | 조건 강제 종료 / URI 차단 판정 | 성공: 검증 assertThat(window.blocks("/api/v1/projects/1")).isTrue(); assertThat(window.blocks("/api/v1/notices/2")).isTrue(); assertThat(window.blocks("/api/v1/random-thing")).isTrue(); |
| [213](../../../src/test/java/com/umc/product/maintenance/domain/MaintenanceWindowTest.java#L213) | URI 차단 판정 / PER DOMAIN 점검은 대상 도메인만 차단 | 조건 URI 차단 판정 / PER DOMAIN 점검은 대상 도메인만 차단 | 성공: 검증 assertThat(window.blocks("/api/v1/challenger/me")).isTrue(); assertThat(window.blocks("/api/v1/challenger-record/1")).isTrue(); assertThat(window.blocks("/api/v1/notices/2")).isFalse(); assertThat(window.blocks("/api/... |
