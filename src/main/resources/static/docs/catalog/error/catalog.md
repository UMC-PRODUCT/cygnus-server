# 에러 코드 목록

서버가 응답할 수 있는 에러 코드를 도메인별로 확인할 수 있어요.

> 코드를 추가하거나 수정했다면 `./gradlew generateDocumentationCatalogs`를 실행해주세요.

## analytics

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | analytics | `ANALYTICS-0001` | `RESOURCE_ACCESS_DENIED` | 403 FORBIDDEN | 운영진 대시보드는 권한이 있는 운영진만 볼 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/analytics/domain/AnalyticsErrorCode.java:13` |
| 2 | analytics | `ANALYTICS-0002` | `INVALID_SORT` | 400 BAD_REQUEST | 지원하지 않는 정렬 조건이에요. 정렬 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/analytics/domain/AnalyticsErrorCode.java:15` |
| 3 | analytics | `ANALYTICS-0003` | `INVALID_PERIOD` | 400 BAD_REQUEST | 조회 시작 시각은 종료 시각보다 빨라야 해요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/analytics/domain/AnalyticsErrorCode.java:16` |

## authentication

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 4 | authentication | `AUTHENTICATION-0001` | `OAUTH_PROVIDER_NOT_FOUND` | 400 BAD_REQUEST | 지원하지 않는 로그인 방식이에요. 다른 방식을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:23` |
| 5 | authentication | `AUTHENTICATION-0002` | `NO_MATCHING_MEMBER` | 404 NOT_FOUND | 가입된 계정을 찾을 수 없어요. 회원가입을 먼저 진행해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:24` |
| 6 | authentication | `AUTHENTICATION-0003` | `NO_EMAIL_VERIFICATION_METHOD_GIVEN` | 400 BAD_REQUEST | 이메일 인증 요청이 올바르지 않아요. 인증을 다시 요청해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:26` |
| 7 | authentication | `AUTHENTICATION-0004` | `INVALID_EMAIL_VERIFICATION` | 401 UNAUTHORIZED | 이메일 인증 정보가 맞지 않아요. 인증 메일을 다시 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:28` |
| 8 | authentication | `AUTHENTICATION-0006` | `OAUTH_SUCCESS_BUT_NO_MEMBER` | 404 NOT_FOUND | 가입된 계정을 찾을 수 없어요. 회원가입을 먼저 진행해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:32` |
| 9 | authentication | `AUTHENTICATION-0007` | `OAUTH_SUCCESS_BUT_NO_INFO` | 503 SERVICE_UNAVAILABLE | 로그인에 필요한 정보를 받아오지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:34` |
| 10 | authentication | `AUTHENTICATION-0008` | `OAUTH_FAILURE` | 400 BAD_REQUEST | OAuth 로그인에 실패했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:36` |
| 11 | authentication | `AUTHENTICATION-0009` | `OAUTH_INVALID_ACCESS_TOKEN` | 400 BAD_REQUEST | OAuth 인증 정보가 올바르지 않아요. 다시 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:37` |
| 12 | authentication | `AUTHENTICATION-0010` | `OAUTH_TOKEN_VERIFICATION_FAILED` | 401 UNAUTHORIZED | OAuth 인증 정보를 확인하지 못했어요. 다시 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:38` |
| 13 | authentication | `AUTHENTICATION-0011` | `INVALID_OAUTH_TOKEN` | 401 UNAUTHORIZED | OAuth 인증 정보가 올바르지 않아요. 다시 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:40` |
| 14 | authentication | `AUTHENTICATION-0012` | `OAUTH_ALREADY_LINKED` | 401 UNAUTHORIZED | 이미 다른 계정에 연결된 OAuth 계정이에요. 연결된 계정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:41` |
| 15 | authentication | `AUTHENTICATION-0013` | `OAUTH_PROVIDER_ALREADY_LINKED` | 401 UNAUTHORIZED | 이미 연결된 OAuth 제공자예요. 기존 연결을 해제한 뒤 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:42` |
| 16 | authentication | `AUTHENTICATION-0014` | `MEMBER_OAUTH_NOT_FOUND` | 404 NOT_FOUND | 연결된 OAuth 정보를 찾을 수 없어요. 다시 연결해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:44` |
| 17 | authentication | `AUTHENTICATION-0015` | `NOT_VALID_MEMBER` | 403 FORBIDDEN | 이 작업을 할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:45` |
| 18 | authentication | `AUTHENTICATION-0016` | `OAUTH_CANNOT_UNLINK_LAST_PROVIDER` | 400 BAD_REQUEST | 비밀번호를 등록하지 않은 계정은 연결된 유일한 OAuth를 해제할 수 없어요. 비밀번호를 먼저 등록하거나 회원 탈퇴를 이용해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:47` |
| 19 | authentication | `AUTHENTICATION-0017` | `ALREADY_VERIFIED_EMAIL` | 400 BAD_REQUEST | 이미 인증이 끝난 이메일 인증 세션이에요. 다음 단계로 진행해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:51` |
| 20 | authentication | `AUTHENTICATION-0018` | `EMAIL_VERIFICATION_SESSION_EXPIRED` | 400 BAD_REQUEST | 이메일 인증 세션이 만료됐어요. 새로운 인증을 요청해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:53` |
| 21 | authentication | `AUTHENTICATION-0019` | `LOGIN_ID_ALREADY_EXISTS` | 409 CONFLICT | 이미 사용 중인 로그인 ID예요. 다른 ID를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:59` |
| 22 | authentication | `AUTHENTICATION-0020` | `INVALID_LOGIN_ID_FORMAT` | 400 BAD_REQUEST | 로그인 ID는 영문, 숫자, ., _, -를 사용해 5~20자로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:60` |
| 23 | authentication | `AUTHENTICATION-0021` | `PASSWORD_POLICY_VIOLATION` | 400 BAD_REQUEST | 비밀번호는 8~64자로 입력하고 영문, 숫자, 특수문자 중 2종류 이상을 포함해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:62` |
| 24 | authentication | `AUTHENTICATION-0022` | `INVALID_LOGIN_CREDENTIAL` | 401 UNAUTHORIZED | 로그인 ID 또는 비밀번호가 올바르지 않아요. 다시 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:65` |
| 25 | authentication | `AUTHENTICATION-0023` | `UNSUPPORTED_OAUTH_FLOW` | 400 BAD_REQUEST | 선택한 OAuth 제공자는 이 인증 방식을 지원하지 않아요. 다른 로그인 방식을 사용해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:84` |
| 26 | authentication | `AUTHENTICATION-0024` | `INVALID_OAUTH_REDIRECT_URI` | 400 BAD_REQUEST | 허용되지 않은 OAuth redirect URI예요. 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:86` |
| 27 | authentication | `AUTHENTICATION-0025` | `INVALID_EMAIL_FORMAT` | 400 BAD_REQUEST | 이메일 형식이 올바르지 않아요. 이메일 주소를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:67` |
| 28 | authentication | `AUTHENTICATION-0026` | `EMAIL_ALREADY_EXISTS` | 409 CONFLICT | 이미 사용 중인 이메일이에요. 다른 이메일을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:69` |
| 29 | authentication | `AUTHENTICATION-0027` | `EMAIL_VERIFICATION_THROTTLED` | 429 TOO_MANY_REQUESTS | 이메일 인증 요청이 너무 잦아요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:55` |
| 30 | authentication | `AUTHENTICATION-0028` | `INVALID_SSO_CLIENT` | 400 BAD_REQUEST | 지원하지 않는 SSO client예요. 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:71` |
| 31 | authentication | `AUTHENTICATION-0029` | `INVALID_SSO_REDIRECT_URI` | 400 BAD_REQUEST | 허용되지 않은 SSO redirect URI예요. 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:73` |
| 32 | authentication | `AUTHENTICATION-0030` | `INVALID_SSO_AUTHORIZATION_REQUEST` | 400 BAD_REQUEST | SSO 인증 요청이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:75` |
| 33 | authentication | `AUTHENTICATION-0031` | `INVALID_SSO_PKCE` | 400 BAD_REQUEST | SSO PKCE 검증에 실패했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:76` |
| 34 | authentication | `AUTHENTICATION-0032` | `SSO_BROWSER_LOGIN_REQUIRED` | 401 UNAUTHORIZED | Auth App 로그인이 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:77` |
| 35 | authentication | `AUTHENTICATION-0033` | `INVALID_SSO_BROWSER_LOGIN` | 401 UNAUTHORIZED | Auth App 로그인 정보가 유효하지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:78` |
| 36 | authentication | `AUTHENTICATION-0034` | `INVALID_SSO_AUTHORIZATION_CODE` | 401 UNAUTHORIZED | SSO authorization code가 유효하지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:79` |
| 37 | authentication | `AUTHENTICATION-0035` | `EXPIRED_SSO_AUTHORIZATION_CODE` | 401 UNAUTHORIZED | SSO authorization code가 만료됐어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:80` |
| 38 | authentication | `AUTHENTICATION-0036` | `UNSUPPORTED_SSO_GRANT_TYPE` | 400 BAD_REQUEST | 지원하지 않는 SSO grant_type이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:81` |
| 39 | authentication | `JWT-0001` | `WRONG_JWT_SIGNATURE` | 401 UNAUTHORIZED | 인증 정보가 올바르지 않아요. 다시 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:16` |
| 40 | authentication | `JWT-0002` | `EXPIRED_JWT_TOKEN` | 401 UNAUTHORIZED | 로그인이 만료됐어요. 다시 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:17` |
| 41 | authentication | `JWT-0003` | `UNSUPPORTED_JWT` | 401 UNAUTHORIZED | 지원하지 않는 인증 정보예요. 다시 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:18` |
| 42 | authentication | `JWT-0004` | `INVALID_JWT` | 401 UNAUTHORIZED | 인증 정보가 올바르지 않아요. 다시 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:19` |
| 43 | authentication | `JWT-0005` | `INVALID_REFRESH_TOKEN` | 401 UNAUTHORIZED | 유효하지 않거나 폐기된 Refresh Token 입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/authentication/domain/exception/AuthenticationErrorCode.java:20` |

## authorization

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 44 | authorization | `AUTHORIZATION-0001` | `PERMISSION_DENIED` | 403 FORBIDDEN | 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:14` |
| 45 | authorization | `AUTHORIZATION-0002` | `RESOURCE_ACCESS_DENIED` | 403 FORBIDDEN | 이 항목에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:16` |
| 46 | authorization | `AUTHORIZATION-0003` | `INVALID_PERMISSION` | 400 BAD_REQUEST | 권한 값이 올바르지 않아요. 요청 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:18` |
| 47 | authorization | `AUTHORIZATION-0004` | `POLICY_EVALUATION_FAILED` | 500 INTERNAL_SERVER_ERROR | 권한을 확인하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:19` |
| 48 | authorization | `AUTHORIZATION-0005` | `NO_EVALUATOR_MATCHING_RESOURCE_TYPE` | 500 INTERNAL_SERVER_ERROR | 권한 확인 설정을 찾지 못했어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:21` |
| 49 | authorization | `AUTHORIZATION-0006` | `PERMISSION_TYPE_NOT_SUPPORTED_BY_RESOURCE_TYPE` | 500 INTERNAL_SERVER_ERROR | 지원하지 않는 권한 유형이에요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:23` |
| 50 | authorization | `AUTHORIZATION-0007` | `INVALID_INPUT_VALUE` | 400 BAD_REQUEST | 권한 확인 요청이 올바르지 않아요. 요청 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:25` |
| 51 | authorization | `AUTHORIZATION-0008` | `INVALID_RESOURCE_ID_TYPE` | 400 BAD_REQUEST | 권한을 확인할 항목 ID가 올바르지 않아요. 요청 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:26` |
| 52 | authorization | `AUTHORIZATION-0009` | `INVALID_RESOURCE_PERMISSION_GIVEN` | 500 INTERNAL_SERVER_ERROR | 권한 확인 요청을 처리하지 못했어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:28` |
| 53 | authorization | `AUTHORIZATION-0010` | `CHALLENGER_ROLE_NOT_FOUND` | 404 NOT_FOUND | 역할을 찾을 수 없어요. 역할 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:30` |
| 54 | authorization | `AUTHORIZATION-0011` | `PERMISSION_TYPE_NOT_IMPLEMENTED` | 501 NOT_IMPLEMENTED | 아직 지원하지 않는 권한 확인이에요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/authorization/domain/exception/AuthorizationErrorCode.java:31` |

