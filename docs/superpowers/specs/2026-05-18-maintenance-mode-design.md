# 점검 모드(Maintenance Mode) 설계 문서

작성일: 2026-05-18
갱신일: 2026-07-17 — 커뮤니티 Trophy 경로 제거 반영
도메인: `maintenance` (신설)
관련 PR: TBD (구현 후 추가)

## 1. 배경 및 목적

대한민국 은행 앱의 점검 시간 차단 패턴을 본 서비스에 도입한다.
운영진이 어드민에서 점검 윈도우를 등록/해제하면, 해당 시간 동안 일반 사용자의 API 요청은
HTTP 503 으로 차단되고, 클라이언트(iOS/Android/Web)는 점검 안내 화면을 노출한다.

## 2. 확정된 요구사항

| 항목 | 결정 |
|---|---|
| 점검 범위 | **FULL + PER_DOMAIN 둘 다 지원**. 평소엔 FULL, 필요 시 특정 도메인만 차단 |
| 트리거 방식 | **즉시 시작 + 예약 시작 둘 다 지원**. 어드민 페이지에서 토글 |
| 종료 시점 | `endAt` **필수** (NOT NULL). 무기한 점검 불허 |
| 운영진 예외 | `ChallengerRoleType.SUPER_ADMIN` 보유자만 통과 |
| 확장 포인트 | Bypass 정책을 `MaintenanceBypassPolicy` 포트로 추상화. 향후 `Member.role` 도입 시 구현체만 교체 |

## 3. 핵심 도메인 모델

### MaintenanceWindow (Aggregate Root)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | Long | PK | |
| `scope` | `MaintenanceScope` | NOT NULL | `FULL` / `PER_DOMAIN` |
| `targetDomains` | `Set<MaintenanceDomain>` | nullable | scope=PER_DOMAIN 일 때만 필수 |
| `startAt` | Instant | NOT NULL | 시작 시각. 즉시는 `now()` |
| `endAt` | Instant | NOT NULL | 종료 시각 (필수) |
| `title` | String | NOT NULL | 사용자에게 보일 제목 |
| `message` | String | NOT NULL | 사용자에게 보일 본문 |
| `forcedEndedAt` | Instant | nullable | 어드민이 강제 종료한 시각. set 되면 즉시 비활성 |
| `createdBy` | Long | NOT NULL | 생성자 memberId |

DB 컬럼은 `created_at` / `updated_at` 은 `BaseEntity` 가 자동 관리.

### 활성 판정 (`isActiveAt(now)`)
```
forcedEndedAt == null
  AND startAt <= now
  AND endAt > now
```

### MaintenanceScope (enum)
- `FULL`
- `PER_DOMAIN`

### MaintenanceDomain (enum) — URL 패턴 보유
```
CHALLENGER   → /api/v1/challenger/**, /api/v1/challenger-record/**
PROJECT      → /api/v1/projects/**, /api/v1/project/**
SCHEDULE     → /api/v1/schedules/**, /api/v1/study-groups/schedules/**
NOTICE       → /api/v1/notices/**
COMMUNITY    → /api/v1/posts/**
ORGANIZATION → /api/v1/gisu/**, /api/v1/schools/**, /api/v1/chapters/**, /api/v1/study-groups/**
NOTIFICATION → /api/v1/notification/**
MEMBER       → /api/v1/member/**
STORAGE      → /api/v1/storage/**
AUTHORIZATION → /api/v1/authorization/**
```
도메인은 enum 으로 닫혀 있고, 신규 도메인 추가 시 enum 한 줄 + URL 패턴만 추가하면 됨.

## 4. 패키지 구조 (헥사고날 준수)

