# OAuth Provider OIDC 통합 전환 작업 계획

> 작성일: 2026-05-06
> 영역: `authentication` 도메인
> 상태: Backlog (초안)

---

## 1. 배경 및 문제 정의

### 1-1. 현재 상태 (검증된 사실)

| 항목                                                  | 현재 구현                                                                                   | 위치                                                                                                                                                                                                                                                                                 |
|-----------------------------------------------------|-----------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Apple `client_secret` 생성                            | **로그인 시마다 ES256 JWT 새로 서명**                                                             | [AppleTokenVerifier.java:216-236](../../src/main/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifier.java#L216-L236)                                                                                                                                       |
| Apple JWKS 조회                                       | **요청마다 `https://appleid.apple.com/auth/keys` 호출**                                       | [AppleTokenVerifier.java:264-287](../../src/main/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifier.java#L264-L287)                                                                                                                                       |
| Apple private key                                   | 메모리 캐시 (`cachedPrivateKey`)                                                             | [AppleTokenVerifier.java:309-329](../../src/main/java/com/umc/product/authentication/adapter/out/external/AppleTokenVerifier.java#L309-L329)                                                                                                                                       |
| Apple 로그인 입력                                        | **`authorizationCode` (idToken 아님)** + `clientType`                                     | [AppleLoginRequest.java](../../src/main/java/com/umc/product/authentication/adapter/in/web/dto/request/AppleLoginRequest.java)                                                                                                                                                     |
| Google 로그인 입력                                       | `accessToken` 검증 (OIDC idToken 미사용, deprecated 메서드 잔존)                                  | [GoogleTokenVerifier.java](../../src/main/java/com/umc/product/authentication/adapter/out/external/GoogleTokenVerifier.java)                                                                                                                                                       |
| Kakao 로그인 입력                                        | `accessToken` 검증 (`/v2/user/me`)                                                        | [KakaoTokenVerifier.java](../../src/main/java/com/umc/product/authentication/adapter/out/external/KakaoTokenVerifier.java)                                                                                                                                                         |
| Provider 추상화                                        | **없음**. `OAuthTokenVerificationAdapter`가 enum switch로 분기                                | [OAuthTokenVerificationAdapter.java](../../src/main/java/com/umc/product/authentication/adapter/out/external/OAuthTokenVerificationAdapter.java)                                                                                                                                   |
| Apple refresh token 저장                              | `MemberOAuth.appleRefreshToken` (평문, 512자)                                              | [MemberOAuth.java:44-45](../../src/main/java/com/umc/product/authentication/domain/MemberOAuth.java#L44-L45)                                                                                                                                                                       |
| Apple revoke API 시그니처                               | **버그**: Port는 `(refreshToken, clientId)` 2-arg, Adapter/Service는 1-arg 호출 → clientId 유실 | [RevokeOAuthTokenPort.java](../../src/main/java/com/umc/product/authentication/application/port/out/RevokeOAuthTokenPort.java), [OAuthAuthenticationService.java:181](../../src/main/java/com/umc/product/authentication/application/service/OAuthAuthenticationService.java#L181) |
| OIDC discovery (`.well-known/openid-configuration`) | 미사용. 모든 엔드포인트 하드코딩                                                                      | —                                                                                                                                                                                                                                                                                  |

### 1-2. 핵심 문제

1. **네트워크 비용**: Apple 로그인 1회당 최소 2회의 외부 호출 (token endpoint + JWKS), 매번 client_secret JWT 서명.
2. **이종 검증 모델**: Apple은 authorization code, Google/Kakao는 access token. 코드 경로가 provider마다 갈라짐.
3. **회원가입과 로그인의 결합**: 매 로그인마다 authorization code → token endpoint 교환을 하면서 refresh token도 함께 들고 옴. 이미 가입한 회원의 로그인에서는 refresh token을 무의미하게 갱신/폐기 처리.
4. **확장성**: 신규 OIDC provider 추가 시 `OAuthTokenVerificationAdapter`의 switch가 계속 부풀어 오름.

---

## 2. 목표 / 비목표

### 목표 (In-Scope)

- [G1] Apple 로그인 시 외부 호출 횟수를 **2회 → 0~1회**로 감축 (캐시 적중 시 0회).
- [G2] OIDC `id_token` 검증 경로를 **Provider 공통 추상화**로 통합.
- [G3] Apple **회원가입(최초 가입)** 흐름에서만 authorization code를 받아 refresh token을 획득·저장. 로그인 흐름은 idToken만 검증.
- [G4] Apple revoke 시그니처 버그를 수정하면서 `MemberOAuth`의 refresh token 저장을 일반화·암호화.
- [G5] OIDC discovery + JWKS 캐싱(공유 캐시)으로 Google/Kakao(가능한 범위)·Apple을 동일 메커니즘으로 처리.

### 비목표 (Out-of-Scope)

- Kakao OIDC 전면 전환 (Kakao는 OIDC 옵션이 제한적이고 별도 구성 필요 → 본 계획에서는 인터페이스만 통합하고 검증 방식은 유지).
- 모바일 클라이언트 측 변경 (요구되는 경우 별도 PR로 분리).
- 기존 `AccessTokenLoginCommand`/`OAuthLoginCommand` deprecated 정리는 후속 작업.

---

## 3. 제안 아키텍처

### 3-1. 공통 추상화 도입

```java
// application/port/out/
public interface OAuthIdentityProvider {
    OAuthProvider provider();

    /** id_token 또는 access_token을 검증하여 사용자 식별 정보를 반환한다. */
    OAuth2Attributes verifyIdentity(VerifyIdentityCommand command);

    /** authorization code를 refresh token으로 교환한다. (지원 provider만) */
    Optional<RefreshTokenBundle> exchangeAuthorizationCode(ExchangeCodeCommand command);

    /** 연동 해제 시 호출. provider별 revoke/unlink를 캡슐화. */
    void revoke(RevokeContext context);
}
```

| Provider | `verifyIdentity` 입력   | `exchangeAuthorizationCode` |
|----------|-----------------------|-----------------------------|
| Apple    | `id_token` (OIDC)     | 지원 (회원가입 시)                 |
| Google   | `id_token` (OIDC)     | 미지원 (현재는)                   |
| Kakao    | `access_token` (REST) | 미지원                         |

### 3-2. JWKS / OIDC Discovery 캐시

- 공통 빈 `OidcMetadataCache` (provider 별 `.well-known/openid-configuration` + JWKS).
- TTL: 발급자가 제공하는 `Cache-Control: max-age` 우선, 미제공 시 기본 6시간.
- 라이브러리: **Nimbus JOSE+JWT** + Spring Security `JwtDecoder` (OAuth2 Resource Server 의존성 추가). 자체 파싱(`extractKidFromToken`, `buildRsaPublicKey`) 코드 제거.
- 동시성: `Caffeine` 캐시 + `loadIfAbsent` 패턴 (현재 `cachedPrivateKey` 패턴은 thread-unsafe).

### 3-3. Apple `client_secret` JWT 캐시

- key: `clientId` (iOS/Web 분리)
- value: 1시간 만료 ES256 JWT
- 캐시 TTL: **50분** (5분의 안전 버퍼). 만료 임박 시 비동기 갱신 옵션은 v2.
- 캐시는 동일 `OidcMetadataCache`와는 분리(서명 키 라이프사이클이 다름).

### 3-4. Apple 회원가입 / 로그인 분기

현재 `AuthenticationController#appleOAuthLogin`은 단일 엔드포인트가 둘 다 처리. 분리 권고:

| 시나리오             | 입력                                                | 동작                                                                         |
|------------------|---------------------------------------------------|----------------------------------------------------------------------------|
| **로그인 (기존 회원)**  | `id_token` (+ `clientType`)                       | OIDC idToken 검증 → `MemberOAuth` 매칭 → 즉시 토큰 발급. 외부 호출은 JWKS 캐시 미스 시 1회.     |
| **회원가입 (최초 가입)** | `id_token` + `authorizationCode` (+ `clientType`) | idToken 검증 → 신규 회원 판단 시 token endpoint로 code 교환 → refresh token 저장 → 가입 처리 |

> **엣지 케이스**: 클라이언트가 항상 `(idToken, authorizationCode)` 페어를 보내도 서버는 신규 회원 판정 시에만 code를 교환한다. 이미 회원이라면 code는 무시(또는 refresh token이 NULL이면 보충 갱신).

#### 신규 vs 기존 판정 위치

`OAuthAuthenticationService.loginWithOAuth2Attributes`가 이미 신규/기존을 분기 (line 41-71). 여기에 신규일 때만 `exchangeAuthorizationCode` 호출하는 hook 삽입.

### 3-5. 데이터 모델 일반화

- `member_oauth.apple_refresh_token` → **`provider_refresh_token`** 으로 컬럼 리네임.
- 암호화: JPA `AttributeConverter` 기반 AES-256-GCM (키는 `APP_DATA_ENCRYPTION_KEY` env). 암호화 컨버터는 별도 PR로 도입 가능 시 그쪽에 의존.
- 기존 `appleRefreshToken` 데이터는 동일 컬럼 데이터 그대로 의미만 일반화 (rename은 무손실).

### 3-6. revoke 버그 수정

- `MemberOAuth`에 `clientType` (또는 `clientId`) 컬럼을 함께 저장(가입 시 사용한 값). revoke 시 이 값을 참조하여 client_secret 재생성.
- 또는 모든 client_secret JWT를 단일 clientId로 통합할 수 있다면 시그니처 자체를 1-arg로 정정.

---

## 4. 작업 단위 (커밋 분할)

> 커밋 메시지는 [CLAUDE.md](../../CLAUDE.md) 규약을 따름: `<type>: <subject>` (Conventional Commits)
> PR 단위는 `[Feat]/[Refactor]/[Fix]/[Chore]` 태그 사용.
> 모든 커밋은 빌드/테스트가 통과한 상태로 머지 가능해야 함 (Trunk-friendly, behind feature toggle if needed).

### Phase 0 — 추상화 레이어 도입 (동작 변경 없음)

1. **`refactor: OAuthIdentityProvider 인터페이스 및 DTO 도입`**
    - `application/port/out/OAuthIdentityProvider.java` 신규
    - `VerifyIdentityCommand`, `ExchangeCodeCommand`, `RefreshTokenBundle`, `RevokeContext` record 추가
    - 기존 `VerifyOAuthTokenPort`, `RevokeOAuthTokenPort`는 `@Deprecated` 표시만 하고 유지
    - 구현체 0개. 컴파일·테스트 그린만 확인.

2. **`refactor: OAuthTokenVerificationAdapter를 provider 단위로 분리`**
    - `Apple/Google/KakaoIdentityProvider` 클래스 생성 (기존 Verifier를 위임 호출)
    - `OAuthTokenVerificationAdapter`는 `Map<OAuthProvider, OAuthIdentityProvider>` 주입으로 단순화
    - 기능 변경 없음, 동작 동일 회귀 테스트 통과 필수.

### Phase 1 — OIDC 인프라

3. **`chore: nimbus-jose-jwt / spring-security-oauth2-jose 의존성 추가`**
    - `build.gradle.kts` 수정만. 실제 사용은 다음 커밋부터.

4. **`feat: OidcMetadataCache 도입 (discovery + JWKS 캐싱)`**
    - `adapter/out/external/oidc/OidcMetadataCache.java`
    - Caffeine 기반, TTL 동적 결정
    - 단위 테스트: WireMock으로 JWKS 응답을 stub하고 캐시 적중 동작 검증

5. **`feat: Apple OIDC IdToken 검증 경로 추가 (병렬 운용)`**
    - `AppleIdentityProvider#verifyIdentity` 구현체에 OIDC idToken 검증 분기 추가
    - 기존 authorization code 경로는 유지 (feature flag `auth.apple.use-oidc-login` 도입, default `false`)
    - 통합 테스트: stub JWKS로 검증 성공/실패 케이스 모두 커버

6. **`feat: Google OIDC IdToken 검증 경로 추가`**
    - 기존 access token 경로 유지하되 idToken 검증 메서드 추가
    - 클라이언트 마이그레이션 시점까지 둘 다 지원
    - feature flag `auth.google.use-oidc-login`

### Phase 2 — Apple client_secret 캐시

7. **`perf: Apple client_secret JWT 캐싱 도입`**
    - `AppleClientSecretCache` 빈
    - TTL 50분 (만료까지 10분 버퍼 보장)
    - 메트릭: 캐시 히트율(`auth_apple_client_secret_cache_hit_total`)
    - 단위 테스트: 동일 clientId 100회 호출 시 서명은 1회만 수행되는지 검증

### Phase 3 — 회원가입/로그인 분기

8. **`refactor: AppleLoginRequest를 idToken 기반으로 확장 (호환성 유지)`**
    - 신규 필드 `idToken` 추가, 기존 `authorizationCode`는 optional로 변경
    - validation: 둘 중 하나는 필수, 회원가입 분기 결정 시 idToken 우선
    - **마이그레이션 윈도우**: 한 릴리즈 동안 둘 다 허용

9. **`feat: OAuth 회원가입과 로그인을 별개 UseCase로 분리`**
    - `OAuthLoginUseCase` (기존 회원만 허용, 외부 호출 ≤ 1)
    - `OAuthSignupUseCase` (신규 회원 + Apple은 authorizationCode 교환)
    - 컨트롤러는 `loginWithOAuth2Attributes` 결과의 `newMember` 여부에 따라 routing (기존 단일 엔드포인트 유지하면서 내부 분기)

10. **`feat: Apple 회원가입 시 authorization code 교환 흐름 적용`**
    - `AppleIdentityProvider#exchangeAuthorizationCode` 구현
    - 신규 회원 가입 트랜잭션 안에서 호출 → refresh token 저장
    - 기존 회원이 refresh token을 갖고 있지 않은 경우(과거 가입자) 보충 갱신 옵션 (`if appleRefreshToken == null && code present then exchange`)

11. **`feat: Apple 로그인 흐름을 OIDC idToken 검증으로 전환`**
    - feature flag `auth.apple.use-oidc-login=true`로 단계적 활성화
    - 기존 authorization code 경로는 회원가입 전용으로 좁아짐
    - 점진 롤아웃: 카나리 비율 → 100%

### Phase 4 — 데이터 모델 일반화

12. **`feat: member_oauth refresh token 컬럼 일반화 (Flyway)`**
    - `V2026.MM.DD.HH.mm__rename_apple_refresh_token_to_provider_refresh_token.sql`
    - `ALTER TABLE member_oauth RENAME COLUMN apple_refresh_token TO provider_refresh_token;`
    - 엔티티 필드 동시 리네임 (`appleRefreshToken` → `providerRefreshToken`)
    - 도메인 메서드 `updateRefreshToken` 시그니처 유지

13. **`feat: provider refresh token 암호화 (AES-256-GCM AttributeConverter)`**
    - `EncryptedStringConverter` 도입 (전사 재사용 가능하도록 `global/persistence/`)
    - 마이그레이션: 기존 평문 데이터 일괄 암호화 (Flyway Java migration 또는 ApplicationRunner one-shot)
    - 키 관리: `APP_DATA_ENCRYPTION_KEY` env (16/24/32 bytes Base64)

14. **`feat: member_oauth에 client_type 컬럼 추가`**
    - revoke 시 사용한 client_id를 식별하기 위한 정보
    - `V2026.MM.DD.HH.mm__add_client_type_to_member_oauth.sql`
    - 회원가입 시 함께 저장. 기존 행은 NULL → revoke 시 fallback (web client) 처리

### Phase 5 — Revoke 정리 / 레거시 제거

15. **`fix: Apple revoke 시 clientId 누락 버그 수정`**
    - `RevokeContext`에 `clientType`/`clientId` 포함
    - `OAuthAuthenticationService#revokeProviderToken` 호출 경로 정정
    - 회귀 테스트: 저장된 client_type을 사용해 client_secret이 정확한 audience로 서명되는지

16. **`refactor: VerifyOAuthTokenPort / RevokeOAuthTokenPort 폐기`**
    - 모든 호출자가 `OAuthIdentityProvider`로 이전된 후 deprecated 인터페이스/구현 삭제
    - Adapter도 동일하게 정리

17. **`chore: Apple/Google/Kakao TokenVerifier 내부 코드 정리`**
    - 자체 JWT 파싱 코드 제거 (Nimbus 사용으로 일원화)
    - dead code 정리

### Phase 6 — 관측 / 문서화

18. **`feat: OIDC verification 메트릭 및 구조화 로그 추가`**
    - Micrometer 카운터: 검증 성공/실패, 외부 호출 횟수, JWKS 캐시 히트율
    - 트레이싱: 각 verifier 호출에 span 추가 (이미 OpenTelemetry 인프라 있음)

19. **`docs: OAuth/OIDC 아키텍처 문서 업데이트`**
    - `docs/guides/OAUTH_OIDC_ARCHITECTURE.md` 신규
    - 본 백로그 문서를 history로 정리

---

## 5. 마이그레이션 / 롤백 전략

### 5-1. 단계별 feature flag

| Flag                              | 기본값   | 의미                                      |
|-----------------------------------|-------|-----------------------------------------|
| `auth.apple.use-oidc-login`       | false | true 시 Apple 로그인은 OIDC idToken 사용       |
| `auth.google.use-oidc-login`      | false | true 시 Google 로그인은 idToken 사용           |
| `auth.oauth.signup-code-exchange` | true  | false 시 authorization code 교환 비활성 (롤백용) |

### 5-2. 롤백 트리거

1. OIDC 검증 실패율 > 1% (5분 평균) → flag off + 경보
2. JWKS 캐시 만료 후 첫 응답 지연 > 2초 → 캐시 TTL 단축, fetch 타임아웃 강화
3. Refresh token 암호화 마이그레이션 중 실패 발생 → migration 중단, 데이터 검수

### 5-3. DB 변경 안전성

- 컬럼 rename은 별도 릴리즈로 격리 (구버전 코드도 새 컬럼명 인식하도록 사전 배포 → 그 다음 릴리즈에서 rename)
- 또는 무중단을 위해 **add new column → dual-write → backfill → switch read → drop old** 5단계 분할 (대규모 사용자 시 권장)

---

## 6. 테스트 전략

| 레이어 | 도구                        | 핵심 케이스                                                                     |
|-----|---------------------------|----------------------------------------------------------------------------|
| 단위  | JUnit 5 + Mockito         | OidcMetadataCache TTL/동시성, ClientSecretCache 만료, AppleIdentityProvider 분기  |
| 통합  | Testcontainers + WireMock | Apple/Google JWKS endpoint stub, token endpoint stub, revoke endpoint stub |
| 회귀  | MockMvc                  | 기존 `POST /api/v1/auth/login/apple` 응답 스키마 보존 (호환 윈도우 동안)                   |
| 성능  | k6 또는 Gatling             | 100 req/s 로그인 부하에서 외부 호출 횟수 ≤ 0.05 req/login (캐시 정상 동작)                    |

> 모든 테스트는 한국어 `@DisplayName` 사용 (CLAUDE.md 규약).

---

## 7. 위험 / 미해결 결정 사항

| ID             | 위험                                                                                                | 대응                                     |
|----------------|---------------------------------------------------------------------------------------------------|----------------------------------------|
| R1             | Apple JWKS 키 회전 시점에 캐시가 stale → 인증 실패                                                             | JWKS 검증 실패 시 캐시 강제 무효화 후 1회 재시도        |
| R2             | client_secret JWT 캐시가 prod 인스턴스마다 분리 → 키 교체 시 일시적 불일치                                             | 만료 임박 시 lazy 재생성으로 인스턴스별 자가 회복         |
| R3             | Refresh token 암호화 키 분실 = 모든 Apple revoke 불가                                                       | 암호화 키는 KMS 또는 Secret Manager 관리, 백업 필수 |
| R4             | Kakao는 OIDC 미흡 → 통합 인터페이스에서 `exchangeAuthorizationCode = Optional.empty()` 반환. 추후 OIDC 전환 시 별도 PR | —                                      |
| R5             | 기존 회원이 idToken 없이 authorizationCode만 보낼 경우 (구버전 클라이언트)                                            | Phase 3에서 호환 윈도우 1릴리즈 유지               |
| **D1 (결정 필요)** | client_id를 iOS/Web 단일로 통합할지, 분리 유지할지                                                              | iOS/Android 클라이언트 팀 협의 필요              |
| **D2 (결정 필요)** | 회원가입과 로그인 엔드포인트 분리 vs 단일 엔드포인트 내부 분기                                                              | API 호환성 vs 의미론적 명확성 트레이드오프             |

---

## 8. 일정 가이드 (참고)

| Phase   | 추정 작업량     | 선후 관계             |
|---------|------------|-------------------|
| Phase 0 | 0.5 d      | 선결                |
| Phase 1 | 1.5 d      | Phase 0 후         |
| Phase 2 | 0.5 d      | Phase 1 병행 가능     |
| Phase 3 | 2 d        | Phase 1, 2 후      |
| Phase 4 | 1.5 d      | Phase 3 후 (DB 변경) |
| Phase 5 | 1 d        | Phase 3, 4 후      |
| Phase 6 | 0.5 d      | 마지막               |
| **합계**  | **~7.5 d** |                   |

---

## 9. PR 분할 가이드

각 Phase는 1개의 PR로 묶지 않고, 위 커밋 단위가 곧 PR 단위(또는 2~3개 묶음). 다음 원칙을 따른다:

1. 한 PR은 **하나의 의미적 변경**만 포함 (커밋이 여러 개여도 변경의 의도는 하나).
2. **DB 마이그레이션이 포함된 PR은 코드 변경과 분리** (배포 순서 제어).
3. **Feature flag로 보호되지 않는 동작 변경은 단독 PR**로 (롤백 단위 명확화).
4. PR 제목 예:
    - `[Refactor] OAuthIdentityProvider 인터페이스 도입`
    - `[Feat] Apple OIDC idToken 검증 경로 추가 (flag 보호)`
    - `[Fix] Apple revoke 시 clientId 누락 버그 수정`
    - `[Chore] Apple client_secret JWT 캐시 도입`

---

## 10. 종료 기준 (Definition of Done)

- [ ] Apple 로그인 1회당 외부 호출 평균 ≤ 0.1회 (캐시 히트 시 0회)
- [ ] Apple 회원가입 시에만 authorization code 교환이 일어나며 refresh token이 저장됨
- [ ] `OAuthIdentityProvider` 인터페이스를 통해 Google/Apple/Kakao가 동일 호출 경로로 처리됨
- [ ] `MemberOAuth.providerRefreshToken`이 암호화 컬럼으로 저장됨
- [ ] Apple revoke가 저장된 client_type을 사용하여 정상 동작함을 통합 테스트로 검증
- [ ] OIDC 관련 메트릭이 Prometheus에 노출되고 대시보드 1종 이상 추가됨
- [ ] `docs/guides/OAUTH_OIDC_ARCHITECTURE.md` 신규 문서 작성 완료