## certificate

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 55 | certificate | `CERTIFICATE-0001` | `CERTIFICATE_NOT_FOUND` | 404 NOT_FOUND | 인증서를 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:13` |
| 56 | certificate | `CERTIFICATE-0002` | `CERTIFICATE_ACCESS_FORBIDDEN` | 403 FORBIDDEN | 인증서에 접근할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:14` |
| 57 | certificate | `CERTIFICATE-0003` | `CERTIFICATE_ISSUE_FORBIDDEN` | 403 FORBIDDEN | 인증서를 발급할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:15` |
| 58 | certificate | `CERTIFICATE-0004` | `CERTIFICATE_SELF_ISSUE_FORBIDDEN` | 400 BAD_REQUEST | 직접 발급할 수 없는 인증서 종류예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:16` |
| 59 | certificate | `CERTIFICATE-0005` | `CERTIFICATE_ELIGIBILITY_NOT_MET` | 400 BAD_REQUEST | 인증서 발급 조건을 만족하지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:17` |
| 60 | certificate | `CERTIFICATE-0006` | `CERTIFICATE_ALREADY_REVOKED` | 400 BAD_REQUEST | 이미 폐기된 인증서예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:18` |
| 61 | certificate | `CERTIFICATE-0007` | `CERTIFICATE_EXPIRED_OR_REVOKED` | 400 BAD_REQUEST | 만료되었거나 폐기된 인증서예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:19` |
| 62 | certificate | `CERTIFICATE-0008` | `CERTIFICATE_SERIAL_GENERATION_FAILED` | 500 INTERNAL_SERVER_ERROR | 인증서 일련번호를 만들지 못했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:20` |
| 63 | certificate | `CERTIFICATE-0009` | `CERTIFICATE_RENDER_FAILED` | 500 INTERNAL_SERVER_ERROR | 인증서 PDF를 만들지 못했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:21` |

## challenger

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 64 | challenger | `CHALLENGER-0001` | `CHALLENGER_NOT_FOUND` | 404 NOT_FOUND | 챌린저를 찾을 수 없어요. 선택한 챌린저를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:14` |
| 65 | challenger | `CHALLENGER-0002` | `CHALLENGER_ALREADY_EXISTS` | 409 CONFLICT | 이미 등록된 챌린저예요. 기존 기록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:15` |
| 66 | challenger | `CHALLENGER-0003` | `CHALLENGER_ALREADY_WITHDRAWN` | 400 BAD_REQUEST | 이미 탈퇴한 챌린저예요. 다른 챌린저를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:16` |
| 67 | challenger | `CHALLENGER-0004` | `INVALID_CHALLENGER_STATUS` | 400 BAD_REQUEST | 챌린저 상태가 올바르지 않아요. 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:17` |
| 68 | challenger | `CHALLENGER-0005` | `CHALLENGER_NOT_ACTIVE` | 400 BAD_REQUEST | 활동 중인 챌린저만 사용할 수 있어요. 챌린저 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:18` |
| 69 | challenger | `CHALLENGER-0007` | `CHALLENGER_POINT_NOT_FOUND` | 404 NOT_FOUND | 상벌점 기록을 찾을 수 없어요. 선택한 기록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:19` |
| 70 | challenger | `CHALLENGER-0008` | `BAD_CHALLENGER_UPDATE_REQUEST` | 404 NOT_FOUND | 챌린저 수정 요청이 올바르지 않아요. 입력값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:20` |
| 71 | challenger | `CHALLENGER-0009` | `NOT_ALLOWED_AUTHOR` | 400 BAD_REQUEST | 일정을 만들려면 챌린저 상태가 활동 중이거나 수료여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:21` |
| 72 | challenger | `CHALLENGER-0010` | `MEMBER_PROFILE_NOT_FOUND` | 404 NOT_FOUND | 연결된 멤버 프로필을 찾을 수 없어요. 회원 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:22` |
| 73 | challenger | `CHALLENGER-0011` | `INVALID_CURSOR_ID` | 400 BAD_REQUEST | 커서 값이 올바르지 않아요. 목록을 처음부터 다시 조회해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:23` |
| 74 | challenger | `CHALLENGER-0012` | `USED_CHALLENGER_RECORD_CODE` | 400 BAD_REQUEST | 이미 사용한 챌린저 기록 추가 코드예요. 새 코드를 발급받아주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:24` |
| 75 | challenger | `CHALLENGER-0013` | `INVALID_MEMBER_NAME_FOR_RECORD` | 400 BAD_REQUEST | 코드에 등록된 이름이 내 정보와 일치하지 않아요. 입력한 코드를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:25` |
| 76 | challenger | `CHALLENGER-0014` | `INVALID_SCHOOL_FOR_RECORD` | 400 BAD_REQUEST | 코드에 등록된 학교가 내 소속과 일치하지 않아요. 소속 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:26` |
| 77 | challenger | `CHALLENGER-0015` | `INVALID_CHALLENGER_RECORD_CREATE_REQUEST` | 400 BAD_REQUEST | 입력한 정보로 챌린저 기록을 만들 수 없어요. 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:27` |
| 78 | challenger | `CHALLENGER-0016` | `NO_CHALLENGER_IN_MEMBER_GISU` | 404 NOT_FOUND | 해당 기수의 챌린저 기록을 찾을 수 없어요. 기수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:28` |
| 79 | challenger | `CHALLENGER-0017` | `CHALLENGER_PART_NOT_FOUND` | 404 NOT_FOUND | 챌린저 파트를 찾을 수 없어요. 파트 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:29` |

## chat

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 80 | chat | `CHAT-0001` | `CHAT_ROOM_NOT_FOUND` | 404 NOT_FOUND | 채팅방을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:14` |
| 81 | chat | `CHAT-0002` | `CHAT_MEMBER_ALREADY_EXISTS` | 409 CONFLICT | 이미 채팅방에 참여 중인 멤버입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:15` |
| 82 | chat | `CHAT-0003` | `CHAT_MEMBER_NOT_FOUND` | 404 NOT_FOUND | 채팅방 멤버를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:16` |
| 83 | chat | `CHAT-0004` | `CHAT_MESSAGE_NOT_FOUND` | 404 NOT_FOUND | 채팅 메시지를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:17` |
| 84 | chat | `CHAT-0005` | `CHAT_MESSAGE_INVALID_CONTENT_TYPE` | 400 BAD_REQUEST | 허용되지 않는 메시지 콘텐츠 타입입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:18` |
| 85 | chat | `CHAT-0006` | `CHAT_MESSAGE_EMPTY` | 400 BAD_REQUEST | 메시지 내용 또는 첨부가 필요합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:19` |
| 86 | chat | `CHAT-0007` | `CHAT_ROOM_ACCESS_DENIED` | 403 FORBIDDEN | 해당 채팅방에 접근할 권한이 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:20` |
| 87 | chat | `CHAT-0008` | `CHAT_MESSAGE_INVALID_PAGE_SIZE` | 400 BAD_REQUEST | 허용되지 않는 페이지 크기입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:21` |
| 88 | chat | `CHAT-0009` | `CHAT_MESSAGE_INVALID_REPLY_TARGET` | 400 BAD_REQUEST | 답장할 메시지를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:22` |
| 89 | chat | `CHAT-0010` | `CHAT_MESSAGE_ATTACHMENT_REQUIRED` | 400 BAD_REQUEST | 이미지 또는 파일 메시지에는 첨부파일이 필요합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:23` |
| 90 | chat | `CHAT-0011` | `CHAT_MESSAGE_ATTACHMENT_NOT_ALLOWED` | 400 BAD_REQUEST | 텍스트 메시지에는 파일을 첨부할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:24` |
| 91 | chat | `CHAT-0012` | `CHAT_MESSAGE_INVALID_FILE_TYPE` | 400 BAD_REQUEST | 메시지 타입에 허용되지 않는 파일 형식입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:25` |
| 92 | chat | `CHAT-0013` | `CHAT_MESSAGE_INVALID_ATTACHMENT` | 400 BAD_REQUEST | 첨부파일 정보가 올바르지 않습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:26` |

## community

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 93 | community | `COMMUNITY-0001` | `POST_NOT_FOUND` | 404 NOT_FOUND | 게시글을 찾을 수 없어요. 목록을 새로고침해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:14` |
| 94 | community | `COMMUNITY-0002` | `COMMENT_NOT_FOUND` | 404 NOT_FOUND | 댓글을 찾을 수 없어요. 목록을 새로고침해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:15` |
| 95 | community | `COMMUNITY-0004` | `INVALID_POST_TITLE` | 400 BAD_REQUEST | 게시글 제목이 올바르지 않아요. 제목을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:17` |
| 96 | community | `COMMUNITY-0005` | `INVALID_POST_CONTENT` | 400 BAD_REQUEST | 게시글 내용이 올바르지 않아요. 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:18` |
| 97 | community | `COMMUNITY-0006` | `INVALID_POST_CATEGORY` | 400 BAD_REQUEST | 게시글 카테고리가 올바르지 않아요. 카테고리를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:19` |
| 98 | community | `COMMUNITY-0007` | `INVALID_POST_REGION` | 400 BAD_REQUEST | 게시글 지역이 올바르지 않아요. 지역을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:20` |
| 99 | community | `COMMUNITY-0008` | `CANNOT_CHANGE_TO_LIGHTNING` | 400 BAD_REQUEST | 번개글은 번개글 작성 화면에서 만들어주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:21` |
| 100 | community | `COMMUNITY-0009` | `CANNOT_CHANGE_FROM_LIGHTNING` | 400 BAD_REQUEST | 번개글은 일반 게시글로 바꿀 수 없어요. 새 게시글로 작성해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:22` |
| 101 | community | `COMMUNITY-0010` | `INVALID_COMMENT_CONTENT` | 400 BAD_REQUEST | 댓글 내용이 올바르지 않아요. 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:24` |
| 102 | community | `COMMUNITY-0011` | `COMMENT_NOT_OWNED` | 403 FORBIDDEN | 내가 작성한 댓글만 삭제할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:25` |
| 103 | community | `COMMUNITY-0016` | `REPORT_ALREADY_EXISTS` | 409 CONFLICT | 이미 신고한 게시글 또는 댓글이에요. 신고 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:27` |
| 104 | community | `COMMUNITY-0017` | `INVALID_POST_AUTHOR` | 400 BAD_REQUEST | 작성자 정보가 필요해요. 로그인 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:30` |
| 105 | community | `COMMUNITY-0018` | `NOT_LIGHTNING_POST` | 400 BAD_REQUEST | 번개글이 아니에요. 일반 게시글 화면에서 수정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:31` |
| 106 | community | `COMMUNITY-0019` | `USE_LIGHTNING_API` | 400 BAD_REQUEST | 번개글은 번개글 화면에서 작성해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:32` |
| 107 | community | `COMMUNITY-0020` | `LIGHTNING_INFO_REQUIRED` | 400 BAD_REQUEST | 번개글을 작성하려면 모임 정보를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:33` |
| 108 | community | `COMMUNITY-0021` | `POST_NOT_OWNED` | 403 FORBIDDEN | 내가 작성한 게시글만 수정하거나 삭제할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:34` |
| 109 | community | `COMMUNITY-0022` | `INVALID_LIGHTNING_MEET_AT` | 400 BAD_REQUEST | 모임 시간을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:37` |
| 110 | community | `COMMUNITY-0023` | `INVALID_LIGHTNING_LOCATION` | 400 BAD_REQUEST | 모임 장소를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:38` |
| 111 | community | `COMMUNITY-0024` | `INVALID_LIGHTNING_MAX_PARTICIPANTS` | 400 BAD_REQUEST | 최대 참가자는 1명 이상으로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:39` |
| 112 | community | `COMMUNITY-0025` | `INVALID_LIGHTNING_OPEN_CHAT_URL` | 400 BAD_REQUEST | 오픈 채팅 링크를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:40` |
| 113 | community | `COMMUNITY-0026` | `INVALID_LIGHTNING_OPEN_CHAT_URL_FORMAT` | 400 BAD_REQUEST | 오픈 채팅 링크는 http:// 또는 https://로 시작해야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:41` |
| 114 | community | `COMMUNITY-0027` | `INVALID_LIGHTNING_MEET_AT_PAST` | 400 BAD_REQUEST | 모임 시간은 현재 이후로 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:42` |
| 115 | community | `COMMUNITY-0028` | `INVALID_COMMENT_POST_ID` | 400 BAD_REQUEST | 댓글을 작성할 게시글을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:45` |
| 116 | community | `COMMUNITY-0029` | `INVALID_COMMENT_CHALLENGER_ID` | 400 BAD_REQUEST | 댓글 작성자 챌린저 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:46` |
| 117 | community | `COMMUNITY-0030` | `INVALID_ID` | 400 BAD_REQUEST | ID는 1 이상의 숫자로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:49` |
| 118 | community | `COMMUNITY-0031` | `POST_SAVE_REQUIRES_AUTHOR` | 400 BAD_REQUEST | 새 게시글을 만들려면 작성자 정보가 필요해요. 로그인 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:52` |
| 119 | community | `COMMUNITY-0032` | `POST_UPDATE_INVALID_CALL` | 400 BAD_REQUEST | 게시글 수정 요청이 올바르지 않아요. 요청 방식을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:53` |