```
maintenance/
├── domain/
│   ├── MaintenanceWindow.java            # JPA 엔티티 (Rich Domain)
│   ├── MaintenanceScope.java
│   ├── MaintenanceDomain.java
│   └── MaintenanceSnapshot.java          # 캐시용 불변 VO
├── exception/
│   ├── MaintenanceErrorCode.java         # MAINTENANCE-0001 ~
│   └── MaintenanceDomainException.java
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── command/
│   │   │   │   ├── ManageMaintenanceUseCase.java
│   │   │   │   └── dto/
│   │   │   │       ├── StartMaintenanceCommand.java
│   │   │   │       └── EndMaintenanceCommand.java
│   │   │   └── query/
│   │   │       ├── GetMaintenanceStatusUseCase.java
│   │   │       └── dto/
│   │   │           └── MaintenanceStatusInfo.java
│   │   └── out/
│   │       ├── LoadMaintenanceWindowPort.java
│   │       ├── SaveMaintenanceWindowPort.java
│   │       └── MaintenanceBypassPolicy.java
│   └── service/
│       ├── MaintenanceCommandService.java
│       ├── MaintenanceQueryService.java
│       └── MaintenanceStateHolder.java   # in-memory 캐시 빈
└── adapter/
    ├── in/
    │   └── web/
    │       ├── AdminMaintenanceController.java  # /api/v1/admin/maintenance
    │       ├── SystemStatusController.java      # /api/v1/system/status (Public)
    │       ├── filter/
    │       │   └── MaintenanceFilter.java       # OncePerRequestFilter
    │       ├── support/
    │       │   └── MaintenanceDomainResolver.java
    │       └── dto/
    │           ├── request/  (StartMaintenanceRequest)
    │           └── response/ (MaintenanceWindowResponse, SystemStatusResponse)
    └── out/
        ├── persistence/
        │   ├── MaintenanceWindowRepository.java   # JpaRepository
        │   └── MaintenanceWindowPersistenceAdapter.java
        └── bypass/
            └── ChallengerRoleBasedBypassPolicy.java
```

## 5. API 계약

### 5.1 공개 — 점검 상태 조회

`GET /api/v1/system/status` (@Public, 점검 중에도 통과)

응답 (성공):
```json
{
  "success": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {
    "inMaintenance": false,
    "current": null,
    "upcoming": {
      "id": 12,
      "scope": "FULL",
      "targetDomains": [],
      "startAt": "2026-05-18T22:00:00Z",
      "endAt": "2026-05-19T02:00:00Z",
      "title": "정기 점검",
      "message": "..."
    }
  }
}
```

`inMaintenance=true` 일 때 `current` 가 채워짐. 항상 다음 예약(`upcoming`) 도 함께 제공
(없으면 null) — 클라이언트가 "곧 점검 시작" 배너를 띄울 수 있게.

### 5.2 어드민 — 점검 시작

`POST /api/v1/admin/maintenance` (SUPER_ADMIN 만 허용 — 컨트롤러에서 직접 검증)

요청:
```json
{
  "scope": "FULL",
  "targetDomains": null,
  "startAt": "2026-05-18T22:00:00Z",
  "endAt": "2026-05-19T02:00:00Z",
  "title": "정기 점검",
  "message": "더 나은 서비스를 위한 점검 중입니다."
}
```

검증:
- `endAt > startAt`
- `startAt >= now - 60s` (1분 그레이스. 클라이언트 시계 어긋남 허용)
- `scope=PER_DOMAIN` 이면 `targetDomains` 비어있지 않아야 함
- 기존 활성/예약 윈도우와 겹치지 않아야 함

### 5.3 어드민 — 점검 강제 종료

`PATCH /api/v1/admin/maintenance/{id}/end` (SUPER_ADMIN 만)

해당 윈도우의 `forcedEndedAt = now` 설정. 이후 캐시 즉시 재로드.

### 5.4 어드민 — 윈도우 목록 조회

`GET /api/v1/admin/maintenance` (SUPER_ADMIN 만)

활성/예약/종료된 윈도우 모두 조회. (페이지네이션은 v2에서)

## 6. 차단 흐름 (MaintenanceFilter)

```
1. JwtAuthenticationFilter → SecurityContext 채움 (anonymous 가능)
2. MaintenanceFilter (이 필터):
   a. ALWAYS_ALLOW 경로면 통과
      - /api/v1/system/status
      - /api/v1/admin/maintenance/**
      - /api/v1/auth/**
      - /actuator/**
   b. snapshot = MaintenanceStateHolder.current()
   c. snapshot 비활성 → 통과
   d. SecurityContext 에서 memberId 추출 → bypassPolicy.shouldBypass(memberId) → true 면 통과
   e. scope=PER_DOMAIN 이면 요청 URI → MaintenanceDomain 매칭 → 매칭 안 되면 통과
   f. 그 외 모든 경우: 503 응답 (ApiResponse 포맷 + 점검 정보 result)
3. 통과 시 컨트롤러 진입
```

