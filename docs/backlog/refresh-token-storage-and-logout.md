# Refresh Token 저장 및 로그아웃 API 도입 계획서

> 작성일: 2026-05-06
> 대상 도메인: `authentication`
> 관련 모듈: [JwtTokenProvider.java](src/main/java/com/umc/product/global/security/JwtTokenProvider.java), [AuthenticationService.java](src/main/java/com/umc/product/authentication/application/service/AuthenticationService.java), [TokenAuthenticationController.java](src/main/java/com/umc/product/authentication/adapter/in/web/TokenAuthenticationController.java), [CredentialAuthenticationService.java](src/main/java/com/umc/product/authentication/application/service/CredentialAuthenticationService.java), [OAuthAuthenticationService.java](src/main/java/com/umc/product/authentication/application/service/OAuthAuthenticationService.java)

---

## 1. 배경 및 문제 정의

### 1.1 현재 상태

- Refresh Token(이하 RT)을 **stateless JWT** 로만 관리하고 있어, 서버 측에 어떠한 발급/사용 이력도 저장되지 않는다.
- [TokenAuthenticationController.java:28-37](src/main/java/com/umc/product/authentication/adapter/in/web/TokenAuthenticationController.java#L28-L37) 의 `POST /api/v1/auth/token/renew` 는 **서명과 만료시각만** 검증한다.
- [AuthenticationService.java:36-52](src/main/java/com/umc/product/authentication/application/service/AuthenticationService.java#L36-L52) 의 `renewAccessToken()` 내 TODO 에서 명시된 것처럼 **재사용 통제, 폐기, 무효화 수단이 부재**하다.
- 로그아웃 엔드포인트 자체가 존재하지 않으며, 클라이언트가 로컬에서 토큰을 폐기하는 것 외에 서버 측 차단 수단이 없다.

### 1.2 보안 리스크

- 유출된 RT 는 만료(7일)까지 무제한 재사용 가능하다.
- 사용자가 의도적으로 로그아웃해도 토큰을 회수할 수 없다.
- 1회용 RT (rotation) 정책을 적용하려면 사용 이력 저장이 필수다.

### 1.3 목표

1. **RT 저장소(Whitelist)** 를 도입하여, 서버에 등록되지 않은 RT 는 거부한다.
2. **로그아웃 API** 를 추가하여, RT 를 Whitelist 에서 제거함으로써 서버 차원의 세션 종료를 가능하게 한다.
3. RT **회전(rotation)** 의 토대를 마련한다(이번 계획에서는 회전까지 포함, 5분 grace window 는 후속 과제).

---

## 2. 설계 개요

### 2.1 접근 방식: Whitelist (Allow-list)

- **선택 이유:** RT 만료(7일)와 발급 횟수가 사용자당 한정적이므로, 블랙리스트보다 화이트리스트가 데이터 양·질의 관점 모두에서 유리하다.
- **대조 — Blacklist 안:** 모든 검증 요청마다 폐기 목록을 조회해야 하며, 폐기되지 않은 정상 토큰을 통과시키기 위해 만료시각까지 row 를 보존해야 한다. RT 회전 정책과 결합하면 데이터 폭증.

### 2.2 토큰 식별 방식: JTI (JWT ID) Claim

- **선택 이유:**
    - JWT 표준(RFC 7519) 의 `jti` 클레임을 그대로 활용하므로 의미가 명확하다.
    - DB 에는 16~36 byte 의 UUID 만 저장하면 되므로 토큰 전체나 SHA-256 해시(64 byte)를 저장하는 것보다 간결하다.
    - 인덱스 효율과 디버깅 가시성이 좋다.
- **변경점:** [JwtTokenProvider.createRefreshToken](src/main/java/com/umc/product/global/security/JwtTokenProvider.java#L122-L136) 에서 RT 생성 시 `jti = UUID.randomUUID()` 를 부여하고, 파싱 메소드가 JTI 를 반환하도록 시그니처를 확장한다.
- **대안 — Token Hash 안:** JWT 구조 변경이 없어 가장 간단하지만, 매 검증 시 SHA-256 연산이 필요하고 디버깅 시 DB 의 hash 만 보고 토큰을 식별하기 어렵다.

### 2.3 테이블 스키마

```sql
CREATE TABLE refresh_token
(
    id         BIGSERIAL PRIMARY KEY,
    jti        UUID        NOT NULL UNIQUE,
    member_id  BIGINT      NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_member_id ON refresh_token (member_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token (expires_at);
```

설계 메모:

- `jti` 는 UNIQUE — 한 RT 는 한 행으로 식별된다.
- `member_id` 인덱스 — "특정 사용자의 모든 RT 폐기"(예: 비밀번호 변경, 전체 기기 로그아웃) 시나리오 대비.
- `expires_at` 인덱스 — 배치 정리(만료된 행 삭제) 효율 확보. 정기 cleanup 작업은 본 계획서 범위 외(후속 과제).
- 도메인 간 ID 참조 원칙([CLAUDE.md](CLAUDE.md))에 따라 `member` 테이블에 FK 를 두지 않는다.

### 2.4 패키지 구조 (Hexagonal)

`authentication` 도메인 내부에 추가한다 (인증 흐름의 일부이므로 별도 도메인을 신설하지 않음).

```
authentication/
├── domain/
│   └── RefreshToken.java                        (추가)
├── application/
│   ├── port/
│   │   ├── in/command/
│   │   │   ├── ManageAuthenticationUseCase.java        (logout 시그니처 추가)
│   │   │   └── dto/
│   │   │       └── LogoutCommand.java                  (추가)
│   │   └── out/
│   │       ├── LoadRefreshTokenPort.java               (추가)
│   │       └── SaveRefreshTokenPort.java               (추가)
│   └── service/
│       └── AuthenticationService.java                  (수정: 저장·검증·회전·삭제)
└── adapter/
    ├── in/web/
    │   ├── LogoutController.java                       (추가) ※ 또는 기존 TokenAuthenticationController 에 endpoint 추가
    │   └── dto/request/LogoutRequest.java              (추가)
    └── out/persistence/
        ├── RefreshTokenJpaRepository.java              (추가)
        └── RefreshTokenPersistenceAdapter.java         (추가)
```

> **컨트롤러 배치 결정:** RT 발급/재발급/폐기 모두 인증 라이프사이클이므로 [TokenAuthenticationController.java](src/main/java/com/umc/product/authentication/adapter/in/web/TokenAuthenticationController.java) 에 `POST /logout` 으로 묶는다. 별도 컨트롤러를 만들지 않는다.

### 2.5 도메인 모델 (`RefreshToken`)

```java

@Entity
@Table(name = "refresh_token")
@Getter
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "uuid")
    private UUID jti;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder(access = AccessLevel.PRIVATE)
    private RefreshToken(UUID jti, Long memberId, Instant expiresAt) { ...}

    public static RefreshToken issue(UUID jti, Long memberId, Instant expiresAt) {
        return RefreshToken.builder()
            .jti(jti).memberId(memberId).expiresAt(expiresAt)
            .build();
    }

    public void validateNotExpired() {
        if (Instant.now().isAfter(expiresAt)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.EXPIRED_JWT_TOKEN);
        }
    }
}
```

### 2.6 흐름 변화

#### 2.6.1 로그인 (ID/PW · OAuth)

1. 기존대로 AT/RT 를 발급한다.
2. RT 의 `jti` 와 만료시각을 `refresh_token` 테이블에 저장한다.

> 영향 파일: [CredentialAuthenticationService.loginByIdPw](src/main/java/com/umc/product/authentication/application/service/CredentialAuthenticationService.java), [OAuthAuthenticationService.accessTokenLogin](src/main/java/com/umc/product/authentication/application/service/OAuthAuthenticationService.java)

#### 2.6.2 토큰 재발급 (Rotation)

1. 요청 RT 의 서명/만료 검증 (기존 로직 유지).
2. RT 의 `jti` 가 `refresh_token` 테이블에 존재하는지 검증.
    - 없으면 `INVALID_REFRESH_TOKEN` (신규 에러코드) 으로 401 응답.
3. 기존 row 를 **삭제**하고, 새 RT 를 발급해 새 row 를 INSERT (1회용).
4. 새 AT, RT 를 응답.

> 영향 파일: [AuthenticationService.renewAccessToken:36-52](src/main/java/com/umc/product/authentication/application/service/AuthenticationService.java#L36-L52)

#### 2.6.3 로그아웃 (신규)

- **Endpoint:** `POST /api/v1/auth/logout`
- **인증:** `@Public`. AccessToken 없이 호출할 수 있으며, 만료된 AccessToken이 Authorization 헤더에 있어도 RefreshToken만으로 처리한다.
- **Request Body:** `{ "refreshToken": "..." }`
- **동작:**
    1. RT 의 서명/만료와 `jti` 존재 여부를 검증한다.
    2. RT 의 `jti` 에 해당하는 row 를 삭제한다.
    3. 행이 존재하지 않더라도(이미 로그아웃) 멱등하게 200 을 반환한다.
- **Response:** `204 No Content` 또는 `200 OK`(빈 바디).

### 2.7 신규 에러 코드

[AuthenticationErrorCode.java](src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java) 에 추가:

- `INVALID_REFRESH_TOKEN` (HTTP 401, "JWT-0005", "유효하지 않거나 폐기된 Refresh Token 입니다.")

> 코드 번호는 기존 enum 끝에 이어서 부여한다.

---

## 3. 비범위(Out of Scope)

다음은 본 계획에 포함하지 **않는다**(후속 과제로 분리):

- RT 5분 grace window (네트워크 재시도 보호) — TODO 에 명시되어 있으나, 단순 회전 후 추가 PR 로 진행.
- 만료된 row 의 정기 cleanup 배치(스케줄러 또는 SQL job).
- 디바이스/세션 단위 다중 RT 관리(특정 디바이스만 로그아웃 등 UX). 현 스키마는 향후 `device_id` 컬럼 추가만으로 확장 가능하도록 설계됨.
- "전체 기기 로그아웃" API.
- Apple Refresh Token 흐름과의 통합 — 현재 Apple RT 는 `member_oauth.apple_refresh_token` 에 별도 저장되어 있고, 이는 Apple 서버용 RT 이므로 이번 도입 대상과 분리 운영한다.

---

## 4. 작업 단위(커밋) 분할

각 커밋은 독립적으로 빌드·테스트 가능해야 한다. Conventional Commit 형식([CLAUDE.md](CLAUDE.md) §6) 을 따른다.

### Commit 1 — `chore: refresh_token 테이블 추가`

- 신규 파일: `src/main/resources/db/migration/V2026.05.06.HH.MM__create_refresh_token_table.sql`
- 내용: §2.3 의 DDL.
- 검증: 로컬 Postgres 에 Flyway migration 적용 시 성공해야 한다 (`./gradlew flywayMigrate` 또는 `./gradlew bootRun` 자동 적용).
- **이 커밋은 코드 변경 없음** — 스키마만 도입.

### Commit 2 — `feat: refresh token 도메인 엔티티 및 포트 정의`

- 신규 파일:
    - `authentication/domain/RefreshToken.java`
    - `authentication/application/port/out/LoadRefreshTokenPort.java`
    - `authentication/application/port/out/SaveRefreshTokenPort.java`
- 포트 시그니처:
    - `LoadRefreshTokenPort#findByJti(UUID jti) -> Optional<RefreshToken>` ([CLAUDE.md](CLAUDE.md) §6 의 read 명명 규약 준수, "없을 수 있음" 의미)
    - `SaveRefreshTokenPort#save(RefreshToken)`
    - `SaveRefreshTokenPort#deleteByJti(UUID jti)`
    - (선택) `SaveRefreshTokenPort#deleteAllByMemberId(Long memberId)` — "전체 기기 로그아웃" 후속 과제 대비. 미사용이면 이번 커밋에서 제외하고 필요 시 추가.
- 검증: 컴파일 통과, 빌드 성공.

### Commit 3 — `feat: refresh token persistence adapter 구현`

- 신규 파일:
    - `authentication/adapter/out/persistence/RefreshTokenJpaRepository.java` (Spring Data JPA — `findByJti`, `deleteByJti`, `deleteByMemberId` 등 표준 메소드 명명 규약 준수)
    - `authentication/adapter/out/persistence/RefreshTokenPersistenceAdapter.java` (`@Component`, 두 Out Port 모두 구현)
- 검증: 어댑터 단위 테스트(@DataJpaTest + Testcontainers) 로 CRUD 검증.
    - `void refresh_token_저장_후_조회_성공()`
    - `void jti_로_삭제_성공()`

### Commit 4 — `feat: JWT 발급 시 jti 클레임 및 파싱 결과에 jti 포함`

- 수정 파일: [JwtTokenProvider.java](src/main/java/com/umc/product/global/security/JwtTokenProvider.java)
- 변경:
    - `createRefreshToken(Long memberId)` 내부에서 `UUID.randomUUID()` 를 `jti` 클레임으로 추가.
    - 새 record `RefreshTokenClaims(Long memberId, UUID jti, Instant expiresAt)` 도입.
    - `parseRefreshToken(String token)` 의 반환 타입을 `RefreshTokenClaims` 로 변경 (또는 `parseRefreshTokenWithJti` 신규 메소드 추가 — 호환성 우선이면 후자).
- **결정:** 기존 `parseRefreshToken` 호출지가 두 곳뿐이므로(`renewAccessToken` 만), 시그니처를 직접 변경하고 호출지도 함께 갱신한다.
- 영향 호출지: [AuthenticationService.renewAccessToken:46](src/main/java/com/umc/product/authentication/application/service/AuthenticationService.java#L46) — Commit 6 에서 함께 정비. 이 커밋에서는 일단 호출 코드를 `parsed.memberId()` 형태로 최소 수정하여 컴파일 에러를 막는다.
- 검증: 기존 인증 통합 테스트가 모두 그린이어야 한다.

### Commit 5 — `feat: 로그인 시 refresh token whitelist 등록`

- 수정 파일:
    - [CredentialAuthenticationService.java](src/main/java/com/umc/product/authentication/application/service/CredentialAuthenticationService.java) — `loginByIdPw` 끝부분에서 `SaveRefreshTokenPort.save(RefreshToken.issue(jti, memberId, expiresAt))` 호출.
    - [OAuthAuthenticationService.java](src/main/java/com/umc/product/authentication/application/service/OAuthAuthenticationService.java) — OAuth 로그인 성공 후 동일 로직.
    - 또는 두 서비스에서 중복을 피하기 위해 [AuthenticationService](src/main/java/com/umc/product/authentication/application/service/AuthenticationService.java) 또는 신규 `RefreshTokenIssuer` (private package-level component) 로 추출 가능 — **이번 커밋에서는 명시적 호출로 단순화**, 중복 발생 시 후속 리팩터링.
- 검증:
    - 기존 로그인 통합 테스트 + 새 단위 테스트 (`given_로그인_요청_when_loginByIdPw_then_refreshToken_DB_저장됨`).

### Commit 6 — `feat: refresh token 회전 및 whitelist 검증 적용`

- 수정 파일: [AuthenticationService.renewAccessToken:36-52](src/main/java/com/umc/product/authentication/application/service/AuthenticationService.java#L36-L52)
- 변경 흐름 (§2.6.2):
    1. `RefreshTokenClaims parsed = jwtTokenProvider.parseRefreshToken(rt)`
    2. `RefreshToken stored = loadRefreshTokenPort.findByJti(parsed.jti()).orElseThrow(() -> new AuthenticationDomainException(INVALID_REFRESH_TOKEN));`
    3. `stored.validateNotExpired();` (방어적, JWT 검증으로 이미 거의 보장됨)
    4. `deleteRefreshTokenPort.deleteByJti(parsed.jti());`
    5. 새 RT 발급 → 새 jti 저장.
    6. 응답.
- `@Transactional` 추가 (state-changing → CQRS Command).
- 신규 에러 코드 추가: `INVALID_REFRESH_TOKEN`.
- 검증:
    - `given_정상_RT_when_renew_then_새_AT_RT_발급_및_기존_jti_삭제`
    - `given_DB에_없는_RT_when_renew_then_INVALID_REFRESH_TOKEN`
    - `given_이미_사용된_RT_when_재차_renew_then_INVALID_REFRESH_TOKEN` (1회용 검증)

### Commit 7 — `feat: 로그아웃 API 추가`

- 신규/수정 파일:
    - [TokenAuthenticationController.java](src/main/java/com/umc/product/authentication/adapter/in/web/TokenAuthenticationController.java) 에 `POST /logout` 추가.
    - `authentication/adapter/in/web/dto/request/LogoutRequest.java` (record + `@NotBlank String refreshToken`).
    - [ManageAuthenticationUseCase.java](src/main/java/com/umc/product/authentication/application/port/in/command/ManageAuthenticationUseCase.java) 에 `void logout(LogoutCommand command)` 추가.
    - `application/port/in/command/dto/LogoutCommand.java` (record `LogoutCommand(String refreshToken)`).
    - [AuthenticationService.java](src/main/java/com/umc/product/authentication/application/service/AuthenticationService.java) 에 `logout` 구현.
- 동작 (§2.6.3):
    - JwtTokenProvider 로 RT 의 jti 추출.
    - `deleteRefreshTokenPort.deleteByJti(jti)` 호출 (멱등).
    - `@Transactional` 적용.
- 신규 에러 코드: `INVALID_REFRESH_TOKEN`.
- 검증:
    - `given_RT_when_logout_then_RT_DB삭제_및_200`
    - `given_이미_로그아웃된_RT_when_logout_then_200` (멱등)
    - `given_AT_없이_요청_when_logout_then_200` (Spring Security 통과 검증)
    - `given_만료된_AT_헤더_when_logout_then_200`

---

## 5. 테스트 전략

| 레이어               | 도구                                         | 주요 케이스                                     |
|-------------------|--------------------------------------------|--------------------------------------------|
| 도메인               | JUnit5                                     | `RefreshToken.issue`, `validateNotExpired` |
| 어댑터 (Persistence) | `@DataJpaTest` + Testcontainers (Postgres) | CRUD, jti 유니크 제약                           |
| 어댑터 (Web)         | `@WebMvcTest` + MockMvc                   | logout, renew 엔드포인트                        |
| 서비스               | `@ExtendWith(MockitoExtension.class)`      | renew 회전, logout 멱등성, owner 검증             |
| End-to-End        | `@SpringBootTest`                          | 로그인 → 재발급 → 로그아웃 → 재발급 실패 시나리오             |

`@DisplayName` 은 **모두 한국어**, Given/When/Then 구조 준수([CLAUDE.md](CLAUDE.md) §4).

---

## 6. 위험 요소 및 완화

| 위험                                       | 영향                   | 완화                                                                                                                               |
|------------------------------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------|
| 기존 RT(이미 클라이언트가 보유)는 jti 가 없어 회전 시 즉시 실패 | 배포 직후 모든 사용자 강제 재로그인 | (a) 배포 윈도우를 한가한 시간대로, (b) 공지, (c) `parseRefreshToken` 에서 jti null 인 경우 임시로 통과시키는 마이그레이션 모드 플래그 — 단, 후자는 보안 약화. 본 계획은 (a)+(b) 권장. |
| logout 트랜잭션 실패 후 클라이언트가 다시 호출            | 데이터 정합성              | DELETE 는 멱등하므로 문제 없음.                                                                                                            |
| RT 탈취 후 공격자가 먼저 renew → 정상 사용자 차단        | 사용자 경험 저하            | 본 계획은 탐지·차단까지 다루지 않음. RT rotation reuse detection 은 후속 과제.                                                                       |
| jti UUID 충돌                              | 사실상 0                | UNIQUE 제약 + 충돌 시 예외 → 호출자 재시도 없음, 5xx 로 자연 노출.                                                                                   |

---

## 7. 롤백 전략

각 커밋이 독립적이므로 `git revert <commit>` 로 단계별 롤백 가능. 단, **Commit 1(스키마)** 은 revert 후 별도 down-migration 이 필요하므로 다음 절차 권장:

1. Commit 6, 7 의 코드를 revert → renew/logout 흐름이 whitelist 미사용으로 회귀.
2. 운영 안정 확인 후 Commit 1, 2, 3, 5 순으로 정리.
3. 테이블 자체는 데이터 손실을 막기 위해 즉시 drop 하지 않고 일정 보관 후 제거하는 별도 마이그레이션 발행.

---

## 8. 참고

- [CLAUDE.md](CLAUDE.md) — 아키텍처 규칙, 명명 규약, 커밋/PR 컨벤션
- [JwtTokenProvider.java](src/main/java/com/umc/product/global/security/JwtTokenProvider.java) — 토큰 발급/검증 진입점
- [GlobalExceptionHandler.java](src/main/java/com/umc/product/global/exception/GlobalExceptionHandler.java) — 신규 에러코드 노출 경로