## curriculum

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 120 | curriculum | `CURRICULUM-0001` | `CURRICULUM_NOT_FOUND` | 404 NOT_FOUND | 커리큘럼을 찾을 수 없어요. 선택한 커리큘럼을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:14` |
| 121 | curriculum | `CURRICULUM-0002` | `WORKBOOK_NOT_FOUND` | 404 NOT_FOUND | 워크북을 찾을 수 없어요. 선택한 워크북을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:15` |
| 122 | curriculum | `CURRICULUM-0003` | `MISSION_NOT_FOUND` | 404 NOT_FOUND | 미션을 찾을 수 없어요. 선택한 미션을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:16` |
| 123 | curriculum | `CURRICULUM-0004` | `WORKBOOK_HAS_SUBMISSIONS` | 409 CONFLICT | 제출된 워크북이 있어 삭제할 수 없어요. 제출 내역을 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:17` |
| 124 | curriculum | `CURRICULUM-0005` | `WORKBOOK_NOT_IN_CURRICULUM` | 404 NOT_FOUND | 이 커리큘럼에 포함된 워크북이 아니에요. 워크북을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:18` |
| 125 | curriculum | `CURRICULUM-0006` | `CHALLENGER_WORKBOOK_NOT_FOUND` | 404 NOT_FOUND | 챌린저 워크북을 찾을 수 없어요. 선택한 워크북을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:19` |
| 126 | curriculum | `CURRICULUM-0007` | `SUBMISSION_REQUIRED` | 400 BAD_REQUEST | 제출 내용을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:20` |
| 127 | curriculum | `CURRICULUM-0008` | `INVALID_WORKBOOK_STATUS` | 400 BAD_REQUEST | 워크북 상태가 올바르지 않아요. 상태 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:21` |
| 128 | curriculum | `CURRICULUM-0009` | `WORKBOOK_SUBMISSION_ALREADY_EXISTS` | 409 CONFLICT | 이미 해당 주차의 워크북 미션을 제출했어요. 제출 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:22` |
| 129 | curriculum | `CURRICULUM-0010` | `CURRICULUM_ALREADY_EXISTS` | 409 CONFLICT | 해당 기수와 파트의 커리큘럼이 이미 있어요. 기존 커리큘럼을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:23` |
| 130 | curriculum | `CURRICULUM-0011` | `WORKBOOK_ACCESS_DENIED` | 403 FORBIDDEN | 이 워크북에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:24` |
| 131 | curriculum | `CURRICULUM-0012` | `INVALID_WEEKLY_CURRICULUM_PERIOD` | 400 BAD_REQUEST | 주차 커리큘럼 시작일은 종료일보다 빨라야 해요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:26` |
| 132 | curriculum | `CURRICULUM-0013` | `INVALID_WORKBOOK_STATUS_TRANSITION` | 400 BAD_REQUEST | 현재 상태에서는 워크북 상태를 변경할 수 없어요. 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:27` |
| 133 | curriculum | `CURRICULUM-0014` | `WEEKLY_CURRICULUM_NOT_FOUND` | 404 NOT_FOUND | 주차별 커리큘럼을 찾을 수 없어요. 선택한 주차를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:28` |
| 134 | curriculum | `CURRICULUM-0015` | `CURRICULUM_HAS_WEEKLY_CURRICULUMS` | 409 CONFLICT | 주차별 커리큘럼이 남아 있어 삭제할 수 없어요. 주차별 커리큘럼을 먼저 정리해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:29` |
| 135 | curriculum | `CURRICULUM-0016` | `WEEKLY_CURRICULUM_HAS_WORKBOOKS` | 409 CONFLICT | 원본 워크북이 남아 있어 삭제할 수 없어요. 원본 워크북을 먼저 정리해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:30` |
| 136 | curriculum | `CURRICULUM-0017` | `WEEKLY_CURRICULUM_DATE_LOCKED` | 409 CONFLICT | 배포된 워크북이 있어 주차 기간을 수정할 수 없어요. 배포 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:31` |
| 137 | curriculum | `CURRICULUM-0018` | `WEEKLY_CURRICULUM_ALREADY_EXISTS` | 409 CONFLICT | 동일한 주차와 부록 여부의 주차별 커리큘럼이 이미 있어요. 기존 항목을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:32` |
| 138 | curriculum | `CURRICULUM-0019` | `WEEKLY_CURRICULUM_PERIOD_ALREADY_ENDED` | 400 BAD_REQUEST | 종료된 기간으로는 주차별 커리큘럼을 만들거나 수정할 수 없어요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:33` |
| 139 | curriculum | `CURRICULUM-0020` | `MISSION_HAS_SUBMISSIONS` | 409 CONFLICT | 이미 제출된 미션이 있어 삭제할 수 없어요. 제출 내역을 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:34` |
| 140 | curriculum | `CURRICULUM-0021` | `RELEASED_WORKBOOK_NECESSARY_MISSION_FORBIDDEN` | 400 BAD_REQUEST | 배포된 워크북에는 필수 미션을 추가할 수 없어요. 선택 미션으로 추가해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:35` |
| 141 | curriculum | `CURRICULUM-0022` | `RELEASED_WORKBOOK_MISSION_UPGRADE_FORBIDDEN` | 400 BAD_REQUEST | 배포된 워크북의 미션은 필수에서 선택으로만 변경할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:36` |

## documentation

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 142 | documentation | `DOCS-0001` | `ERROR_CODE_CATALOG_UNAVAILABLE` | 500 INTERNAL_SERVER_ERROR | 에러 코드 목록을 불러오지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/documentation/domain/DocumentationErrorCode.java:14` |

## feedback

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 143 | feedback | `FEEDBACK-0001` | `USER_FEEDBACK_TEMPLATE_NOT_FOUND` | 404 NOT_FOUND | 피드백 양식을 찾을 수 없어요. 양식을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/feedback/domain/exception/FeedbackErrorCode.java:15` |

## form

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 144 | form | `FORM-0001` | `FORM_NOT_FOUND` | 404 NOT_FOUND | 폼을 찾을 수 없어요. 선택한 폼을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:14` |
| 145 | form | `FORM-0002` | `FORM_NOT_DRAFT` | 409 CONFLICT | 임시저장 상태의 폼만 편집할 수 있어요. 폼 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:15` |
| 146 | form | `FORM-0003` | `QUESTION_NOT_FOUND` | 404 NOT_FOUND | 질문을 찾을 수 없어요. 선택한 질문을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:16` |
| 147 | form | `FORM-0005` | `FORM_ALREADY_PUBLISHED` | 400 BAD_REQUEST | 이미 발행된 폼이에요. 폼 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:17` |
| 148 | form | `FORM-0006` | `FORM_RESPONSE_NOT_FOUND` | 404 NOT_FOUND | 폼 응답을 찾을 수 없어요. 응답 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:18` |
| 149 | form | `FORM-0007` | `QUESTION_IS_NOT_OWNED_BY_FORM` | 400 BAD_REQUEST | 이 폼에 포함된 질문이 아니에요. 질문을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:19` |
| 150 | form | `FORM-0008` | `FORM_RESPONSE_FORBIDDEN` | 403 FORBIDDEN | 이 폼 응답에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:20` |
| 151 | form | `FORM-0009` | `QUESTION_TYPE_MISMATCH` | 400 BAD_REQUEST | 질문 유형이 맞지 않아요. 질문 유형을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:22` |
| 152 | form | `FORM-0010` | `REQUIRED_QUESTION_NOT_ANSWERED` | 400 BAD_REQUEST | 필수 질문에 답변해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:23` |
| 153 | form | `FORM-0011` | `INVALID_ANSWER_FORMAT` | 400 BAD_REQUEST | 응답 형식이 올바르지 않아요. 답변을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:24` |
| 154 | form | `FORM-0012` | `OTHER_OPTION_DUPLICATED` | 400 BAD_REQUEST | '기타' 선택지가 중복됐어요. 선택지를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:25` |
| 155 | form | `FORM-0013` | `OPTION_NOT_IN_QUESTION` | 400 BAD_REQUEST | 해당 질문에 없는 선택지예요. 선택지를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:26` |
| 156 | form | `FORM-0014` | `OPTION_TEXT_REQUIRED` | 400 BAD_REQUEST | '기타' 선택지의 내용을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:27` |
| 157 | form | `FORM-0015` | `INVALID_FORM_ACTIVE_PERIOD` | 400 BAD_REQUEST | 폼 응답 가능 기간이 올바르지 않아요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:28` |
| 158 | form | `FORM-0023` | `INVALID_VOTE_SELECTION` | 400 BAD_REQUEST | 투표 선택이 올바르지 않아요. 선택지를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:30` |
| 159 | form | `FORM-0025` | `INVALID_VOTE_FORM_STRUCTURE` | 400 BAD_REQUEST | 투표 질문 형식이 올바르지 않아요. 투표 구성을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:32` |
| 160 | form | `FORM-0027` | `FORM_RESPONSE_ALREADY_EXISTS` | 400 BAD_REQUEST | 이미 제출한 응답이 있어요. 제출 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:34` |
| 161 | form | `FORM-0028` | `FORM_NOT_PUBLISHED` | 409 CONFLICT | 발행된 폼에만 응답할 수 있어요. 폼 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:35` |
| 162 | form | `FORM-0029` | `QUESTION_OPTION_NOT_FOUND` | 404 NOT_FOUND | 선택지를 찾을 수 없어요. 선택지를 다시 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:36` |
| 163 | form | `FORM-0030` | `ANSWER_NOT_FOUND` | 404 NOT_FOUND | 답변을 찾을 수 없어요. 응답 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:37` |
| 164 | form | `FORM-0031` | `FORM_RESPONSE_NOT_DRAFT` | 409 CONFLICT | 임시저장 상태의 응답에서만 할 수 있는 작업이에요. 응답 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:38` |
| 165 | form | `FORM-0032` | `ANSWER_ALREADY_EXISTS` | 400 BAD_REQUEST | 이미 해당 질문에 대한 답변이 있어요. 기존 답변을 수정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:39` |
| 166 | form | `FORM-0033` | `FORM_RESPONSE_LOOKUP_AMBIGUOUS` | 409 CONFLICT | 중복 응답을 허용하는 폼은 응답을 하나로 특정할 수 없어요. 응답 ID를 사용해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:40` |
| 167 | form | `FORM-0034` | `RESPONDENT_MEMBER_ID_REQUIRED` | 400 BAD_REQUEST | 응답자 정보가 필요해요. 이 문제가 계속되면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:42` |
| 168 | form | `FORM-0035` | `RESPONSE_ACCESS_KEY_REQUIRED` | 400 BAD_REQUEST | 응답 접근 키가 필요해요. 이 문제가 계속되면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:44` |
| 169 | form | `FORM-0036` | `INVALID_SUBMIT_SCOPE` | 400 BAD_REQUEST | 제출 범위가 올바르지 않아요. 이 문제가 계속되면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:46` |
| 170 | form | `FORM-0037` | `INVALID_NEXT_SECTION_SELF_LOOP` | 400 BAD_REQUEST | 조건부 섹션 이동은 자기 자신을 대상으로 할 수 없어요. 이동 대상 섹션을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:48` |
| 171 | form | `FORM-0038` | `FORM_INVALID_TRANSITION` | 409 CONFLICT | 현재 폼 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:50` |
| 172 | form | `FORM-0039` | `FORM_HAS_RESPONSES` | 409 CONFLICT | 응답이 있는 폼은 초안 상태로 되돌릴 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:51` |

## global

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 173 | global | `COMMON-0001` | `INTERNAL_SERVER_ERROR` | 500 INTERNAL_SERVER_ERROR | 요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:26` |
| 174 | global | `COMMON-400` | `BAD_REQUEST` | 400 BAD_REQUEST | 요청 값이 올바르지 않아요. 입력한 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:28` |
| 175 | global | `COMMON-401` | `UNAUTHORIZED` | 401 UNAUTHORIZED | 로그인이 필요해요. 로그인 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:29` |
| 176 | global | `COMMON-403` | `FORBIDDEN` | 403 FORBIDDEN | 요청할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:30` |
| 177 | global | `COMMON-404` | `NOT_FOUND` | 404 NOT_FOUND | 요청한 항목을 찾을 수 없어요. 입력한 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:31` |
| 178 | global | `COMMON-429` | `TOO_MANY_REQUESTS` | 429 TOO_MANY_REQUESTS | 요청이 너무 많습니다. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:32` |
| 179 | global | `COMMON-501` | `NOT_IMPLEMENTED` | 501 NOT_IMPLEMENTED | 아직 사용할 수 없는 기능이에요. 필요한 기능이라면 서버팀에 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:33` |
| 180 | global | `ENV-0001` | `INVALID_ENV` | 400 BAD_REQUEST | 현재 실행 환경에서는 사용할 수 없는 기능이에요. 환경 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:42` |
| 181 | global | `PE-0001` | `PERMISSION_TYPE_NOT_IMPLEMENTED` | 501 NOT_IMPLEMENTED | 아직 지원하지 않는 권한 확인이에요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:45` |
| 182 | global | `SECURITY-0001` | `SECURITY_NOT_GIVEN` | 401 UNAUTHORIZED | 인증 정보가 없어요. 로그인 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:36` |
| 183 | global | `SECURITY-0002` | `SECURITY_FORBIDDEN` | 403 FORBIDDEN | 권한이 부족해요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:37` |
| 184 | global | `SECURITY-0003` | `SECURITY_WEBSOCKET_BROKER_ACCESS` | 403 FORBIDDEN | 브로커 경로로 직접 메시지를 전송할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:38` |
| 185 | global | `SECURITY-0004` | `SECURITY_WEBSOCKET_INVALID_DESTINATION` | 403 FORBIDDEN | 허용되지 않은 웹소켓 경로입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:39` |