### 503 응답 본문
```json
{
  "success": false,
  "code": "MAINTENANCE-0001",
  "message": "서비스 점검 중입니다.",
  "result": {
    "scope": "FULL",
    "targetDomains": [],
    "startAt": "2026-05-18T22:00:00Z",
    "endAt": "2026-05-19T02:00:00Z",
    "title": "정기 점검",
    "message": "더 나은 서비스를 위한 점검 중입니다."
  }
}
```

응답 헤더에 `Retry-After: <초>` 도 함께 (endAt - now).

## 7. 캐시 & 일관성

`MaintenanceStateHolder` 빈이 `AtomicReference<MaintenanceSnapshot>` 보유.

- 어드민 Command (start/end) 직후: 동일 인스턴스에서 즉시 `refresh()`
- `@Scheduled(fixedDelay = 10_000)` 으로 10초마다 DB 재조회 (다중 인스턴스 동기화)

다중 인스턴스 환경: 어드민이 인스턴스 A 에서 점검 켜면 A 는 즉시 반영, B/C 는 최대 10초 후.
점검 시작 시점에 10초 지연은 허용 가능 — Redis pub/sub 까지는 YAGNI.

## 8. 운영진 예외 추상화

```java
// application/port/out/MaintenanceBypassPolicy.java
public interface MaintenanceBypassPolicy {
    boolean shouldBypass(Long memberId);
}

// adapter/out/bypass/ChallengerRoleBasedBypassPolicy.java
@Component
@RequiredArgsConstructor
class ChallengerRoleBasedBypassPolicy implements MaintenanceBypassPolicy {
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Override
    public boolean shouldBypass(Long memberId) {
        if (memberId == null) return false;
        return getChallengerRoleUseCase.findAllByMemberId(memberId).stream()
            .anyMatch(r -> r.roleType().isSuperAdmin());
    }
}
```

미래 `Member.role` 도입 시: `MemberRoleBasedBypassPolicy` 신규 구현 + 기존 삭제.
도메인/필터 코드는 영향 0.

## 9. 클라이언트 패턴 (참고 — 본 PR 범위 외)

세 클라이언트(iOS Swift / Android Kotlin / React) 공통 흐름:

### 9.1 앱 진입 시 (Splash)
1. `GET /api/v1/system/status` 호출
2. `inMaintenance=true` → 점검 화면(title, message, endAt 카운트다운)
3. `upcoming` 가 1시간 이내 → 메인 화면 상단에 배너
4. 둘 다 없음 → 정상 진입

### 9.2 세션 중 (HTTP Interceptor)
- 모든 API 응답을 글로벌 인터셉터에서 감시
- `503` + `code: "MAINTENANCE-0001"` 감지 → 즉시 점검 화면 전환
- 점검 화면에서 30초마다 `/api/v1/system/status` 폴링하여 자동 복귀

### 9.3 플랫폼별 구현 위치

| 플랫폼 | 인터셉터 | 상태 관리 |
|---|---|---|
| iOS Swift | `URLSession` `URLProtocol` 또는 Alamofire `RequestInterceptor` | `@Observable` / Combine |
| Android Kotlin | OkHttp `Interceptor` | Flow / LiveData |
| React Web | Axios `response interceptor` 또는 Fetch wrapper | Zustand / Context |

각 클라이언트는 점검 화면 컴포넌트 + 인터셉터 추가 + 진입 시 status 조회만 구현하면 됨.

## 10. 의사결정 근거 요약 (사용자 자율 진행 요청 대응)

