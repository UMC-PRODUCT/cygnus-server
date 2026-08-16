# Authentication 테스트 케이스

- 테스트 파일: 14개
- 테스트 케이스: 71개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 4 |
| UseCase / Application Service | 37 |
| Repository / Outbound Persistence | 4 |
| Scheduler | 1 |
| Domain | 25 |

## Controller / Inbound Adapter

### LoginByEmailRequestTest
- 위치: `src/test/java/com/umc/product/authentication/adapter/in/web/dto/request/LoginByEmailRequestTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [13](../../../src/test/java/com/umc/product/authentication/adapter/in/web/dto/request/LoginByEmailRequestTest.java#L13) | 이메일/PW 로그인 요청의 clientType 을 커맨드로 전달한다 | 조건 이메일/PW 로그인 요청의 clientType 을 커맨드로 전달한다 | 성공: 검증 assertThat(command.email()).isEqualTo(EMAIL); assertThat(command.rawPassword()).isEqualTo(RAW_PASSWORD); assertThat(command.clientType()).isEqualTo(ClientType.ANDROID); |

### TokenAuthenticationControllerTest
- 테스트 설명: TokenAuthenticationController
- 위치: `src/test/java/com/umc/product/authentication/adapter/in/web/TokenAuthenticationControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [19](../../../src/test/java/com/umc/product/authentication/adapter/in/web/TokenAuthenticationControllerTest.java#L19) | 로그아웃은 Authorization 헤더 없이 RefreshToken만으로 성공한다 | HTTP POST /api/v1/auth/logout; body { "refreshToken": "refresh-token" } | 성공: HTTP 200 OK |
| [42](../../../src/test/java/com/umc/product/authentication/adapter/in/web/TokenAuthenticationControllerTest.java#L42) | TokenAuthenticationController / 로그아웃은 만료된 AccessToken이 Authorization 헤더에 있어도 RefreshToken만으로 성공한다 | HTTP POST /api/v1/auth/logout; body { "refreshToken": "refresh-token" } | 성공: HTTP 200 OK; 에러코드 AuthenticationErrorCode.EXPIRED_JWT_TOKEN |
| [64](../../../src/test/java/com/umc/product/authentication/adapter/in/web/TokenAuthenticationControllerTest.java#L64) | TokenAuthenticationController / 로그아웃 요청의 refreshToken이 blank이면 400을 반환한다 | HTTP POST /api/v1/auth/logout; body { "refreshToken": " " } | 실패: HTTP 400 Bad Request |

## UseCase / Application Service

### AuthenticationServiceTest
- 위치: `src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [103](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L103) | allow-list에 있는 RefreshToken이면 기존 jti를 삭제하고 새 토큰을 발급한다 | RenewAccessTokenCommand {refreshToken=REFRESH_TOKEN} | 성공: 검증 assertThat(result).isSameAs(newTokens); |
| [134](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L134) | renewAccessToken / 조회 후 삭제할 RefreshToken row가 없으면 INVALID_REFRESH_TOKEN 예외를 던진다 | RenewAccessTokenCommand {refreshToken=REFRESH_TOKEN} | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_REFRESH_TOKEN; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN); |
| [159](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L159) | allow-list에 없는 RefreshToken이면 INVALID_REFRESH_TOKEN 예외를 던진다 | RenewAccessTokenCommand {refreshToken=REFRESH_TOKEN} | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_REFRESH_TOKEN; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN); |
| [184](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L184) | RefreshToken의 jti를 삭제한다 | 호출 logout(LogoutCommand.from(REFRESH_TOKEN)) | 성공: RefreshToken의 jti를 삭제한다 |
| [200](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L200) | logout / 이미 삭제된 RefreshToken이어도 멱등하게 성공한다 | 호출 logout(LogoutCommand.from(REFRESH_TOKEN))) | 성공: logout / 이미 삭제된 RefreshToken이어도 멱등하게 성공한다 |
| [216](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L216) | logout / 잘못된 이메일 형식이면 INVALID_EMAIL_FORMAT 예외를 던지고 저장/이벤트 발행하지 않는다 | 조건 logout / 잘못된 이메일 형식이면 INVALID_EMAIL_FORMAT 예외를 던지고 저장/이벤트 발행하지 않는다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_FORMAT; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_FORMAT); |
| [233](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L233) | createEmailVerificationSession / REGISTER + 미가입 이메일이면 세션을 저장하고 메일 발송 이벤트를 발행한다 | 호출 createEmailVerificationSession(EMAIL, EmailVerificationPurpose.REGISTER) | 성공: 검증 assertThat(sessionId).isEqualTo(SESSION_ID); |
| [250](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L250) | createEmailVerificationSession / REGISTER + 이미 가입된 이메일이면 EMAIL_ALREADY_EXISTS 예외, 저장/이벤트 발행 모두 차단 | 호출 createEmailVerificationSession(EMAIL, EmailVerificationPurpose.REGISTER)) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.EMAIL_ALREADY_EXISTS; 검증 .isEqualTo(AuthenticationErrorCode.EMAIL_ALREADY_EXISTS); |
| [267](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L267) | createEmailVerificationSession / PASSWORD_RESET + 자격증명 존재하면 세션을 저장하고 메일 발송 이벤트를 발행한다 | 호출 createEmailVerificationSession(EMAIL, EmailVerificationPurpose.PASSWORD_RESET) | 성공: 검증 assertThat(sessionId).isEqualTo(SESSION_ID); |
| [285](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L285) | CHANGE_EMAIL + 미사용 이메일이면 세션을 저장하고 메일 발송 이벤트를 발행한다 | 호출 createEmailVerificationSession(EMAIL, EmailVerificationPurpose.CHANGE_EMAIL) | 성공: 검증 assertThat(sessionId).isEqualTo(SESSION_ID); |
| [302](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L302) | CHANGE_EMAIL + 이미 사용 중인 이메일이면 EMAIL_ALREADY_EXISTS 예외, 저장/이벤트 발행 모두 차단 | 호출 createEmailVerificationSession(EMAIL, EmailVerificationPurpose.CHANGE_EMAIL)) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.EMAIL_ALREADY_EXISTS; 검증 .isEqualTo(AuthenticationErrorCode.EMAIL_ALREADY_EXISTS); |
| [319](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L319) | 직전 발송으로부터 60초 이내면 EMAIL_VERIFICATION_THROTTLED 예외로 거부한다 | 호출 createEmailVerificationSession(EMAIL, EmailVerificationPurpose.REGISTER)) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.EMAIL_VERIFICATION_THROTTLED; 검증 .isEqualTo(AuthenticationErrorCode.EMAIL_VERIFICATION_THROTTLED); |
| [340](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L340) | PASSWORD_RESET + 자격증명 미존재면 세션은 저장하되 이벤트는 발행하지 않는다 (enumeration 방어) | 호출 createEmailVerificationSession(EMAIL, EmailVerificationPurpose.PASSWORD_RESET) | 성공: 검증 assertThat(sessionId).isEqualTo(SESSION_ID); |
| [360](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L360) | 정답 코드 검증 시 세션의 purpose 로 emailVerificationToken 을 발급한다 | ValidateEmailVerificationSessionCommand {sessionId=SESSION_ID, code=CODE}; 호출 validateEmailVerificationSession(command) | 성공: 검증 assertThat(issued).isEqualTo(ISSUED_TOKEN); assertThat(session.isVerified()).isTrue(); |
| [387](../../../src/test/java/com/umc/product/authentication/application/service/AuthenticationServiceTest.java#L387) | validateEmailVerificationSession / 틀린 코드면 INVALID_EMAIL_VERIFICATION 예외를 던지고 토큰을 발급하지 않는다 | ValidateEmailVerificationSessionCommand {sessionId=SESSION_ID, code="000000"}; 호출 validateEmailVerificationSession(command)) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); assertThat(session.getAttemptCount()).isEqualTo(1); |

### CredentialAuthenticationServiceTest
- 위치: `src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [65](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java#L65) | 인코딩된 비밀번호로 회원 자격증명을 등록한다 | 호출 registerCredentialByEmail(command) | 성공: 검증 assertThat(captured.memberId()).isEqualTo(MEMBER_ID); assertThat(captured.encodedPassword()).isEqualTo(ENCODED_PASSWORD); |
| [91](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java#L91) | 자격 증명 등록 (이메일 기반) / 현재 비밀번호가 일치하면 새 비밀번호로 교체한다 | 호출 changePassword(command) | 성공: 검증 assertThat(captor.getValue().memberId()).isEqualTo(MEMBER_ID); assertThat(captor.getValue().encodedPassword()).isEqualTo("{argon2}new-hash"); |
| [116](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java#L116) | changePassword / 자격증명이 등록되지 않은 회원은 INVALID_LOGIN_CREDENTIAL 로 응답한다 | 호출 changePassword(command)) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL); |
| [135](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java#L135) | changePassword / 현재 비밀번호가 다르면 INVALID_LOGIN_CREDENTIAL 로 응답한다 | 호출 changePassword(command)) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL); |
| [158](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java#L158) | 이메일로 자격증명을 가진 회원이 있으면 새 비밀번호로 교체한다 | 호출 resetPasswordByEmail(command) | 성공: 검증 assertThat(captor.getValue().memberId()).isEqualTo(MEMBER_ID); assertThat(captor.getValue().encodedPassword()).isEqualTo(NEW_ENCODED_PASSWORD); |
| [186](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java#L186) | 이메일 인증 기반 비밀번호 초기화 / 자격증명이 없는 회원(또는 미가입 이메일)은 INVALID_LOGIN_CREDENTIAL 단일 메시지로 응답한다 | 호출 resetPasswordByEmail(command)) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL); |
| [209](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java#L209) | 이메일 인증 기반 비밀번호 초기화 / 정상 로그인 시 clientType 을 포함해 토큰 발급을 위임하고, 별도 트랜잭션의 rehashService.rehashIfNeeded 를 호출한다 | 조건 이메일 인증 기반 비밀번호 초기화 / 정상 로그인 시 clientType 을 포함해 토큰 발급을 위임하고, 별도 트랜잭션의 rehashService.rehashIfNeeded 를 호출한다 | 성공: 검증 assertThat(result.memberId()).isEqualTo(MEMBER_ID); assertThat(result.accessToken()).isEqualTo("access-token"); assertThat(result.refreshToken()).isEqualTo("refresh-token"); |
| [238](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java#L238) | 이메일/PW 로그인 / 자격증명을 찾을 수 없으면 INVALID_LOGIN_CREDENTIAL 단일 메시지를 반환한다 | 호출 loginByEmail(LoginByEmailCommand.of(EMAIL, RAW_PASSWORD))) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL); |
| [256](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAuthenticationServiceTest.java#L256) | 이메일/PW 로그인 / 비밀번호가 다르면 INVALID_LOGIN_CREDENTIAL 단일 메시지를 반환한다 | 호출 loginByEmail(LoginByEmailCommand.of(EMAIL, RAW_PASSWORD))) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL); |

### CredentialAvailabilityQueryServiceTest
- 위치: `src/test/java/com/umc/product/authentication/application/service/CredentialAvailabilityQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [29](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAvailabilityQueryServiceTest.java#L29) | 정상 형식의 미사용 이메일은 사용 가능으로 판정한다 | 호출 isEmailAvailable(EMAIL) | 성공: 검증 assertThat(available).isTrue(); |
| [42](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAvailabilityQueryServiceTest.java#L42) | 정상 형식이지만 이미 사용 중인 이메일은 사용 불가로 판정한다 | 호출 isEmailAvailable(EMAIL) | 실패: 검증 assertThat(available).isFalse(); |
| [55](../../../src/test/java/com/umc/product/authentication/application/service/CredentialAvailabilityQueryServiceTest.java#L55) | 형식이 잘못된 이메일은 사용 가능 여부를 묻지 않고 INVALID_EMAIL_FORMAT 예외를 던진다 | 호출 isEmailAvailable("not-an-email")) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_FORMAT; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_FORMAT); |

### OAuthAuthenticationServiceTest
- 테스트 설명: OAuthAuthenticationService
- 위치: `src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [38](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceTest.java#L38) | local credential이 있으면 마지막 OAuth도 해제할 수 있다 | 호출 unlinkOAuth(command(false)) | 성공: local credential이 있으면 마지막 OAuth도 해제할 수 있다 |
| [84](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceTest.java#L84) | unlinkOAuth / local credential이 없고 마지막 OAuth이면 해제할 수 없다 | 호출 unlinkOAuth(command(false))) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.OAUTH_CANNOT_UNLINK_LAST_PROVIDER; 검증 .isEqualTo(AuthenticationErrorCode.OAUTH_CANNOT_UNLINK_LAST_PROVIDER); |
| [102](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceTest.java#L102) | unlinkOAuth / local credential이 없어도 다른 OAuth가 남으면 해제할 수 있다 | 호출 unlinkOAuth(command(false)) | 성공: unlinkOAuth / local credential이 없어도 다른 OAuth가 남으면 해제할 수 있다 |
| [118](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceTest.java#L118) | 탈퇴 흐름은 credential과 마지막 OAuth 검사를 건너뛰고 해제한다 | 호출 unlinkOAuth(command(true)) | 성공: 탈퇴 흐름은 credential과 마지막 OAuth 검사를 건너뛰고 해제한다 |
| [132](../../../src/test/java/com/umc/product/authentication/application/service/OAuthAuthenticationServiceTest.java#L132) | Google access token의 providerId가 연동 계정과 다르면 연동 해제를 거부한다 | UnlinkOAuthCommand {memberId=MEMBER_ID, memberOAuthId=MEMBER_OAUTH_ID, isWithdrawal=true, googleAccessToken=GOOGLE_ACCESS_TOKEN}; 호출 unlinkOAuth(command)) | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN; 검증 .isEqualTo(AuthenticationErrorCode.OAUTH_INVALID_ACCESS_TOKEN); |

### SendVerificationEmailEventTest
- 위치: `src/test/java/com/umc/product/authentication/application/event/SendVerificationEmailEventTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [10](../../../src/test/java/com/umc/product/authentication/application/event/SendVerificationEmailEventTest.java#L10) | of 정적 팩토리는 eventId와 occurredAt을 자동 주입한다 | 조건 of 정적 팩토리는 eventId와 occurredAt을 자동 주입한다 | 성공: 검증 assertThat(event.eventId()).isNotNull(); assertThat(event.occurredAt()).isBetween(before, after); assertThat(event.email()).isEqualTo("user@example.com"); assertThat(event.verificationCode()).isEqualTo("123456"); |
| [27](../../../src/test/java/com/umc/product/authentication/application/event/SendVerificationEmailEventTest.java#L27) | of 정적 팩토리는 매번 새로운 eventId를 반환한다 | 조건 of 정적 팩토리는 매번 새로운 eventId를 반환한다 | 성공: 검증 assertThat(first.eventId()).isNotEqualTo(second.eventId()); |
| [38](../../../src/test/java/com/umc/product/authentication/application/event/SendVerificationEmailEventTest.java#L38) | eventId와 occurredAt을 명시하면 그 값이 그대로 유지된다 | 조건 eventId와 occurredAt을 명시하면 그 값이 그대로 유지된다 | 성공: 검증 assertThat(event.eventId()).isEqualTo(givenId); assertThat(event.occurredAt()).isEqualTo(givenInstant); |
| [55](../../../src/test/java/com/umc/product/authentication/application/event/SendVerificationEmailEventTest.java#L55) | eventType은 'authentication.email.verification.requested'이다 | 조건 eventType은 'authentication.email.verification.requested'이다 | 성공: 검증 assertThat(event.eventType()).isEqualTo("authentication.email.verification.requested"); |

### SendVerificationEmailOutboxFlowTest
- 테스트 설명: SendVerificationEmailEvent outbox flow
- 위치: `src/test/java/com/umc/product/authentication/application/event/SendVerificationEmailOutboxFlowTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [32](../../../src/test/java/com/umc/product/authentication/application/event/SendVerificationEmailOutboxFlowTest.java#L32) | 이메일 인증 세션 생성 시 SendVerificationEmailEvent가 event outbox로 저장된다 | 호출 createEmailVerificationSession(EMAIL, EmailVerificationPurpose.REGISTER) | 성공: 검증 assertThat(sessionId).isEqualTo(1L); assertThat(saveEventOutboxPort.saved).hasSize(1); assertThat(outbox.getEventType()).isEqualTo("authentication.email.verification.requested"); assertThat(outbox.getEventClass()).isE... |

## Repository / Outbound Persistence

### EmailVerificationPersistenceAdapterTest
- 위치: `src/test/java/com/umc/product/authentication/adapter/out/persistence/EmailVerificationPersistenceAdapterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [50](../../../src/test/java/com/umc/product/authentication/adapter/out/persistence/EmailVerificationPersistenceAdapterTest.java#L50) | 존재하면 엔티티를 반환한다 | 조건 존재하면 엔티티를 반환한다 | 성공: 검증 assertThat(result).isSameAs(session); |
| [66](../../../src/test/java/com/umc/product/authentication/adapter/out/persistence/EmailVerificationPersistenceAdapterTest.java#L66) | getById / 미존재 시 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 getById / 미존재 시 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); |

### RefreshTokenPersistenceAdapterTest
- 테스트 설명: RefreshTokenPersistenceAdapter
- 위치: `src/test/java/com/umc/product/authentication/adapter/out/persistence/RefreshTokenPersistenceAdapterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [24](../../../src/test/java/com/umc/product/authentication/adapter/out/persistence/RefreshTokenPersistenceAdapterTest.java#L24) | RefreshToken을 저장하고 jti로 조회한다 | 조건 RefreshToken을 저장하고 jti로 조회한다 | 성공: 검증 assertThat(found).isPresent(); assertThat(found.get().getMemberId()).isEqualTo(MEMBER_ID); |
| [52](../../../src/test/java/com/umc/product/authentication/adapter/out/persistence/RefreshTokenPersistenceAdapterTest.java#L52) | RefreshTokenPersistenceAdapter / jti로 RefreshToken을 삭제하며 이미 없어도 멱등하게 성공한다 | 조건 RefreshTokenPersistenceAdapter / jti로 RefreshToken을 삭제하며 이미 없어도 멱등하게 성공한다 | 성공: 검증 assertThat(firstDeleted).isTrue(); assertThat(secondDeleted).isFalse(); assertThat(adapter.findByJti(JTI)).isEmpty(); |

## Scheduler

### EmailVerificationRetentionSchedulerTest
- 위치: `src/test/java/com/umc/product/authentication/adapter/in/scheduler/EmailVerificationRetentionSchedulerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [32](../../../src/test/java/com/umc/product/authentication/adapter/in/scheduler/EmailVerificationRetentionSchedulerTest.java#L32) | purge 호출 시 현재 - 7일 이전 만료 레코드 삭제를 위임한다 | 조건 purge 호출 시 현재 - 7일 이전 만료 레코드 삭제를 위임한다 | 실패: 검증 assertThat(threshold).isBetween( |

## Domain

### CredentialPolicyTest
- 위치: `src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [22](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L22) | 정상 형식의 이메일은 통과한다 | 파라미터 @ValueSource strings = { "user@example.com", "user.name@example.com", "user+tag@example.co.kr", "user_name@example.com", "user-name@sub.example.com", "u@a.io" } | 성공: 정상 형식의 이메일은 통과한다 |
| [39](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L39) | validateEmail / 형식이 어긋나면 INVALID_EMAIL_FORMAT 예외를 던진다 | 파라미터 @ValueSource strings = { "user", // @ 없음 "user@", // 도메인 없음 "@example.com", // 로컬 파트 없음 "user@example", // TLD 없음 "user@.com", // 도메인 시작이 점 "user name@example.com", // 공백 포함 "user@exa mple.com", // 도메인에 공백 "user@@example.com", // @ 중복 "한글@example.com" // 비ASCII 로컬 파트 } | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_FORMAT; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_FORMAT); |
| [59](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L59) | validateEmail / null 이메일은 예외를 던진다 | 조건 validateEmail / null 이메일은 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_FORMAT; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_FORMAT); |
| [68](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L68) | validateEmail / EMAIL_MAX_LENGTH 를 초과하는 이메일은 예외를 던진다 | 조건 validateEmail / EMAIL_MAX_LENGTH 를 초과하는 이메일은 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_FORMAT; 검증 assertThat(tooLong.length()).isEqualTo(CredentialPolicy.EMAIL_MAX_LENGTH + 1); .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_FORMAT); |
| [84](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L84) | validateEmail / 정상 정책의 비밀번호는 통과한다 | 파라미터 @ValueSource strings = { "passw0rd", // 영문+숫자, 8자 (최소 | 성공: validateEmail / 정상 정책의 비밀번호는 통과한다 |
| [100](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L100) | validatePassword / 길이가 8자 미만이면 PASSWORD_POLICY_VIOLATION 예외 | 파라미터 @ValueSource strings = { "passwd1", // 7자 (최소 미달 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION; 검증 .isEqualTo(AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION); |
| [113](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L113) | validatePassword / 64자 초과 비밀번호는 예외를 던진다 | 조건 validatePassword / 64자 초과 비밀번호는 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION; 검증 .isEqualTo(AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION); |
| [123](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L123) | validatePassword / 단일 카테고리 비밀번호는 거부한다 | 파라미터 @ValueSource strings = { "abcdefgh", // 영문만 "12345678", // 숫자만 "!@#$%^&*" // 특수문자만 } | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION; 검증 .isEqualTo(AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION); |
| [137](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L137) | validatePassword / 공백/제어문자가 포함된 비밀번호는 거부한다 | 파라미터 @ValueSource strings = { "passwd 12", // 공백 포함 "passwd\t12", // 탭 포함 "passwd\n12" // 개행 포함 } | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION; 검증 .isEqualTo(AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION); |
| [151](../../../src/test/java/com/umc/product/authentication/domain/CredentialPolicyTest.java#L151) | null 비밀번호는 예외를 던진다 | 조건 null 비밀번호는 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION; 검증 .isEqualTo(AuthenticationErrorCode.PASSWORD_POLICY_VIOLATION); |

### EmailVerificationTest
- 위치: `src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [35](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L35) | attemptCount 는 0 으로, isVerified 는 false 로 시작한다 | 조건 attemptCount 는 0 으로, isVerified 는 false 로 시작한다 | 성공: 검증 assertThat(session.getAttemptCount()).isZero(); assertThat(session.isVerified()).isFalse(); assertThat(session.getPurpose()).isEqualTo(EmailVerificationPurpose.REGISTER); assertThat(session.getEmail()).isEqualTo(EMAIL... |
| [50](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L50) | 새 세션 생성 / purpose 는 빌더에 전달된 값으로 고정된다 | 조건 새 세션 생성 / purpose 는 빌더에 전달된 값으로 고정된다 | 성공: 검증 assertThat(session.getPurpose()).isEqualTo(EmailVerificationPurpose.PASSWORD_RESET); |
| [63](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L63) | 새 세션 생성 / 올바른 코드와 미만료 상태이면 검증 성공한다 | 조건 새 세션 생성 / 올바른 코드와 미만료 상태이면 검증 성공한다 | 실패: 검증 assertThat(session.isVerified()).isTrue(); assertThat(session.getVerifiedAt()).isNotNull(); assertThat(session.getVerifiedBy()).isEqualTo("CODE"); assertThat(session.getAttemptCount()).isEqualTo(1); |
| [81](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L81) | verifyCode / 틀린 코드면 attemptCount 가 1 증가하고 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 verifyCode / 틀린 코드면 attemptCount 가 1 증가하고 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); assertThat(session.getAttemptCount()).isEqualTo(1); assertThat(session.isVerified()).isFalse(); |
| [96](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L96) | verifyCode / MAX_ATTEMPT_COUNT 회 실패 시 세션이 즉시 만료된다 | 조건 verifyCode / MAX_ATTEMPT_COUNT 회 실패 시 세션이 즉시 만료된다 | 실패: 예외 AuthenticationDomainException; 검증 assertThat(session.getAttemptCount()).isEqualTo(EmailVerification.MAX_ATTEMPT_COUNT); assertThat(session.isExpired()).isTrue(); |
| [113](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L113) | verifyCode / 이미 검증된 세션에 재호출하면 verifiedAt 을 덮어쓰지 않고 INVALID_EMAIL_VERIFICATION 으로 거부한다 | 조건 verifyCode / 이미 검증된 세션에 재호출하면 verifiedAt 을 덮어쓰지 않고 INVALID_EMAIL_VERIFICATION 으로 거부한다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); assertThat(session.getVerifiedAt()).isEqualTo(firstVerifiedAt); assertThat(session.getAttemptCount()).isEqualTo(countBefore); |
| [131](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L131) | 임계치 도달 후에는 정답을 입력해도 검증되지 않으며 attemptCount 가 더 이상 증가하지 않는다 | 조건 임계치 도달 후에는 정답을 입력해도 검증되지 않으며 attemptCount 가 더 이상 증가하지 않는다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); assertThat(session.getAttemptCount()).isEqualTo(countBefore); assertThat(session.isVerified()).isFalse(); |
| [154](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L154) | lastSentAt 이 null 이면 throttle 대상이 아니다 | 조건 lastSentAt 이 null 이면 throttle 대상이 아니다 | 성공: 검증 assertThat(session.isSendThrottled()).isFalse(); assertThat(session.getLastSentAt()).isNull(); |
| [167](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L167) | isSendThrottled / markSent / markSent 직후 60초 미만 경과 시 throttle 위반 | 조건 isSendThrottled / markSent / markSent 직후 60초 미만 경과 시 throttle 위반 | 실패: 검증 assertThat(session.isSendThrottled()).isTrue(); assertThat(session.getLastSentAt()).isNotNull(); |
| [179](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L179) | isSendThrottled / markSent / lastSentAt 이 60초 이전이면 throttle 해제 | 조건 isSendThrottled / markSent / lastSentAt 이 60초 이전이면 throttle 해제 | 성공: 검증 assertThat(session.isSendThrottled()).isFalse(); |
| [194](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L194) | isSendThrottled / markSent / 코드 재발급 시 attemptCount 가 0 으로 초기화된다 | 조건 isSendThrottled / markSent / 코드 재발급 시 attemptCount 가 0 으로 초기화된다 | 실패: 예외 AuthenticationDomainException; 검증 assertThat(session.getAttemptCount()).isEqualTo(1); assertThat(session.getAttemptCount()).isZero(); assertThat(session.getCode()).isEqualTo("999999"); assertThat(session.getToken()).isEqualTo("new-token"); |
| [214](../../../src/test/java/com/umc/product/authentication/domain/EmailVerificationTest.java#L214) | regenerate / 이미 검증된 세션은 ALREADY_VERIFIED_EMAIL 예외로 재발급 불가 | 조건 regenerate / 이미 검증된 세션은 ALREADY_VERIFIED_EMAIL 예외로 재발급 불가 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.ALREADY_VERIFIED_EMAIL; 검증 .isEqualTo(AuthenticationErrorCode.ALREADY_VERIFIED_EMAIL); |

### RefreshTokenTest
- 위치: `src/test/java/com/umc/product/authentication/domain/RefreshTokenTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [18](../../../src/test/java/com/umc/product/authentication/domain/RefreshTokenTest.java#L18) | 소유자와 만료시각이 유효하면 검증을 통과한다 | 조건 소유자와 만료시각이 유효하면 검증을 통과한다 | 실패: 소유자와 만료시각이 유효하면 검증을 통과한다 |
| [29](../../../src/test/java/com/umc/product/authentication/domain/RefreshTokenTest.java#L29) | 다른 회원의 RefreshToken이면 INVALID_REFRESH_TOKEN 예외를 던진다 | 조건 다른 회원의 RefreshToken이면 INVALID_REFRESH_TOKEN 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_REFRESH_TOKEN; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_REFRESH_TOKEN); |
| [42](../../../src/test/java/com/umc/product/authentication/domain/RefreshTokenTest.java#L42) | 만료된 RefreshToken이면 EXPIRED_JWT_TOKEN 예외를 던진다 | 조건 만료된 RefreshToken이면 EXPIRED_JWT_TOKEN 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.EXPIRED_JWT_TOKEN; 검증 .isEqualTo(AuthenticationErrorCode.EXPIRED_JWT_TOKEN); |