## llm

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 186 | llm | `LLM-0001` | `CHAT_COMPLETION_FAILED` | 502 BAD_GATEWAY | AI 응답을 생성하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/llm/domain/exception/LlmErrorCode.java:14` |
| 187 | llm | `LLM-0002` | `CHAT_COMPLETION_INVALID_RESPONSE` | 502 BAD_GATEWAY | AI 응답을 읽지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/llm/domain/exception/LlmErrorCode.java:15` |
| 188 | llm | `LLM-0003` | `PROVIDER_NOT_CONFIGURED` | 500 INTERNAL_SERVER_ERROR | AI 제공자 설정이 누락됐어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/llm/domain/exception/LlmErrorCode.java:16` |

## maintenance

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 189 | maintenance | `MAINTENANCE-0001` | `SERVICE_UNDER_MAINTENANCE` | 503 SERVICE_UNAVAILABLE | 서비스 점검 중이에요. 점검이 끝난 뒤 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:14` |
| 190 | maintenance | `MAINTENANCE-0002` | `MAINTENANCE_WINDOW_NOT_FOUND` | 404 NOT_FOUND | 점검 일정을 찾을 수 없어요. 선택한 일정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:15` |
| 191 | maintenance | `MAINTENANCE-0003` | `INVALID_TIME_RANGE` | 400 BAD_REQUEST | 종료 시각은 시작 시각 이후로 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:16` |
| 192 | maintenance | `MAINTENANCE-0004` | `START_AT_IN_PAST` | 400 BAD_REQUEST | 시작 시각은 현재 시각 이후로 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:17` |
| 193 | maintenance | `MAINTENANCE-0005` | `TARGET_DOMAINS_REQUIRED` | 400 BAD_REQUEST | 도메인별 점검은 대상 도메인을 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:18` |
| 194 | maintenance | `MAINTENANCE-0006` | `OVERLAPPING_WINDOW` | 409 CONFLICT | 다른 점검 일정과 시간이 겹쳐요. 시간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:19` |
| 195 | maintenance | `MAINTENANCE-0007` | `ALREADY_ENDED` | 400 BAD_REQUEST | 이미 종료된 점검 일정이에요. 진행 중이거나 예정된 일정을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:20` |
| 196 | maintenance | `MAINTENANCE-0008` | `NOT_SUPER_ADMIN` | 403 FORBIDDEN | 점검을 관리할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:21` |

## member

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 197 | member | `MEMBER-0001` | `MEMBER_NOT_FOUND` | 404 NOT_FOUND | 사용자를 찾을 수 없어요. 선택한 사용자를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:14` |
| 198 | member | `MEMBER-0002` | `MEMBER_ALREADY_EXISTS` | 409 CONFLICT | 이미 등록된 사용자예요. 기존 계정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:15` |
| 199 | member | `MEMBER-0003` | `EMAIL_ALREADY_EXISTS` | 409 CONFLICT | 이미 사용 중인 이메일이에요. 다른 이메일을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:16` |
| 200 | member | `MEMBER-0004` | `MEMBER_ALREADY_WITHDRAWN` | 400 BAD_REQUEST | 이미 탈퇴한 사용자예요. 다른 계정으로 진행해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:17` |
| 201 | member | `MEMBER-0005` | `INVALID_MEMBER_STATUS` | 400 BAD_REQUEST | 사용자 상태가 올바르지 않아요. 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:18` |
| 202 | member | `MEMBER-0006` | `MEMBER_NOT_ACTIVE` | 400 BAD_REQUEST | 활동 중인 사용자만 이용할 수 있어요. 계정 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:19` |
| 203 | member | `MEMBER-0007` | `MEMBER_ALREADY_REGISTERED` | 400 BAD_REQUEST | 이미 회원가입을 완료한 사용자예요. 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:20` |
| 204 | member | `MEMBER-0008` | `MEMBER_PROFILE_NOT_FOUND` | 404 NOT_FOUND | 프로필을 찾을 수 없어요. 프로필 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:21` |
| 205 | member | `MEMBER-0009` | `MEMBER_SCHOOL_NOT_ASSIGNED` | 400 BAD_REQUEST | 학교가 등록되지 않은 사용자예요. 학교 정보를 먼저 등록해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:22` |
| 206 | member | `MEMBER-0010` | `CREDENTIAL_ALREADY_REGISTERED` | 409 CONFLICT | 이미 로그인 ID와 비밀번호가 등록되어 있어요. 기존 정보로 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:23` |
| 207 | member | `MEMBER-0011` | `CREDENTIAL_NOT_REGISTERED` | 400 BAD_REQUEST | 로그인 ID와 비밀번호가 등록되어 있지 않아요. 먼저 등록해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:24` |
| 208 | member | `MEMBER-0012` | `INVALID_LOGIN_ID` | 400 BAD_REQUEST | 로그인 ID가 올바르지 않아요. 다시 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:25` |
| 209 | member | `MEMBER-0013` | `INVALID_PASSWORD` | 400 BAD_REQUEST | 비밀번호가 올바르지 않아요. 다시 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:26` |
| 210 | member | `MEMBER-0014` | `MEMBER_SEARCH_ACCESS_DENIED` | 403 FORBIDDEN | 챌린저 기록이 있는 회원만 회원 검색을 사용할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:27` |

## notice

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 211 | notice | `NOTICE-0001` | `NOTICE_NOT_FOUND` | 404 NOT_FOUND | 공지를 찾을 수 없어요. 목록을 새로고침해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:14` |
| 212 | notice | `NOTICE-0002` | `ALREADY_PUBLISHED_NOTICE` | 400 BAD_REQUEST | 이미 게시된 공지예요. 게시 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:15` |
| 213 | notice | `NOTICE-0003` | `INVALID_NOTICE_TITLE` | 400 BAD_REQUEST | 공지 제목이 올바르지 않아요. 제목을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:16` |
| 214 | notice | `NOTICE-0004` | `INVALID_NOTICE_CONTENT` | 400 BAD_REQUEST | 공지 내용이 올바르지 않아요. 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:17` |
| 215 | notice | `NOTICE-0005` | `INVALID_NOTICE_STATUS_FOR_REMINDER` | 400 BAD_REQUEST | 현재 상태에서는 공지 알림을 보낼 수 없어요. 공지 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:18` |
| 216 | notice | `NOTICE-0006` | `AUTHOR_REQUIRED` | 400 BAD_REQUEST | 공지 작성자 정보가 필요해요. 로그인 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:19` |
| 217 | notice | `NOTICE-0007` | `NOTICE_SCOPE_REQUIRED` | 400 BAD_REQUEST | 공지 대상 범위를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:20` |
| 218 | notice | `NOTICE-0008` | `NOTICE_AUTHOR_MISMATCH` | 403 FORBIDDEN | 공지 작성자만 수정할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:21` |
| 219 | notice | `NOTICE-0009` | `NO_WRITE_PERMISSION` | 403 FORBIDDEN | 공지를 작성할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:22` |
| 220 | notice | `NOTICE-0010` | `INVALID_TARGET_SETTING` | 400 BAD_REQUEST | 공지 수신자 설정이 올바르지 않아요. 대상 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:26` |
| 221 | notice | `NOTICE-0011` | `NO_TARGET_FOUND` | 404 NOT_FOUND | 공지 수신 대상을 찾을 수 없어요. 대상 설정을 다시 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:27` |
| 222 | notice | `NOTICE-0012` | `NO_READ_PERMISSION` | 403 FORBIDDEN | 공지를 조회할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:24` |
| 223 | notice | `NOTICE-9999` | `NOT_IMPLEMENTED_YET` | 501 NOT_IMPLEMENTED | 아직 사용할 수 없는 기능이에요. 필요한 기능이라면 서버팀에 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:45` |
| 224 | notice | `NOTICE-CONTENTS-0001` | `VOTE_IDS_REQUIRED` | 400 BAD_REQUEST | 투표를 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:30` |
| 225 | notice | `NOTICE-CONTENTS-0002` | `IMAGE_URLS_REQUIRED` | 400 BAD_REQUEST | 이미지 링크를 1개 이상 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:31` |
| 226 | notice | `NOTICE-CONTENTS-0003` | `LINK_URLS_REQUIRED` | 400 BAD_REQUEST | 공지 링크를 1개 이상 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:32` |
| 227 | notice | `NOTICE-CONTENTS-0004` | `NOTICE_VOTE_NOT_FOUND` | 404 NOT_FOUND | 공지 투표를 찾을 수 없어요. 투표를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:33` |
| 228 | notice | `NOTICE-CONTENTS-0005` | `NOTICE_IMAGE_NOT_FOUND` | 404 NOT_FOUND | 공지 이미지를 찾을 수 없어요. 이미지를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:34` |
| 229 | notice | `NOTICE-CONTENTS-0006` | `NOTICE_LINK_NOT_FOUND` | 404 NOT_FOUND | 공지 링크를 찾을 수 없어요. 링크를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:35` |
| 230 | notice | `NOTICE-CONTENTS-0007` | `IMAGE_LIMIT_EXCEEDED` | 400 BAD_REQUEST | 공지 이미지는 최대 10장까지 등록할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:36` |
| 231 | notice | `NOTICE-CONTENTS-0008` | `VOTE_ALREADY_EXISTS` | 409 CONFLICT | 이 공지에는 이미 투표가 있어요. 기존 투표를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:37` |
| 232 | notice | `NOTICE-CONTENTS-0009` | `INVALID_VOTE_OPTION_COUNT` | 400 BAD_REQUEST | 투표 선택지는 2개 이상 5개 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:38` |
| 233 | notice | `NOTICE-CONTENTS-0010` | `INVALID_VOTE_OPTION_CONTENT` | 400 BAD_REQUEST | 투표 선택지에 빈 값이 있어요. 선택지 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:39` |
| 234 | notice | `NOTICE-CONTENTS-0011` | `VOTE_NOT_STARTED` | 400 BAD_REQUEST | 아직 투표 기간이 시작되지 않았어요. 시작 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:40` |
| 235 | notice | `NOTICE-CONTENTS-0012` | `VOTE_CLOSED` | 400 BAD_REQUEST | 이미 종료된 투표예요. 투표 기간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:41` |
| 236 | notice | `NOTICE-CONTENTS-0013` | `SELECTED_OPTION_IDS_REQUIRED` | 400 BAD_REQUEST | 투표 선택지를 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:42` |

## notification

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 237 | notification | `EMAIL-0004` | `EMAIL_TEMPLATE_RENDER_FAILED` | 500 INTERNAL_SERVER_ERROR | 이메일 본문을 만들지 못했어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/EmailErrorCode.java:13` |
| 238 | notification | `EMAIL-0005` | `EMAIL_SEND_FAILED` | 500 INTERNAL_SERVER_ERROR | 이메일을 보내지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/EmailErrorCode.java:14` |
| 239 | notification | `FCM-0001` | `FCM_NOT_FOUND` | 404 NOT_FOUND | 푸시 알림 정보를 찾을 수 없어요. 알림 설정을 다시 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:14` |
| 240 | notification | `FCM-0002` | `USER_FCM_NOT_FOUND` | 404 NOT_FOUND | 사용자의 푸시 알림 정보를 찾을 수 없어요. 알림 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:15` |
| 241 | notification | `FCM-0003` | `FCM_SEND_FAILED` | 500 INTERNAL_SERVER_ERROR | 푸시 알림을 보내지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:16` |
| 242 | notification | `FCM-0004` | `TOPIC_SUBSCRIBE_FAILED` | 500 INTERNAL_SERVER_ERROR | 푸시 알림 주제를 구독하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:17` |
| 243 | notification | `FCM-0005` | `TOPIC_UNSUBSCRIBE_FAILED` | 500 INTERNAL_SERVER_ERROR | 푸시 알림 주제 구독을 해제하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:18` |
| 244 | notification | `FCM-0006` | `TOPIC_SEND_FAILED` | 500 INTERNAL_SERVER_ERROR | 푸시 알림 주제 메시지를 보내지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:19` |
| 245 | notification | `FCM-0007` | `RATE_LIMITED` | 429 TOO_MANY_REQUESTS | 푸시 알림 요청이 너무 많아요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:20` |
| 246 | notification | `WEBHOOK-0001` | `WEBHOOK_SEND_FAILED` | 500 INTERNAL_SERVER_ERROR | 웹훅 메시지를 보내지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/WebhookErrorCode.java:14` |
| 247 | notification | `WEBHOOK-0002` | `WEBHOOK_ADAPTER_NOT_FOUND` | 400 BAD_REQUEST | 해당 플랫폼의 웹훅 설정을 찾을 수 없어요. 플랫폼 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/WebhookErrorCode.java:15` |