| 결정 | 선택 | 근거 |
|---|---|---|
| 503 응답 헤더에 `Retry-After` 포함 | 포함 | RFC 7231 표준. 클라이언트가 자동 재시도 시 활용 가능 |
| 어드민 권한 검증 위치 | 컨트롤러에서 직접 (`SUPER_ADMIN` 체크) | 다른 어드민 컨트롤러에 `@CheckAccess` 가 있긴 하나, 점검은 가장 특권적 작업이므로 명시적 체크가 더 안전 |
| 캐시 갱신 주기 | 10초 | 점검 도메인 특성상 즉각성 < 단순성. Redis pub/sub 은 인프라 복잡도만 늘리고 가치는 낮음 |
| `MaintenanceDomain` enum 의 URL 패턴을 enum 내부에 둠 | 내부에 둠 | 도메인이 자기 URL 을 아는 것이 단일 진실 원천. 외부 매핑 테이블이 분리되면 enum 추가 시 빠뜨리기 쉬움 |
| 활성 윈도우 겹침 허용 여부 | 불허 | 운영자가 실수로 두 윈도우를 동시에 만들면 어느 게 진짜인지 모호. 명시적 거부가 안전 |
| `Domain` 전역 enum 에 `MAINTENANCE` 추가 | 추가 | 기존 컨벤션 (`global/exception/constant/Domain.java`) 따름 |
| 슈퍼관리자 판정 기준 | `roleType.isSuperAdmin()` | enum 에 이미 메서드 존재, 사용자 명시 요구사항 |
| 점검 윈도우 페이지네이션 | 미적용 (v1) | 점검 윈도우는 빈도 낮음. 100건 미만 가정 |
| Upcoming 배너 threshold | 클라이언트 결정 | 서버는 raw 데이터만, 표현 정책은 클라이언트 자율 |

## 11. 마이그레이션

`V2026.05.18.XX.XX__create_maintenance_window.sql`:
```sql
CREATE TABLE maintenance_window (
    id                BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    scope             VARCHAR(32)                             NOT NULL,
    target_domains    VARCHAR(512),
    start_at          TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    end_at            TIMESTAMP WITHOUT TIME ZONE             NOT NULL,
    title             VARCHAR(255)                            NOT NULL,
    message           VARCHAR(1000)                           NOT NULL,
    forced_ended_at   TIMESTAMP WITHOUT TIME ZONE,
    created_by        BIGINT                                  NOT NULL,
    CONSTRAINT pk_maintenance_window PRIMARY KEY (id)
);

CREATE INDEX idx_maintenance_window_active
    ON maintenance_window (start_at, end_at)
    WHERE forced_ended_at IS NULL;
```

`target_domains` 는 `CHALLENGER,PROJECT` 같은 쉼표 구분 문자열로 저장 (JPA `@Convert`).

## 12. 테스트 전략

| 레벨 | 대상 | 도구 |
|---|---|---|
| 단위 | `MaintenanceWindow` 도메인 메서드 (isActiveAt 등) | JUnit (no Spring) |
| 단위 | `MaintenanceCommandService` / `MaintenanceQueryService` | `@ExtendWith(MockitoExtension.class)` |
| 단위 | `MaintenanceDomain.fromUri()` 매핑 | JUnit |
| 통합 | `MaintenanceFilter` 차단/통과 시나리오 | `IntegrationTestSupport` + MockMvc |
| 통합 | `AdminMaintenanceController` E2E | `IntegrationTestSupport` |
| 통합 | `SystemStatusController` 공개 접근 | `IntegrationTestSupport` |

핵심 시나리오:
- FULL 점검 중 일반 사용자 → 503
- FULL 점검 중 SUPER_ADMIN → 200
- PER_DOMAIN(CHALLENGER) 점검 중 `/api/v1/notices` 호출 → 200
- PER_DOMAIN(CHALLENGER) 점검 중 `/api/v1/challenger` 호출 → 503
- 비활성 윈도우 (forcedEndedAt 설정) → 모든 요청 통과
- `/api/v1/system/status` 는 점검 중에도 200
- 윈도우 겹침 생성 시도 → 400

## 13. 미해결 / 후속

- 점검 시작 전 FCM 푸시 알림 (v2) — `notification` 도메인과 연동
- 점검 윈도우 목록 페이지네이션 (v2)
- 점검 종료 후 슬랙/디스코드 알림 (v2)
- 어드민 권한이 향후 `Member.role` 로 옮겨질 때 `MaintenanceBypassPolicy` 구현체 교체