## organization

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 248 | organization | `ORGANIZATION-0001` | `GISU_REQUIRED` | 400 BAD_REQUEST | 기수를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:14` |
| 249 | organization | `ORGANIZATION-0002` | `ORGAN_NAME_REQUIRED` | 400 BAD_REQUEST | 조직 이름을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:15` |
| 250 | organization | `ORGANIZATION-0003` | `SCHOOL_REQUIRED` | 400 BAD_REQUEST | 학교를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:16` |
| 251 | organization | `ORGANIZATION-0004` | `CHAPTER_REQUIRED` | 400 BAD_REQUEST | 지부를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:17` |
| 252 | organization | `ORGANIZATION-0005` | `GISU_START_AT_REQUIRED` | 400 BAD_REQUEST | 기수 시작일을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:20` |
| 253 | organization | `ORGANIZATION-0006` | `GISU_END_AT_REQUIRED` | 400 BAD_REQUEST | 기수 종료일을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:21` |
| 254 | organization | `ORGANIZATION-0007` | `GISU_PERIOD_INVALID` | 400 BAD_REQUEST | 기수 시작일은 종료일보다 빨라야 해요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:22` |
| 255 | organization | `ORGANIZATION-0008` | `SCHOOL_NAME_REQUIRED` | 400 BAD_REQUEST | 학교 이름을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:24` |
| 256 | organization | `ORGANIZATION-0009` | `SCHOOL_DOMAIN_REQUIRED` | 400 BAD_REQUEST | 학교 이메일 도메인을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:25` |
| 257 | organization | `ORGANIZATION-0010` | `STUDY_GROUP_NAME_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 이름을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:27` |
| 258 | organization | `ORGANIZATION-0011` | `STUDY_GROUP_LEADER_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 리더를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:28` |
| 259 | organization | `ORGANIZATION-0012` | `STUDY_GROUP_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:30` |
| 260 | organization | `ORGANIZATION-0013` | `STUDY_GROUP_MEMBER_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 멤버는 1명 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:31` |
| 261 | organization | `ORGANIZATION-0014` | `STUDY_GROUP_MEMBER_ID_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 멤버를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:32` |
| 262 | organization | `ORGANIZATION-0015` | `STUDY_GROUP_MEMBER_ALREADY_EXISTS` | 400 BAD_REQUEST | 이미 스터디 그룹에 포함된 멤버예요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:33` |
| 263 | organization | `ORGANIZATION-0016` | `STUDY_GROUP_MEMBER_NOT_FOUND` | 404 NOT_FOUND | 스터디 그룹 멤버를 찾을 수 없어요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:34` |
| 264 | organization | `ORGANIZATION-0017` | `CHAPTER_NOT_FOUND` | 404 NOT_FOUND | 지부를 찾을 수 없어요. 선택한 지부를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:36` |
| 265 | organization | `ORGANIZATION-0018` | `SCHOOL_NOT_FOUND` | 404 NOT_FOUND | 학교를 찾을 수 없어요. 선택한 학교를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:37` |
| 266 | organization | `ORGANIZATION-0019` | `GISU_IS_ACTIVE_NOT_FOUND` | 404 NOT_FOUND | 활성화된 기수를 찾을 수 없어요. 기수 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:38` |
| 267 | organization | `ORGANIZATION-0020` | `GISU_NOT_FOUND` | 404 NOT_FOUND | 기수를 찾을 수 없어요. 선택한 기수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:39` |
| 268 | organization | `ORGANIZATION-0021` | `PART_REQUIRED` | 400 BAD_REQUEST | 파트를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:40` |
| 269 | organization | `ORGANIZATION-0022` | `STUDY_GROUP_NAME_INVALID` | 400 BAD_REQUEST | 스터디 그룹 이름이 올바르지 않아요. 이름을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:41` |
| 270 | organization | `ORGANIZATION-0023` | `STUDY_GROUP_NOT_FOUND` | 400 BAD_REQUEST | 스터디 그룹을 찾을 수 없어요. 선택한 그룹을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:42` |
| 271 | organization | `ORGANIZATION-0024` | `STUDY_GROUP_CHALLENGER_INVALID` | 400 BAD_REQUEST | 스터디 그룹 리더 또는 멤버에 존재하지 않는 챌린저가 있어요. 구성원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:44` |
| 272 | organization | `ORGANIZATION-0025` | `LEADER_CANNOT_BE_MEMBER` | 400 BAD_REQUEST | 스터디 그룹 리더는 멤버로 중복 등록할 수 없어요. 구성원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:46` |
| 273 | organization | `ORGANIZATION-0026` | `STUDY_GROUP_MEMBER_DUPLICATED` | 400 BAD_REQUEST | 스터디 그룹 멤버가 중복됐어요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:47` |
| 274 | organization | `ORGANIZATION-0027` | `NO_SUCH_CHAPTER_SCHOOL` | 404 NOT_FOUND | 학교와 지부 연결 정보를 찾을 수 없어요. 배정 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:48` |
| 275 | organization | `ORGANIZATION-0028` | `GISU_ALREADY_EXISTS` | 409 CONFLICT | 이미 존재하는 기수예요. 기존 기수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:49` |
| 276 | organization | `ORGANIZATION-0029` | `SCHOOL_ALREADY_ASSIGNED_TO_CHAPTER` | 409 CONFLICT | 해당 기수에서 이미 다른 지부에 배정된 학교가 있어요. 학교 배정 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:50` |
| 277 | organization | `ORGANIZATION-0030` | `CHAPTER_NAME_DUPLICATED` | 409 CONFLICT | 해당 기수에 같은 이름의 지부가 이미 있어요. 다른 이름을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:52` |
| 278 | organization | `ORGANIZATION-0031` | `STUDY_GROUP_ACCESS_DENIED` | 403 FORBIDDEN | 스터디 그룹을 조회할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:54` |
| 279 | organization | `ORGANIZATION-0032` | `GISU_HAS_ASSOCIATED_CHAPTERS` | 409 CONFLICT | 연결된 지부 또는 학교가 있어 기수를 삭제할 수 없어요. 연결 정보를 먼저 정리해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:56` |
| 280 | organization | `ORGANIZATION-0033` | `STUDY_GROUP_MENTOR_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 파트장은 1명 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:58` |
| 281 | organization | `ORGANIZATION-0034` | `STUDY_GROUP_MENTOR_ID_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 파트장을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:59` |
| 282 | organization | `ORGANIZATION-0035` | `STUDY_GROUP_MEMBER_ALREADY_IN_PART_STUDY` | 409 CONFLICT | 다른 스터디 그룹에 이미 속한 멤버가 있어요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:61` |
| 283 | organization | `ORGANIZATION-0036` | `STUDY_GROUP_MENTOR_DUPLICATED` | 400 BAD_REQUEST | 이미 해당 스터디에 속한 파트장이에요. 파트장 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:62` |
| 284 | organization | `ORGANIZATION-0037` | `STUDY_GROUP_MENTOR_NOT_FOUND` | 404 NOT_FOUND | 스터디 그룹 파트장 정보를 찾을 수 없어요. 파트장 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:63` |
| 285 | organization | `ORGANIZATION-0038` | `STUDY_GROUP_SCHEDULE_ATTENDANCE_POLICY_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 일정에는 출석 정책이 필요해요. 출석 정책을 설정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:65` |
| 286 | organization | `ORGANIZATION-0045` | `UMC_PRODUCT_MEMBER_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 인원은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:68` |
| 287 | organization | `ORGANIZATION-0046` | `UMC_PRODUCT_MEMBER_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT 인원을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:69` |
| 288 | organization | `ORGANIZATION-0047` | `UMC_PRODUCT_MEMBER_ALREADY_EXISTS` | 409 CONFLICT | 이미 등록된 UMC PRODUCT 인원입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:70` |
| 289 | organization | `ORGANIZATION-0048` | `UMC_PRODUCT_MEMBER_ID_REQUIRED` | 400 BAD_REQUEST | 회원 ID는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:71` |
| 290 | organization | `ORGANIZATION-0051` | `UMC_PRODUCT_ROLE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 직책은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:72` |
| 291 | organization | `ORGANIZATION-0052` | `UMC_PRODUCT_POSITION_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 포지션은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:73` |
| 292 | organization | `ORGANIZATION-0053` | `UMC_PRODUCT_ACCESS_DENIED` | 403 FORBIDDEN | UMC PRODUCT 관리 권한이 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:74` |
| 293 | organization | `ORGANIZATION-0058` | `UMC_PRODUCT_SQUAD_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Squad는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:75` |
| 294 | organization | `ORGANIZATION-0059` | `UMC_PRODUCT_SQUAD_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Squad를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:76` |
| 295 | organization | `ORGANIZATION-0060` | `UMC_PRODUCT_SQUAD_CODE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Squad 코드는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:77` |
| 296 | organization | `ORGANIZATION-0061` | `UMC_PRODUCT_SQUAD_NAME_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Squad 이름은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:78` |
| 297 | organization | `ORGANIZATION-0065` | `GISU_QUERY_CONDITION_INVALID` | 400 BAD_REQUEST | 기수 조회 조건이 올바르지 않습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:79` |
| 298 | organization | `ORGANIZATION-0066` | `UMC_PRODUCT_START_DATE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 활동 시작일은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:81` |
| 299 | organization | `ORGANIZATION-0067` | `UMC_PRODUCT_PERIOD_INVALID` | 400 BAD_REQUEST | UMC PRODUCT 활동 종료일은 시작일보다 빠를 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:83` |
| 300 | organization | `ORGANIZATION-0068` | `UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 멤버 활동 기간은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:85` |
| 301 | organization | `ORGANIZATION-0069` | `UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE` | 400 BAD_REQUEST | 활동 기간은 멤버 활동 기간과 상위 활동 기간 안에 있어야 합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:87` |
| 302 | organization | `ORGANIZATION-0070` | `UMC_PRODUCT_ACTIVITY_PERIOD_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT 멤버 활동 기간을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:89` |
| 303 | organization | `ORGANIZATION-0071` | `UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED` | 409 CONFLICT | UMC PRODUCT 멤버 활동 기간은 겹치거나 빈 날짜 없이 이어질 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:91` |
| 304 | organization | `ORGANIZATION-0072` | `UMC_PRODUCT_ACTIVITY_PERIOD_HAS_ASSOCIATIONS` | 409 CONFLICT | 연결된 활동 이력이 있어 멤버 활동 기간을 삭제할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:93` |
| 305 | organization | `ORGANIZATION-0073` | `UMC_PRODUCT_CHAPTER_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Chapter는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:95` |
| 306 | organization | `ORGANIZATION-0074` | `UMC_PRODUCT_CHAPTER_CODE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Chapter 코드는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:97` |
| 307 | organization | `ORGANIZATION-0075` | `UMC_PRODUCT_CHAPTER_NAME_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Chapter 이름은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:99` |
| 308 | organization | `ORGANIZATION-0076` | `UMC_PRODUCT_CHAPTER_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Chapter를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:101` |
| 309 | organization | `ORGANIZATION-0077` | `UMC_PRODUCT_CHAPTER_ALREADY_EXISTS` | 409 CONFLICT | 이미 존재하는 UMC PRODUCT Chapter 코드입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:103` |
| 310 | organization | `ORGANIZATION-0078` | `UMC_PRODUCT_CHAPTER_HAS_MEMBERSHIPS` | 409 CONFLICT | 연결된 소속 이력이 있어 UMC PRODUCT Chapter를 삭제할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:105` |
| 311 | organization | `ORGANIZATION-0086` | `UMC_PRODUCT_CHAPTER_MEMBERSHIP_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Chapter 소속 이력을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:107` |
| 312 | organization | `ORGANIZATION-0087` | `UMC_PRODUCT_CHAPTER_MEMBERSHIP_OVERLAPPED` | 409 CONFLICT | 동일한 UMC PRODUCT Chapter 소속 활동 기간이 겹칩니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:109` |
| 313 | organization | `ORGANIZATION-0089` | `UMC_PRODUCT_LEADERSHIP_ROLE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Leadership 역할은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:111` |
| 314 | organization | `ORGANIZATION-0090` | `UMC_PRODUCT_LEADERSHIP_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Leadership 이력을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:113` |
| 315 | organization | `ORGANIZATION-0091` | `UMC_PRODUCT_LEADERSHIP_OVERLAPPED` | 409 CONFLICT | 해당 기간에 중복되는 UMC PRODUCT Leadership이 존재합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:115` |
| 316 | organization | `ORGANIZATION-0092` | `UMC_PRODUCT_SQUAD_PARTICIPANT_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Squad 참여 이력을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:117` |
| 317 | organization | `ORGANIZATION-0093` | `UMC_PRODUCT_SQUAD_PARTICIPATION_OVERLAPPED` | 409 CONFLICT | 동일 멤버의 UMC PRODUCT Squad 참여 기간이 겹칩니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:119` |
| 318 | organization | `ORGANIZATION-0094` | `UMC_PRODUCT_SQUAD_LEAD_OVERLAPPED` | 409 CONFLICT | 해당 기간에 이미 UMC PRODUCT Squad Lead가 존재합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:121` |
| 319 | organization | `ORGANIZATION-0095` | `UMC_PRODUCT_SQUAD_HAS_PARTICIPANTS` | 409 CONFLICT | 연결된 참여 이력이 있어 UMC PRODUCT Squad를 삭제할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:123` |
| 320 | organization | `ORGANIZATION-0096` | `UMC_PRODUCT_SQUAD_ALREADY_EXISTS` | 409 CONFLICT | 이미 존재하는 UMC PRODUCT Squad 코드입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:125` |

## project

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 321 | project | `PROJECT-0001` | `PROJECT_NOT_FOUND` | 404 NOT_FOUND | 프로젝트를 찾을 수 없어요. 선택한 프로젝트를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:15` |
| 322 | project | `PROJECT-0002` | `ALREADY_COMPLETED_PROJECT` | 400 BAD_REQUEST | 이미 완료된 프로젝트예요. 프로젝트 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:16` |
| 323 | project | `PROJECT-0003` | `PROJECT_ABORT_UNAVAILABLE` | 400 BAD_REQUEST | 이 프로젝트는 중단할 수 없어요. 프로젝트 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:17` |
| 324 | project | `PROJECT-0004` | `APPLICATION_NOT_SUBMITTED` | 400 BAD_REQUEST | 제출된 지원서에서만 할 수 있는 작업이에요. 지원서 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:20` |
| 325 | project | `PROJECT-0005` | `APPLICATION_SUBMIT_NOT_AVAILABLE` | 400 BAD_REQUEST | 이미 제출했거나 평가가 끝난 지원서예요. 지원서 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:21` |
| 326 | project | `PROJECT-0006` | `APPLICATION_FORM_NOT_FOUND` | 404 NOT_FOUND | 프로젝트 지원 폼을 찾을 수 없어요. 선택한 프로젝트를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:29` |
| 327 | project | `PROJECT-0007` | `APPLICATION_FORM_ACCESS_NOT_ALLOWED` | 403 FORBIDDEN | 이 지원 폼 섹션에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:30` |
| 328 | project | `PROJECT-0008` | `PROJECT_DRAFT_ALREADY_IN_PROGRESS` | 409 CONFLICT | 작성 중인 프로젝트가 있어 새로 시작할 수 없어요. 기존 초안을 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:40` |
| 329 | project | `PROJECT-0009` | `PROJECT_INVALID_STATE` | 400 BAD_REQUEST | 현재 상태에서는 할 수 없는 작업이에요. 프로젝트 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:41` |
| 330 | project | `PROJECT-0010` | `PROJECT_OWNER_NOT_PLAN_CHALLENGER` | 400 BAD_REQUEST | 프로젝트 PO는 PLAN 파트 챌린저만 맡을 수 있어요. PO 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:42` |
| 331 | project | `PROJECT-0011` | `PROJECT_SUBMIT_VALIDATION_FAILED` | 400 BAD_REQUEST | 제출에 필요한 정보가 부족해요. 필수 항목을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:43` |
| 332 | project | `PROJECT-0012` | `PROJECT_ACCESS_DENIED` | 403 FORBIDDEN | 이 프로젝트에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:44` |
| 333 | project | `PROJECT-0013` | `APPLICATION_FORM_POLICY_PARTS_EMPTY` | 400 BAD_REQUEST | 파트 섹션에는 파트를 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:32` |
| 334 | project | `PROJECT-0014` | `APPLICATION_FORM_INVALID_SECTION_ID` | 400 BAD_REQUEST | 현재 폼에 없는 섹션이에요. 섹션을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:33` |
| 335 | project | `PROJECT-0015` | `APPLICATION_FORM_INVALID_QUESTION_ID` | 400 BAD_REQUEST | 해당 섹션에 없는 질문이에요. 질문을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:34` |
| 336 | project | `PROJECT-0016` | `APPLICATION_FORM_INVALID_OPTION_ID` | 400 BAD_REQUEST | 해당 질문에 없는 선택지예요. 선택지를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:35` |
| 337 | project | `PROJECT-0017` | `APPLICATION_FORM_OPTIONS_NOT_ALLOWED` | 400 BAD_REQUEST | 선택형 질문에만 선택지를 추가할 수 있어요. 질문 유형을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:36` |
| 338 | project | `PROJECT-0018` | `APPLICATION_FORM_OPTIONS_REQUIRED` | 400 BAD_REQUEST | 선택형 질문에는 선택지가 1개 이상 필요해요. 선택지를 추가해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:37` |
| 339 | project | `PROJECT-0019` | `APPLICATION_DRAFT_NOT_EXPOSABLE` | 500 INTERNAL_SERVER_ERROR | 임시저장 지원서를 운영진 응답으로 보여줄 수 없어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:22` |
| 340 | project | `PROJECT-0020` | `APPLICATION_DRAFT_FILTER_NOT_ALLOWED` | 400 BAD_REQUEST | 운영진 지원자 목록에서는 임시저장 상태를 필터로 사용할 수 없어요. 다른 상태를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:24` |
| 341 | project | `PROJECT-0021` | `PROJECT_APPLICATION_NOT_FOUND` | 404 NOT_FOUND | 지원서를 찾을 수 없어요. 선택한 지원서를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:26` |
| 342 | project | `PROJECT-0022` | `PROJECT_DELETE_NOT_ALLOWED_IN_STATUS` | 409 CONFLICT | 프로젝트는 DRAFT 또는 PENDING_REVIEW 상태에서만 삭제할 수 있어요. 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:46` |
| 343 | project | `PROJECT-0023` | `PROJECT_ABORT_REASON_REQUIRED` | 400 BAD_REQUEST | 프로젝트 중단 사유를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:48` |
| 344 | project | `PROJECT-0100` | `PROJECT_MEMBER_NOT_FOUND` | 404 NOT_FOUND | 프로젝트 멤버를 찾을 수 없어요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:51` |
| 345 | project | `PROJECT-0101` | `PROJECT_MEMBER_ALREADY_EXISTS` | 409 CONFLICT | 이미 이 프로젝트의 멤버예요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:52` |
| 346 | project | `PROJECT-0102` | `PROJECT_MAIN_PM_REMOVAL_REQUIRES_TRANSFER` | 400 BAD_REQUEST | 메인 PM은 팀원 제거가 아니라 소유권 양도로 변경해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:53` |
| 347 | project | `PROJECT-0200` | `PROJECT_PART_QUOTA_INVALID` | 400 BAD_REQUEST | 파트 정원은 1명 이상으로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:56` |
| 348 | project | `PROJECT-0202` | `PROJECT_PART_QUOTA_REQUIRED` | 400 BAD_REQUEST | 프로젝트를 공개하려면 파트별 정원을 1개 이상 등록해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:57` |
| 349 | project | `PROJECT-0203` | `PROJECT_PART_QUOTA_DUPLICATE` | 400 BAD_REQUEST | 동일한 파트가 중복됐어요. 파트별 정원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:58` |
| 350 | project | `PROJECT-0204` | `PROJECT_DRAFT_APPLICATION_NOT_FOUND` | 404 NOT_FOUND | 작성 중인 지원서를 찾을 수 없어요. 지원서 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:86` |
| 351 | project | `PROJECT-0205` | `PROJECT_APPLICATION_PART_NOT_ALLOWED` | 403 FORBIDDEN | 이 프로젝트에 지원할 수 있는 파트가 아니에요. 지원 가능한 파트를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:87` |
| 352 | project | `PROJECT-0206` | `PROJECT_APPLICATION_MEMBER_ALREADY_IN_TEAM` | 409 CONFLICT | 이미 해당 기수에 소속된 팀이 있어 지원할 수 없어요. 팀 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:88` |
| 353 | project | `PROJECT-0207` | `PROJECT_APPLICATION_DUPLICATE_SUBMISSION` | 409 CONFLICT | 동일한 매칭 차수에 이미 제출한 지원서가 있어요. 기존 지원서를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:89` |
| 354 | project | `PROJECT-0208` | `PROJECT_APPLICATION_ROUND_NOT_OPEN` | 400 BAD_REQUEST | 현재는 해당 매칭 차수의 지원 기간이 아니에요. 지원 기간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:90` |
| 355 | project | `PROJECT-0209` | `PROJECT_APPLICATION_ROUND_TYPE_MISMATCH` | 400 BAD_REQUEST | 선택한 매칭 차수가 내 파트와 맞지 않아요. 매칭 차수를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:91` |
| 356 | project | `PROJECT-0210` | `PROJECT_APPLICATION_ALREADY_EXISTS` | 409 CONFLICT | 이미 작성 중인 지원서가 있어요. 기존 지원서를 이어서 작성해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:92` |
| 357 | project | `PROJECT-0211` | `PROJECT_APPLICATION_SELF_APPLY_NOT_ALLOWED` | 403 FORBIDDEN | 내가 운영하는 프로젝트에는 지원할 수 없어요. 다른 프로젝트를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:93` |
| 358 | project | `PROJECT-0212` | `PROJECT_APPLICATION_DECISION_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 상태에서는 합격 여부를 변경할 수 없어요. 지원서 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:94` |
| 359 | project | `PROJECT-0213` | `PROJECT_APPLICATION_QUOTA_EXCEEDED` | 409 CONFLICT | 해당 파트의 남은 자리를 초과해 합격 처리할 수 없어요. 파트 정원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:96` |
| 360 | project | `PROJECT-0214` | `PROJECT_APPLICATION_CANCEL_NOT_ALLOWED` | 400 BAD_REQUEST | 이미 종결된 지원서는 철회할 수 없어요. 지원서 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:98` |
| 361 | project | `PROJECT-0215` | `PROJECT_APPLICATION_CANCEL_ROUND_CLOSED` | 400 BAD_REQUEST | 매칭 차수가 종료되어 지원서를 철회할 수 없어요. 차수 기간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:99` |
| 362 | project | `PROJECT-0216` | `PROJECT_APPLICATION_MINIMUM_SELECTION_REQUIRED` | 409 CONFLICT | 매칭 규칙의 최소 선발 인원을 충족하지 않아 불합격 처리할 수 없어요. 합격 인원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:100` |
| 363 | project | `PROJECT-0300` | `PROJECT_MATCHING_ROUND_NOT_FOUND` | 404 NOT_FOUND | 매칭 차수를 찾을 수 없어요. 선택한 차수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:61` |
| 364 | project | `PROJECT-0301` | `PROJECT_MATCHING_ROUND_INVALID_PERIOD` | 400 BAD_REQUEST | 매칭 차수 기간은 시작, 종료, 결정 마감 순서여야 해요. 시간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:62` |
| 365 | project | `PROJECT-0302` | `PROJECT_MATCHING_ROUND_PERIOD_OVERLAPPED` | 409 CONFLICT | 같은 지부의 다른 매칭 차수와 기간이 겹쳐요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:64` |
| 366 | project | `PROJECT-0303` | `PROJECT_MATCHING_ROUND_ACCESS_DENIED` | 403 FORBIDDEN | 이 매칭 차수를 관리할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:66` |
| 367 | project | `PROJECT-0304` | `PROJECT_MATCHING_ROUND_DELETE_CONFLICT` | 409 CONFLICT | 연결된 지원서가 있는 매칭 차수는 삭제할 수 없어요. 지원서를 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:68` |
| 368 | project | `PROJECT-0305` | `PROJECT_MATCHING_ROUND_TIME_REQUIRES_CHAPTER` | 400 BAD_REQUEST | 시간 기준으로 조회하려면 지부를 함께 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:70` |
| 369 | project | `PROJECT-0306` | `PROJECT_MATCHING_ROUND_LOCKED` | 400 BAD_REQUEST | 결정 마감 시각이 지나 결정을 변경할 수 없어요. 차수 기간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:72` |
| 370 | project | `PROJECT-0307` | `PROJECT_MATCHING_ROUND_NOT_FINALIZABLE` | 400 BAD_REQUEST | 결정 마감 시각이 지난 뒤 자동 선발을 실행할 수 있어요. 마감 시각을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:78` |
| 371 | project | `PROJECT-0308` | `PROJECT_MATCHING_ROUND_POLICY_NOT_FOUND` | 500 INTERNAL_SERVER_ERROR | 이 매칭 종류의 자동 선발 정책을 찾지 못했어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:80` |
| 372 | project | `PROJECT-0309` | `PROJECT_MATCHING_ROUND_PHASE_SEQUENCE_INVALID` | 409 CONFLICT | 매칭 차수는 FIRST, SECOND, THIRD 순서로 배치하고 이전 차수 결정 마감 이후 1분 이상 간격을 둬야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:82` |
| 373 | project | `PROJECT-0310` | `PROJECT_MATCHING_ROUND_NOT_ENDED` | 400 BAD_REQUEST | 아직 지원 기간이 끝나지 않아 결정을 변경할 수 없어요. 지원 종료 시각을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:74` |
| 374 | project | `PROJECT-0311` | `PROJECT_MATCHING_ROUND_APPLICANTS_NOT_VIEWABLE` | 400 BAD_REQUEST | 아직 지원 기간이 끝나지 않아 지원서를 조회할 수 없어요. 지원 종료 시각을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:76` |

## recruiting

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 375 | recruiting | `RECRUITING-0001` | `RECRUITING_SEASON_NOT_FOUND` | 404 NOT_FOUND | 모집 시즌을 찾을 수 없어요. 모집 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:14` |
| 376 | recruiting | `RECRUITING-0002` | `RECRUITING_ROUND_NOT_FOUND` | 404 NOT_FOUND | 모집 차수를 찾을 수 없어요. 모집 차수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:15` |
| 377 | recruiting | `RECRUITING-0003` | `RECRUITING_APPLICATION_FORM_NOT_FOUND` | 404 NOT_FOUND | 지원 폼을 찾을 수 없어요. 지원 폼을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:16` |
| 378 | recruiting | `RECRUITING-0004` | `RECRUITING_APPLICATION_NOT_FOUND` | 404 NOT_FOUND | 지원서를 찾을 수 없어요. 지원서 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:17` |
| 379 | recruiting | `RECRUITING-0006` | `RECRUITING_INTERVIEW_SCHEDULE_NOT_FOUND` | 404 NOT_FOUND | 면접 일정을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:18` |
| 380 | recruiting | `RECRUITING-0007` | `RECRUITING_APPLICATION_EVALUATION_NOT_FOUND` | 404 NOT_FOUND | 지원서 평가를 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:19` |
| 381 | recruiting | `RECRUITING-0008` | `RECRUITING_ROUND_EVALUATOR_NOT_FOUND` | 404 NOT_FOUND | 모집 차수 평가자를 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:20` |
| 382 | recruiting | `RECRUITING-0009` | `RECRUITING_ROUND_INTERVIEW_QUESTION_NOT_FOUND` | 404 NOT_FOUND | 공통 면접 질문을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:21` |
| 383 | recruiting | `RECRUITING-0010` | `RECRUITING_APPLICATION_INTERVIEW_QUESTION_NOT_FOUND` | 404 NOT_FOUND | 개별 면접 질문을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:22` |
| 384 | recruiting | `RECRUITING-0100` | `RECRUITING_ROUND_INVALID_ROUND_NO` | 400 BAD_REQUEST | 추가모집 차수는 1 이상이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:23` |
| 385 | recruiting | `RECRUITING-0101` | `RECRUITING_SEASON_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 모집 시즌 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:24` |
| 386 | recruiting | `RECRUITING-0102` | `RECRUITING_ROUND_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 모집 차수 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:25` |
| 387 | recruiting | `RECRUITING-0103` | `RECRUITING_SEASON_ALREADY_EXISTS` | 409 CONFLICT | 이미 같은 학교와 기수의 모집 시즌이 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:26` |
| 388 | recruiting | `RECRUITING-0104` | `RECRUITING_ROUND_ALREADY_EXISTS` | 409 CONFLICT | 이미 같은 시즌의 모집 차수가 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:27` |
| 389 | recruiting | `RECRUITING-0105` | `RECRUITING_SEASON_REQUIRED_FIELD` | 400 BAD_REQUEST | 모집 시즌 생성에는 기수와 학교가 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:28` |
| 390 | recruiting | `RECRUITING-0106` | `RECRUITING_QUOTA_INVALID_TARGET_COUNT` | 400 BAD_REQUEST | 모집 목표 인원은 0명 이상이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:29` |
| 391 | recruiting | `RECRUITING-0107` | `RECRUITING_QUOTA_UNSUPPORTED_TRACK` | 400 BAD_REQUEST | 해당 트랙은 모집 목표 인원을 설정할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:30` |
| 392 | recruiting | `RECRUITING-0108` | `RECRUITING_QUOTA_DUPLICATE_TRACK` | 409 CONFLICT | 같은 트랙의 모집 목표 인원을 중복 설정할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:31` |
| 393 | recruiting | `RECRUITING-0109` | `RECRUITING_ROUND_INVALID_TRACKS` | 400 BAD_REQUEST | 모집 차수의 트랙 구성이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:32` |
| 394 | recruiting | `RECRUITING-0110` | `RECRUITING_ROUND_TRACK_NOT_IN_SEASON` | 400 BAD_REQUEST | 모집 차수 트랙은 시즌 모집 트랙에 포함되어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:33` |
| 395 | recruiting | `RECRUITING-0111` | `RECRUITING_ROUND_INVALID_SCHEDULE` | 400 BAD_REQUEST | 모집 차수 일정 순서가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:34` |
| 396 | recruiting | `RECRUITING-0112` | `RECRUITING_SEASON_CREATION_FORBIDDEN` | 403 FORBIDDEN | 해당 기수와 학교의 모집 시즌을 생성할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:35` |
| 397 | recruiting | `RECRUITING-0113` | `RECRUITING_ROUND_RECRUITMENT_POLICY_LOCKED` | 409 CONFLICT | 지원서가 있거나 모집 중인 차수의 트랙과 2지망 정책은 변경할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:36` |
| 398 | recruiting | `RECRUITING-0114` | `RECRUITING_ROUND_INVALID_TITLE` | 400 BAD_REQUEST | 모집 제목은 1자 이상 100자 이하여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:37` |
| 399 | recruiting | `RECRUITING-0115` | `RECRUITING_ROUND_TITLE_ALREADY_EXISTS` | 409 CONFLICT | 같은 시즌에 동일한 모집 제목이 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:38` |
| 400 | recruiting | `RECRUITING-0116` | `RECRUITING_ROUND_NO_SEQUENCE_CONFLICT` | 409 CONFLICT | 추가모집 차수는 이전 차수 다음 번호여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:39` |
| 401 | recruiting | `RECRUITING-0117` | `RECRUITING_ROUND_DELETE_CONFLICT` | 409 CONFLICT | 초안이며 지원서와 Form 응답이 없는 모집만 삭제할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:40` |
| 402 | recruiting | `RECRUITING-0118` | `RECRUITING_ROUND_UNPUBLISH_CONFLICT` | 409 CONFLICT | 지원서 또는 Form 응답이 있는 모집은 비공개할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:41` |
| 403 | recruiting | `RECRUITING-0200` | `RECRUITING_APPLICATION_FORM_INVALID` | 400 BAD_REQUEST | 지원 폼 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:42` |
| 404 | recruiting | `RECRUITING-0201` | `RECRUITING_APPLICATION_FORM_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 지원 폼 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:43` |
| 405 | recruiting | `RECRUITING-0202` | `RECRUITING_APPLICATION_FORM_ALREADY_EXISTS` | 409 CONFLICT | 이미 해당 모집 차수에 연결된 지원 폼이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:44` |
| 406 | recruiting | `RECRUITING-0203` | `RECRUITING_APPLICATION_FORM_NOT_PUBLISHED` | 400 BAD_REQUEST | 게시된 지원 폼에만 지원할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:45` |
| 407 | recruiting | `RECRUITING-0204` | `RECRUITING_FORM_SECTION_POLICY_INVALID` | 400 BAD_REQUEST | 지원 폼 섹션 정책이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:46` |
| 408 | recruiting | `RECRUITING-0205` | `RECRUITING_FORM_SECTION_POLICY_INVALID_TRACK` | 400 BAD_REQUEST | 지원 폼 섹션의 모집 트랙이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:47` |
| 409 | recruiting | `RECRUITING-0206` | `RECRUITING_APPLICATION_FORM_TRACK_SECTION_REQUIRED` | 400 BAD_REQUEST | 모든 모집 트랙에 해당하는 지원 폼 섹션이 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:48` |
| 410 | recruiting | `RECRUITING-0300` | `RECRUITING_APPLICATION_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 지원서 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:49` |
| 411 | recruiting | `RECRUITING-0301` | `RECRUITING_APPLICATION_ALREADY_EXISTS` | 409 CONFLICT | 이미 같은 모집 차수에 제출한 지원서가 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:50` |
| 412 | recruiting | `RECRUITING-0302` | `RECRUITING_APPLICATION_DIFFERENT_SCHOOL_EXISTS` | 409 CONFLICT | 같은 기수의 다른 학교 모집에 이미 지원했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:51` |
| 413 | recruiting | `RECRUITING-0303` | `RECRUITING_APPLICATION_REAPPLICATION_BLOCKED` | 409 CONFLICT | 진행 중이거나 합격한 지원서가 있어 재지원할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:52` |
| 414 | recruiting | `RECRUITING-0304` | `RECRUITING_APPLICATION_FINAL_PASS_ALREADY_EXISTS` | 409 CONFLICT | 이미 최종 합격한 지원서가 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:53` |
| 415 | recruiting | `RECRUITING-0305` | `RECRUITING_REGISTRATION_FORBIDDEN` | 403 FORBIDDEN | 중앙운영사무국 총괄단 이상만 최종 등록을 확정할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:54` |
| 416 | recruiting | `RECRUITING-0306` | `RECRUITING_APPLICATION_MEMBER_REQUIRED` | 400 BAD_REQUEST | 챌린저 등록에는 연결된 회원 정보가 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:55` |
| 417 | recruiting | `RECRUITING-0307` | `RECRUITING_APPLICATION_REQUIRED_FIELD` | 400 BAD_REQUEST | 지원서 필수 정보가 누락되었어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:56` |
| 418 | recruiting | `RECRUITING-0308` | `RECRUITING_APPLICATION_INVALID_APPLICANT_NAME` | 400 BAD_REQUEST | 지원자 이름에는 공백을 사용할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:57` |
| 419 | recruiting | `RECRUITING-0309` | `RECRUITING_APPLICATION_INVALID_EMAIL` | 400 BAD_REQUEST | 지원자 이메일이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:58` |
| 420 | recruiting | `RECRUITING-0310` | `RECRUITING_APPLICATION_INVALID_FIRST_CHOICE` | 400 BAD_REQUEST | 1지망은 현재 모집 중인 트랙이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:59` |
| 421 | recruiting | `RECRUITING-0311` | `RECRUITING_APPLICATION_INVALID_SECOND_CHOICE` | 400 BAD_REQUEST | 2지망 선택이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:60` |
| 422 | recruiting | `RECRUITING-0312` | `RECRUITING_APPLICATION_INVALID_ACCEPTED_TRACK` | 400 BAD_REQUEST | 합격 트랙은 지원한 트랙 중 하나여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:61` |
| 423 | recruiting | `RECRUITING-0313` | `RECRUITING_APPLICATION_INVALID_PRIVACY_CONSENT` | 400 BAD_REQUEST | 개인정보 동의 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:62` |
| 424 | recruiting | `RECRUITING-0314` | `RECRUITING_APPLICATION_INVALID_KEY` | 400 BAD_REQUEST | 지원 키 형식이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:63` |
| 425 | recruiting | `RECRUITING-0315` | `RECRUITING_APPLICATION_KEY_ISSUE_FAILED` | 409 CONFLICT | 지원 키를 발급하지 못했어요. 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:64` |
| 426 | recruiting | `RECRUITING-0316` | `RECRUITING_APPLICATION_APPLICANT_MISMATCH` | 403 FORBIDDEN | 본인의 지원서만 변경할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:65` |
| 427 | recruiting | `RECRUITING-0317` | `RECRUITING_FINAL_DECISION_FORBIDDEN` | 403 FORBIDDEN | 학교 회장단 또는 중앙 총괄단만 최종 판정할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:66` |
| 428 | recruiting | `RECRUITING-0318` | `RECRUITING_QUOTA_NOT_FOUND` | 404 NOT_FOUND | 합격 트랙의 모집 정원을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:67` |
| 429 | recruiting | `RECRUITING-0319` | `RECRUITING_QUOTA_EXCEEDED` | 409 CONFLICT | 합격 트랙의 모집 정원이 모두 예약되었어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:68` |
| 430 | recruiting | `RECRUITING-0320` | `RECRUITING_QUOTA_BELOW_RESERVED` | 409 CONFLICT | 모집 정원을 현재 예약 및 등록 인원보다 작게 줄일 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:69` |
| 431 | recruiting | `RECRUITING-0321` | `RECRUITING_APPLICATION_PERIOD_CLOSED` | 400 BAD_REQUEST | 현재 지원서를 작성하거나 제출할 수 있는 기간이 아니에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:70` |
| 432 | recruiting | `RECRUITING-0322` | `RECRUITING_CONCURRENCY_LOCK_TIMEOUT` | 409 CONFLICT | 동시 요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:71` |
| 433 | recruiting | `RECRUITING-0323` | `RECRUITING_SUMMARY_ACCESS_DENIED` | 403 FORBIDDEN | 해당 기수의 지원 현황을 조회할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:72` |
| 434 | recruiting | `RECRUITING-0324` | `RECRUITING_APPLICATION_ANSWER_OUT_OF_SCOPE` | 400 BAD_REQUEST | 선택한 지원 트랙에 포함되지 않은 문항에는 응답할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:73` |
| 435 | recruiting | `RECRUITING-0401` | `RECRUITING_EVALUATION_ALREADY_SUBMITTED` | 409 CONFLICT | 이미 확정한 지원자 평가가 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:74` |
| 436 | recruiting | `RECRUITING-0402` | `RECRUITING_EVALUATION_ACCESS_DENIED` | 403 FORBIDDEN | 지원자 평가를 조회할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:75` |
| 437 | recruiting | `RECRUITING-0403` | `RECRUITING_EVALUATION_INVALID` | 400 BAD_REQUEST | 지원자 평가 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:76` |
| 438 | recruiting | `RECRUITING-0404` | `RECRUITING_EVALUATION_DECISION_REQUIRED` | 400 BAD_REQUEST | 평가 확정에는 결정이 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:77` |
| 439 | recruiting | `RECRUITING-0405` | `RECRUITING_EVALUATION_COMMENT_TOO_LONG` | 400 BAD_REQUEST | 평가 의견은 2000자 이하여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:78` |
| 440 | recruiting | `RECRUITING-0410` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID` | 400 BAD_REQUEST | 면접 일정 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:79` |
| 441 | recruiting | `RECRUITING-0411` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 면접 일정 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:80` |
| 442 | recruiting | `RECRUITING-0412` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE` | 400 BAD_REQUEST | 면접 가능 시간 응답 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:81` |
| 443 | recruiting | `RECRUITING-0413` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD` | 400 BAD_REQUEST | 면접 일정 시간이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:82` |
| 444 | recruiting | `RECRUITING-0414` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_LOCATION` | 400 BAD_REQUEST | 면접 장소가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:83` |
| 445 | recruiting | `RECRUITING-0415` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_CONTACT` | 400 BAD_REQUEST | 면접 연락처가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:84` |
| 446 | recruiting | `RECRUITING-0416` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_MAIL_STATE` | 400 BAD_REQUEST | 면접 안내 메일 상태가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:85` |
| 447 | recruiting | `RECRUITING-0417` | `RECRUITING_INTERVIEW_SCHEDULE_ALREADY_EXISTS` | 409 CONFLICT | 이미 면접 일정이 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:86` |
| 448 | recruiting | `RECRUITING-0418` | `RECRUITING_INTERVIEW_SCHEDULE_ACCESS_DENIED` | 403 FORBIDDEN | 면접 일정을 조회할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:87` |
| 449 | recruiting | `RECRUITING-0419` | `RECRUITING_INTERVIEW_AVAILABILITY_NOT_IMPLEMENTED` | 501 NOT_IMPLEMENTED | 면접 가능 일정 응답 제출 기능은 아직 사용할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:88` |
| 450 | recruiting | `RECRUITING-0500` | `RECRUITING_ROUND_EVALUATOR_INVALID` | 400 BAD_REQUEST | 모집 차수 평가자 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:89` |
| 451 | recruiting | `RECRUITING-0501` | `RECRUITING_ROUND_EVALUATOR_ALREADY_EXISTS` | 409 CONFLICT | 이미 해당 모집 차수의 평가자로 등록되어 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:90` |
| 452 | recruiting | `RECRUITING-0502` | `RECRUITING_INTERVIEW_QUESTION_INVALID_TARGET` | 400 BAD_REQUEST | 면접 질문 대상이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:91` |
| 453 | recruiting | `RECRUITING-0503` | `RECRUITING_INTERVIEW_QUESTION_INVALID_CONTENT` | 400 BAD_REQUEST | 면접 질문 내용을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:92` |
| 454 | recruiting | `RECRUITING-0504` | `RECRUITING_INTERVIEW_QUESTION_INVALID_ORDER_NO` | 400 BAD_REQUEST | 면접 질문 순서는 0 이상이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:93` |
| 455 | recruiting | `RECRUITING-0505` | `RECRUITING_INTERVIEW_QUESTION_IMMUTABLE` | 409 CONFLICT | 면접 평가 제출 후에는 질문을 변경할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:94` |
| 456 | recruiting | `RECRUITING-0506` | `RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED` | 403 FORBIDDEN | 면접 질문을 변경할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:95` |
| 457 | recruiting | `RECRUITING-0507` | `RECRUITING_INTERVIEW_QUESTION_INVALID_ACTOR` | 400 BAD_REQUEST | 면접 질문 변경자 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:96` |

## schedule

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 458 | schedule | `SCHEDULE-0006` | `INVALID_TIME_RANGE` | 400 BAD_REQUEST | 시작 시간은 종료 시간보다 빨라야 해요. 시간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:14` |
| 459 | schedule | `SCHEDULE-0009` | `SCHEDULE_NOT_FOUND` | 404 NOT_FOUND | 일정을 찾을 수 없어요. 선택한 일정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:16` |
| 460 | schedule | `SCHEDULE-0010` | `TAG_REQUIRED` | 400 BAD_REQUEST | 태그를 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:18` |
| 461 | schedule | `SCHEDULE-0011` | `NOT_FIRST_ATTENDANCE_REQUEST` | 400 BAD_REQUEST | 이미 출석 요청이 있어요. 기존 요청을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:20` |
| 462 | schedule | `SCHEDULE-0012` | `NO_ATTENDANCE_RECORD` | 404 NOT_FOUND | 출석 요청이 없어요. 출석 요청을 먼저 생성해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:22` |
| 463 | schedule | `SCHEDULE-0013` | `INVALID_ATTENDANCE_STATUS_FOR_EXCUSE` | 400 BAD_REQUEST | 첫 요청, 결석 또는 지각 상태에서만 출석 사유를 제출할 수 있어요. 출석 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:24` |
| 464 | schedule | `SCHEDULE-0014` | `INVALID_ATTENDANCE_STATUS_FOR_APPROVAL` | 400 BAD_REQUEST | 현재 출석 상태에서는 승인할 수 없어요. 출석 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:27` |
| 465 | schedule | `SCHEDULE-0015` | `INVALID_ATTENDANCE_STATUS_FOR_REJECT` | 400 BAD_REQUEST | 현재 출석 상태에서는 거절할 수 없어요. 출석 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:29` |
| 466 | schedule | `SCHEDULE-0016` | `NO_EXCUSE_REASON_GIVEN` | 400 BAD_REQUEST | 출석 인정을 요청하려면 사유를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:31` |
| 467 | schedule | `SCHEDULE-0017` | `ATTENDANCE_NOT_REQUIRES_CONFIRM` | 400 BAD_REQUEST | 운영진 확인이 필요한 출석 요청이 아니에요. 출석 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:33` |
| 468 | schedule | `SCHEDULE-0018` | `SCHEDULE_ENDED` | 400 BAD_REQUEST | 종료된 일정에는 출석을 요청할 수 없어요. 일정 시간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:36` |
| 469 | schedule | `SCHEDULE-0019` | `CHECK_IN_TOO_EARLY` | 400 BAD_REQUEST | 아직 출석할 수 있는 시간이 아니에요. 출석 가능 시간 이후에 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:38` |
| 470 | schedule | `SCHEDULE-0020` | `OFFLINE_SCHEDULE_REQUIRES_LOCATION` | 400 BAD_REQUEST | 대면 일정에는 위치 정보가 필요해요. 위치를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:40` |
| 471 | schedule | `SCHEDULE-0021` | `SCHEDULE_ATTENDANCE_POLICY_NOT_EXIST` | 400 BAD_REQUEST | 출석 정책이 없는 일정이에요. 출석 정책을 먼저 설정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:42` |
| 472 | schedule | `SCHEDULE-0022` | `PARTICIPANT_NOT_FOUND` | 400 BAD_REQUEST | 일정 참석자 정보를 찾을 수 없어요. 참석자 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:44` |
| 473 | schedule | `SCHEDULE-0023` | `LOCATION_NOT_VERIFIED` | 400 BAD_REQUEST | 출석 인증 범위 안에 있는지 확인하지 못했어요. 위치를 확인한 뒤 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:46` |
| 474 | schedule | `SCHEDULE-0024` | `ONLINE_SCHEDULE_SHOULD_NOT_HAVE_LOCATION` | 400 BAD_REQUEST | 비대면 일정에는 위치 정보를 포함할 수 없어요. 위치 정보를 제거해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:48` |
| 475 | schedule | `SCHEDULE-0025` | `NOT_ACTIVE_GISU_SCHEDULE` | 400 BAD_REQUEST | 현재 기수의 일정만 만들 수 있어요. 기수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:51` |
| 476 | schedule | `SCHEDULE-0026` | `NOT_SCHEDULE_PARTICIPANT` | 400 BAD_REQUEST | 일정 참여자만 출석할 수 있어요. 참여자 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:54` |
| 477 | schedule | `SCHEDULE-0027` | `ATTENDANCE_POLICY_REQUIRED` | 400 BAD_REQUEST | 출석이 필요한 일정에는 출석 정책을 설정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:56` |
| 478 | schedule | `SCHEDULE-0028` | `STARTED_SCHEDULE_CANT_BE_EDITED` | 400 BAD_REQUEST | 이미 시작된 일정은 수정할 수 없어요. 일정 시간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:58` |
| 479 | schedule | `SCHEDULE-0029` | `CANNOT_CREATE_SCHEDULE` | 403 FORBIDDEN | 일정을 만들려면 챌린저 활동 이력이 필요해요. 활동 기록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:60` |
| 480 | schedule | `SCHEDULE-0030` | `EXCEEDED_MAX_PARTICIPANTS` | 400 BAD_REQUEST | 초대 가능한 참여자 수를 초과했어요. 참여자를 줄여주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:62` |
| 481 | schedule | `SCHEDULE-0031` | `CANNOT_CREATE_ATTENDANCE_REQUIRED_SCHEDULE` | 403 FORBIDDEN | 출석이 필요한 일정을 만들 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:64` |
| 482 | schedule | `SCHEDULE-0032` | `INVALID_MEMBER_INVITE` | 400 BAD_REQUEST | 초대할 수 없는 참여자가 포함되어 있어요. 참여자 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:67` |
| 483 | schedule | `SCHEDULE-0033` | `SCHEDULE_HAS_ATTENDANCE_RECORD` | 400 BAD_REQUEST | 출석 기록이 있는 일정은 삭제할 수 없어요. 출석 기록을 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:69` |

## storage

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 484 | storage | `STORAGE-0001` | `FILE_NOT_FOUND` | 404 NOT_FOUND | 파일을 찾을 수 없어요. 선택한 파일을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:15` |
| 485 | storage | `STORAGE-0002` | `FILE_UPLOAD_NOT_COMPLETED` | 400 BAD_REQUEST | 파일 업로드가 아직 끝나지 않았어요. 업로드를 완료한 뒤 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:16` |
| 486 | storage | `STORAGE-0003` | `FILE_ALREADY_UPLOADED` | 400 BAD_REQUEST | 이미 업로드가 끝난 파일이에요. 파일 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:17` |
| 487 | storage | `STORAGE-0004` | `INVALID_FILE_EXTENSION` | 400 BAD_REQUEST | 지원하지 않는 파일 형식이에요. 다른 파일을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:23` |
| 488 | storage | `STORAGE-0005` | `FILE_SIZE_EXCEEDED` | 400 BAD_REQUEST | 파일 크기가 너무 커요. 더 작은 파일을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:24` |
| 489 | storage | `STORAGE-0006` | `INVALID_CONTENT_TYPE` | 400 BAD_REQUEST | 파일 형식 정보가 올바르지 않아요. 파일을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:25` |
| 490 | storage | `STORAGE-0007` | `STORAGE_UPLOAD_FAILED` | 500 INTERNAL_SERVER_ERROR | 파일을 업로드하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:29` |
| 491 | storage | `STORAGE-0008` | `STORAGE_DELETE_FAILED` | 500 INTERNAL_SERVER_ERROR | 파일을 삭제하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:30` |
| 492 | storage | `STORAGE-0009` | `STORAGE_URL_GENERATION_FAILED` | 500 INTERNAL_SERVER_ERROR | 파일 접근 링크를 만들지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:31` |
| 493 | storage | `STORAGE-0010` | `CDN_SIGNING_FAILED` | 500 INTERNAL_SERVER_ERROR | CDN 접근 링크를 만들지 못했어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:36` |
| 494 | storage | `STORAGE-0011` | `NO_ENV_KEYS` | 500 INTERNAL_SERVER_ERROR | CDN 설정이 누락됐어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:37` |
| 495 | storage | `STORAGE-0012` | `INVALID_SPRING_PROFILE` | 500 INTERNAL_SERVER_ERROR | 서버 실행 환경이 올바르지 않아요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:38` |
| 496 | storage | `STORAGE-0013` | `FILE_DELETE_FORBIDDEN` | 403 FORBIDDEN | 파일을 삭제할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:18` |
| 497 | storage | `STORAGE-0014` | `FILE_SIZE_MISMATCH` | 400 BAD_REQUEST | 요청한 파일 크기와 실제 업로드된 파일 크기가 달라요. 다시 업로드해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:26` |
| 498 | storage | `STORAGE-0015` | `STORAGE_METADATA_READ_FAILED` | 500 INTERNAL_SERVER_ERROR | 파일 정보를 확인하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:32` |
| 499 | storage | `STORAGE-0016` | `FILE_USE_FORBIDDEN` | 403 FORBIDDEN | 이 파일을 사용할 권한이 없어요. 본인이 업로드한 파일만 사용할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:20` |

## term

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 500 | term | `TERMS-0001` | `TERMS_NOT_FOUND` | 404 NOT_FOUND | 약관을 찾을 수 없어요. 선택한 약관을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:14` |
| 501 | term | `TERMS-0002` | `TERMS_TYPE_REQUIRED` | 400 BAD_REQUEST | 약관 타입을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:15` |
| 502 | term | `TERMS-0003` | `TERMS_TITLE_REQUIRED` | 400 BAD_REQUEST | 약관 제목을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:16` |
| 503 | term | `TERMS-0004` | `TERMS_CONTENT_REQUIRED` | 400 BAD_REQUEST | 약관 내용을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:17` |
| 504 | term | `TERMS-0005` | `TERMS_VERSION_REQUIRED` | 400 BAD_REQUEST | 약관 버전을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:18` |
| 505 | term | `TERMS-0006` | `TERMS_CONSENT_NOT_FOUND` | 404 NOT_FOUND | 약관 동의 정보를 찾을 수 없어요. 동의 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:20` |
| 506 | term | `TERMS-0007` | `TERMS_CONSENT_ALREADY_EXISTS` | 400 BAD_REQUEST | 이미 동의한 약관이에요. 동의 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:21` |
| 507 | term | `TERMS-0008` | `MEMBER_ID_REQUIRED` | 400 BAD_REQUEST | 회원을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:22` |
| 508 | term | `TERMS-0009` | `TERM_ID_REQUIRED` | 400 BAD_REQUEST | 약관을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:23` |
| 509 | term | `TERMS-0010` | `MANDATORY_TERMS_NOT_AGREED` | 400 BAD_REQUEST | 필수 약관에 모두 동의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:24` |
| 510 | term | `TERMS-0011` | `TERM_PERMISSION_DENIED` | 403 FORBIDDEN | 약관을 관리할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:26` |

