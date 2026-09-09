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

## blog

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 55 | blog | `BLOG-0001` | `CONTENT_NOT_FOUND` | 404 NOT_FOUND | 글을 찾지 못했어요. 주소를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:13` |
| 56 | blog | `BLOG-0002` | `COMMENT_NOT_FOUND` | 404 NOT_FOUND | 댓글을 찾지 못했어요. 새로고침 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:14` |
| 57 | blog | `BLOG-0003` | `INVALID_CONTENT_TYPE` | 400 BAD_REQUEST | 카테고리를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:15` |
| 58 | blog | `BLOG-0004` | `INVALID_SLUG` | 400 BAD_REQUEST | 주소는 영문 소문자, 숫자, 하이픈만 사용할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:16` |
| 59 | blog | `BLOG-0005` | `INVALID_ID` | 400 BAD_REQUEST | ID는 1 이상이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:17` |
| 60 | blog | `BLOG-0006` | `INVALID_COMMENT_CONTENT` | 400 BAD_REQUEST | 댓글은 1자 이상 1,000자 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:18` |
| 61 | blog | `BLOG-0007` | `INVALID_NICKNAME` | 400 BAD_REQUEST | 닉네임은 1자 이상 20자 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:19` |
| 62 | blog | `BLOG-0008` | `INVALID_PARENT_COMMENT` | 400 BAD_REQUEST | 이 댓글에는 답글을 달 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:20` |
| 63 | blog | `BLOG-0009` | `COMMENT_ALREADY_DELETED` | 400 BAD_REQUEST | 삭제된 댓글에는 수정, 삭제, 좋아요를 할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:21` |
| 64 | blog | `BLOG-0011` | `INVALID_COMMENT_SORT` | 400 BAD_REQUEST | 댓글 정렬 기준을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:22` |
| 65 | blog | `BLOG-0012` | `INVALID_MEMBER_ID` | 400 BAD_REQUEST | 회원 정보를 확인하지 못했어요. 다시 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:23` |
| 66 | blog | `BLOG-0013` | `INVALID_COMMENT_CURSOR` | 400 BAD_REQUEST | 댓글 목록을 불러오지 못했어요. 새로고침 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:24` |
| 67 | blog | `BLOG-0014` | `INVALID_CONTENT_TITLE` | 400 BAD_REQUEST | 제목은 1자 이상 200자 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:25` |
| 68 | blog | `BLOG-0015` | `INVALID_CONTENT_SUMMARY` | 400 BAD_REQUEST | 요약은 500자 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:26` |
| 69 | blog | `BLOG-0016` | `INVALID_THUMBNAIL_URL` | 400 BAD_REQUEST | 썸네일 URL은 1,000자 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:27` |
| 70 | blog | `BLOG-0017` | `INVALID_CONTENT_BODY` | 400 BAD_REQUEST | 본문은 1자 이상 100,000자 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:28` |
| 71 | blog | `BLOG-0018` | `INVALID_CONTENT_STATUS` | 400 BAD_REQUEST | 글 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:29` |
| 72 | blog | `BLOG-0019` | `CONTENT_ALREADY_EXISTS` | 409 CONFLICT | 이미 같은 주소의 글이 있어요. slug를 바꿔주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:30` |
| 73 | blog | `BLOG-0020` | `CONTENT_NOT_PUBLISHED` | 400 BAD_REQUEST | 글을 공개한 뒤 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:31` |
| 74 | blog | `BLOG-0021` | `SERIES_NOT_FOUND` | 404 NOT_FOUND | 시리즈를 찾지 못했어요. 주소를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:32` |
| 75 | blog | `BLOG-0022` | `SERIES_ALREADY_EXISTS` | 409 CONFLICT | 이미 같은 주소의 시리즈가 있어요. slug를 바꿔주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:33` |
| 76 | blog | `BLOG-0023` | `INVALID_SERIES_TITLE` | 400 BAD_REQUEST | 시리즈 제목은 1자 이상 200자 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:34` |
| 77 | blog | `BLOG-0024` | `INVALID_SERIES_DESCRIPTION` | 400 BAD_REQUEST | 시리즈 설명은 1,000자 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:35` |
| 78 | blog | `BLOG-0025` | `INVALID_DISPLAY_ORDER` | 400 BAD_REQUEST | 표시 순서는 0 이상으로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:36` |
| 79 | blog | `BLOG-0026` | `CONTENT_TYPE_MISMATCH` | 400 BAD_REQUEST | 시리즈와 글의 카테고리를 맞춰주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:37` |
| 80 | blog | `BLOG-0027` | `HASHTAG_NOT_FOUND` | 404 NOT_FOUND | 해시태그를 찾지 못했어요. 주소를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:38` |
| 81 | blog | `BLOG-0028` | `INVALID_HASHTAG` | 400 BAD_REQUEST | 해시태그는 공백 없이 1자 이상 30자 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:39` |
| 82 | blog | `BLOG-0029` | `TOO_MANY_HASHTAGS` | 400 BAD_REQUEST | 해시태그는 10개 이하로 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:40` |
| 83 | blog | `BLOG-0030` | `INVALID_SORT` | 400 BAD_REQUEST | 정렬 기준을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:41` |
| 84 | blog | `BLOG-0031` | `INVALID_CURSOR` | 400 BAD_REQUEST | 목록을 불러오지 못했어요. 새로고침 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:42` |
| 85 | blog | `BLOG-0032` | `CONTENT_ALREADY_DELETED` | 400 BAD_REQUEST | 삭제된 글이에요. 목록에서 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:43` |
| 86 | blog | `BLOG-0033` | `SERIES_ALREADY_DELETED` | 400 BAD_REQUEST | 삭제된 시리즈예요. 목록에서 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/blog/domain/BlogErrorCode.java:44` |

## certificate

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 87 | certificate | `CERTIFICATE-0001` | `CERTIFICATE_NOT_FOUND` | 404 NOT_FOUND | 인증서를 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:13` |
| 88 | certificate | `CERTIFICATE-0002` | `CERTIFICATE_ACCESS_FORBIDDEN` | 403 FORBIDDEN | 인증서에 접근할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:14` |
| 89 | certificate | `CERTIFICATE-0003` | `CERTIFICATE_ISSUE_FORBIDDEN` | 403 FORBIDDEN | 인증서를 발급할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:15` |
| 90 | certificate | `CERTIFICATE-0004` | `CERTIFICATE_SELF_ISSUE_FORBIDDEN` | 400 BAD_REQUEST | 직접 발급할 수 없는 인증서 종류예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:16` |
| 91 | certificate | `CERTIFICATE-0005` | `CERTIFICATE_ELIGIBILITY_NOT_MET` | 400 BAD_REQUEST | 인증서 발급 조건을 만족하지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:17` |
| 92 | certificate | `CERTIFICATE-0006` | `CERTIFICATE_ALREADY_REVOKED` | 400 BAD_REQUEST | 이미 폐기된 인증서예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:18` |
| 93 | certificate | `CERTIFICATE-0007` | `CERTIFICATE_EXPIRED_OR_REVOKED` | 400 BAD_REQUEST | 만료되었거나 폐기된 인증서예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:19` |
| 94 | certificate | `CERTIFICATE-0008` | `CERTIFICATE_SERIAL_GENERATION_FAILED` | 500 INTERNAL_SERVER_ERROR | 인증서 일련번호를 만들지 못했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:20` |
| 95 | certificate | `CERTIFICATE-0009` | `CERTIFICATE_RENDER_FAILED` | 500 INTERNAL_SERVER_ERROR | 인증서 PDF를 만들지 못했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/certificate/domain/exception/CertificateErrorCode.java:21` |

## challenger

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 96 | challenger | `CHALLENGER-0001` | `CHALLENGER_NOT_FOUND` | 404 NOT_FOUND | 챌린저를 찾을 수 없어요. 선택한 챌린저를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:14` |
| 97 | challenger | `CHALLENGER-0002` | `CHALLENGER_ALREADY_EXISTS` | 409 CONFLICT | 이미 등록된 챌린저예요. 기존 기록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:15` |
| 98 | challenger | `CHALLENGER-0003` | `CHALLENGER_ALREADY_WITHDRAWN` | 400 BAD_REQUEST | 이미 탈퇴한 챌린저예요. 다른 챌린저를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:16` |
| 99 | challenger | `CHALLENGER-0004` | `INVALID_CHALLENGER_STATUS` | 400 BAD_REQUEST | 챌린저 상태가 올바르지 않아요. 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:17` |
| 100 | challenger | `CHALLENGER-0005` | `CHALLENGER_NOT_ACTIVE` | 400 BAD_REQUEST | 활동 중인 챌린저만 사용할 수 있어요. 챌린저 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:18` |
| 101 | challenger | `CHALLENGER-0007` | `CHALLENGER_POINT_NOT_FOUND` | 404 NOT_FOUND | 상벌점 기록을 찾을 수 없어요. 선택한 기록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:19` |
| 102 | challenger | `CHALLENGER-0008` | `BAD_CHALLENGER_UPDATE_REQUEST` | 404 NOT_FOUND | 챌린저 수정 요청이 올바르지 않아요. 입력값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:20` |
| 103 | challenger | `CHALLENGER-0009` | `NOT_ALLOWED_AUTHOR` | 400 BAD_REQUEST | 일정을 만들려면 챌린저 상태가 활동 중이거나 수료여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:21` |
| 104 | challenger | `CHALLENGER-0010` | `MEMBER_PROFILE_NOT_FOUND` | 404 NOT_FOUND | 연결된 멤버 프로필을 찾을 수 없어요. 회원 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:22` |
| 105 | challenger | `CHALLENGER-0011` | `INVALID_CURSOR_ID` | 400 BAD_REQUEST | 커서 값이 올바르지 않아요. 목록을 처음부터 다시 조회해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:23` |
| 106 | challenger | `CHALLENGER-0012` | `USED_CHALLENGER_RECORD_CODE` | 400 BAD_REQUEST | 이미 사용한 챌린저 기록 추가 코드예요. 새 코드를 발급받아주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:24` |
| 107 | challenger | `CHALLENGER-0013` | `INVALID_MEMBER_NAME_FOR_RECORD` | 400 BAD_REQUEST | 코드에 등록된 이름이 내 정보와 일치하지 않아요. 입력한 코드를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:25` |
| 108 | challenger | `CHALLENGER-0014` | `INVALID_SCHOOL_FOR_RECORD` | 400 BAD_REQUEST | 코드에 등록된 학교가 내 소속과 일치하지 않아요. 소속 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:26` |
| 109 | challenger | `CHALLENGER-0015` | `INVALID_CHALLENGER_RECORD_CREATE_REQUEST` | 400 BAD_REQUEST | 입력한 정보로 챌린저 기록을 만들 수 없어요. 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:27` |
| 110 | challenger | `CHALLENGER-0016` | `NO_CHALLENGER_IN_MEMBER_GISU` | 404 NOT_FOUND | 해당 기수의 챌린저 기록을 찾을 수 없어요. 기수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:28` |
| 111 | challenger | `CHALLENGER-0017` | `CHALLENGER_PART_NOT_FOUND` | 404 NOT_FOUND | 챌린저 파트를 찾을 수 없어요. 파트 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/challenger/domain/exception/ChallengerErrorCode.java:29` |

## chat

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 112 | chat | `CHAT-0001` | `CHAT_ROOM_NOT_FOUND` | 404 NOT_FOUND | 채팅방을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:14` |
| 113 | chat | `CHAT-0002` | `CHAT_MEMBER_ALREADY_EXISTS` | 409 CONFLICT | 이미 채팅방에 참여 중인 멤버입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:15` |
| 114 | chat | `CHAT-0003` | `CHAT_MEMBER_NOT_FOUND` | 404 NOT_FOUND | 채팅방 멤버를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:16` |
| 115 | chat | `CHAT-0004` | `CHAT_MESSAGE_NOT_FOUND` | 404 NOT_FOUND | 채팅 메시지를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:17` |
| 116 | chat | `CHAT-0005` | `CHAT_MESSAGE_INVALID_CONTENT_TYPE` | 400 BAD_REQUEST | 허용되지 않는 메시지 콘텐츠 타입입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:18` |
| 117 | chat | `CHAT-0006` | `CHAT_MESSAGE_EMPTY` | 400 BAD_REQUEST | 메시지 내용 또는 첨부가 필요합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:19` |
| 118 | chat | `CHAT-0007` | `CHAT_ROOM_ACCESS_DENIED` | 403 FORBIDDEN | 해당 채팅방에 접근할 권한이 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:20` |
| 119 | chat | `CHAT-0008` | `CHAT_MESSAGE_INVALID_PAGE_SIZE` | 400 BAD_REQUEST | 허용되지 않는 페이지 크기입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:21` |
| 120 | chat | `CHAT-0009` | `CHAT_MESSAGE_INVALID_REPLY_TARGET` | 400 BAD_REQUEST | 답장할 메시지를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:22` |
| 121 | chat | `CHAT-0010` | `CHAT_MESSAGE_ATTACHMENT_REQUIRED` | 400 BAD_REQUEST | 이미지 또는 파일 메시지에는 첨부파일이 필요합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:23` |
| 122 | chat | `CHAT-0011` | `CHAT_MESSAGE_ATTACHMENT_NOT_ALLOWED` | 400 BAD_REQUEST | 텍스트 메시지에는 파일을 첨부할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:24` |
| 123 | chat | `CHAT-0012` | `CHAT_MESSAGE_INVALID_FILE_TYPE` | 400 BAD_REQUEST | 메시지 타입에 허용되지 않는 파일 형식입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:25` |
| 124 | chat | `CHAT-0013` | `CHAT_MESSAGE_INVALID_ATTACHMENT` | 400 BAD_REQUEST | 첨부파일 정보가 올바르지 않습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:26` |
| 125 | chat | `CHAT-0014` | `CHAT_MESSAGE_IDEMPOTENCY_CONFLICT` | 409 CONFLICT | 같은 메시지 식별자가 다른 내용에 사용되었습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:27` |
| 126 | chat | `CHAT-0015` | `CHAT_MESSAGE_MUTATION_FORBIDDEN` | 403 FORBIDDEN | 해당 메시지를 변경할 권한이 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:28` |
| 127 | chat | `CHAT-0016` | `CHAT_MESSAGE_INVALID_CONTENT_LENGTH` | 400 BAD_REQUEST | 메시지 내용 길이가 허용 범위를 벗어났습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:29` |
| 128 | chat | `CHAT-0017` | `CHAT_MESSAGE_INVALID_MENTION` | 400 BAD_REQUEST | 멘션 대상이 채팅방 멤버가 아닙니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:30` |
| 129 | chat | `CHAT-0018` | `CHAT_MESSAGE_INVALID_REACTION` | 400 BAD_REQUEST | 리액션은 하나의 이모지여야 합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:31` |
| 130 | chat | `CHAT-0019` | `CHAT_MESSAGE_REACTION_NOT_ALLOWED` | 409 CONFLICT | 삭제되었거나 시스템 메시지에는 리액션할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:32` |
| 131 | chat | `CHAT-0020` | `CHAT_MESSAGE_INVALID_ATTACHMENT_COUNT` | 400 BAD_REQUEST | 이미지는 1개 이상 4개 이하만 첨부할 수 있습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:33` |
| 132 | chat | `CHAT-0021` | `CHAT_MESSAGE_ATTACHMENT_TOO_LARGE` | 400 BAD_REQUEST | 첨부파일 크기가 허용 범위를 초과했습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:34` |
| 133 | chat | `CHAT-0022` | `CHAT_MESSAGE_CLIENT_ID_REQUIRED` | 400 BAD_REQUEST | 클라이언트 메시지 식별자가 필요합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/chat/domain/exception/ChatErrorCode.java:35` |

## community

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 134 | community | `COMMUNITY-0001` | `POST_NOT_FOUND` | 404 NOT_FOUND | 게시글을 찾을 수 없어요. 목록을 새로고침해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:14` |
| 135 | community | `COMMUNITY-0002` | `COMMENT_NOT_FOUND` | 404 NOT_FOUND | 댓글을 찾을 수 없어요. 목록을 새로고침해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:15` |
| 136 | community | `COMMUNITY-0004` | `INVALID_POST_TITLE` | 400 BAD_REQUEST | 게시글 제목이 올바르지 않아요. 제목을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:17` |
| 137 | community | `COMMUNITY-0005` | `INVALID_POST_CONTENT` | 400 BAD_REQUEST | 게시글 내용이 올바르지 않아요. 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:18` |
| 138 | community | `COMMUNITY-0006` | `INVALID_POST_CATEGORY` | 400 BAD_REQUEST | 게시글 카테고리가 올바르지 않아요. 카테고리를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:19` |
| 139 | community | `COMMUNITY-0007` | `INVALID_POST_REGION` | 400 BAD_REQUEST | 게시글 지역이 올바르지 않아요. 지역을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:20` |
| 140 | community | `COMMUNITY-0008` | `CANNOT_CHANGE_TO_LIGHTNING` | 400 BAD_REQUEST | 번개글은 번개글 작성 화면에서 만들어주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:21` |
| 141 | community | `COMMUNITY-0009` | `CANNOT_CHANGE_FROM_LIGHTNING` | 400 BAD_REQUEST | 번개글은 일반 게시글로 바꿀 수 없어요. 새 게시글로 작성해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:22` |
| 142 | community | `COMMUNITY-0010` | `INVALID_COMMENT_CONTENT` | 400 BAD_REQUEST | 댓글 내용이 올바르지 않아요. 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:24` |
| 143 | community | `COMMUNITY-0011` | `COMMENT_NOT_OWNED` | 403 FORBIDDEN | 내가 작성한 댓글만 삭제할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:25` |
| 144 | community | `COMMUNITY-0016` | `REPORT_ALREADY_EXISTS` | 409 CONFLICT | 이미 신고한 게시글, 댓글 또는 스레드 메시지예요. 신고 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:27` |
| 145 | community | `COMMUNITY-0017` | `INVALID_POST_AUTHOR` | 400 BAD_REQUEST | 작성자 정보가 필요해요. 로그인 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:30` |
| 146 | community | `COMMUNITY-0018` | `NOT_LIGHTNING_POST` | 400 BAD_REQUEST | 번개글이 아니에요. 일반 게시글 화면에서 수정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:31` |
| 147 | community | `COMMUNITY-0019` | `USE_LIGHTNING_API` | 400 BAD_REQUEST | 번개글은 번개글 화면에서 작성해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:32` |
| 148 | community | `COMMUNITY-0020` | `LIGHTNING_INFO_REQUIRED` | 400 BAD_REQUEST | 번개글을 작성하려면 모임 정보를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:33` |
| 149 | community | `COMMUNITY-0021` | `POST_NOT_OWNED` | 403 FORBIDDEN | 내가 작성한 게시글만 수정하거나 삭제할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:34` |
| 150 | community | `COMMUNITY-0022` | `INVALID_LIGHTNING_MEET_AT` | 400 BAD_REQUEST | 모임 시간을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:37` |
| 151 | community | `COMMUNITY-0023` | `INVALID_LIGHTNING_LOCATION` | 400 BAD_REQUEST | 모임 장소를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:38` |
| 152 | community | `COMMUNITY-0024` | `INVALID_LIGHTNING_MAX_PARTICIPANTS` | 400 BAD_REQUEST | 최대 참가자는 1명 이상으로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:39` |
| 153 | community | `COMMUNITY-0025` | `INVALID_LIGHTNING_OPEN_CHAT_URL` | 400 BAD_REQUEST | 오픈 채팅 링크를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:40` |
| 154 | community | `COMMUNITY-0026` | `INVALID_LIGHTNING_OPEN_CHAT_URL_FORMAT` | 400 BAD_REQUEST | 오픈 채팅 링크는 http:// 또는 https://로 시작해야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:41` |
| 155 | community | `COMMUNITY-0027` | `INVALID_LIGHTNING_MEET_AT_PAST` | 400 BAD_REQUEST | 모임 시간은 현재 이후로 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:42` |
| 156 | community | `COMMUNITY-0028` | `INVALID_COMMENT_POST_ID` | 400 BAD_REQUEST | 댓글을 작성할 게시글을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:45` |
| 157 | community | `COMMUNITY-0029` | `INVALID_COMMENT_CHALLENGER_ID` | 400 BAD_REQUEST | 댓글 작성자 챌린저 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:46` |
| 158 | community | `COMMUNITY-0030` | `INVALID_ID` | 400 BAD_REQUEST | ID는 1 이상의 숫자로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:49` |
| 159 | community | `COMMUNITY-0031` | `POST_SAVE_REQUIRES_AUTHOR` | 400 BAD_REQUEST | 새 게시글을 만들려면 작성자 정보가 필요해요. 로그인 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:52` |
| 160 | community | `COMMUNITY-0032` | `POST_UPDATE_INVALID_CALL` | 400 BAD_REQUEST | 게시글 수정 요청이 올바르지 않아요. 요청 방식을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:53` |
| 161 | community | `COMMUNITY-0033` | `THREAD_NOT_FOUND` | 404 NOT_FOUND | 커뮤니티 스레드를 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:55` |
| 162 | community | `COMMUNITY-0034` | `THREAD_ACCESS_DENIED` | 403 FORBIDDEN | 커뮤니티 스레드에 접근할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:56` |
| 163 | community | `COMMUNITY-0035` | `THREAD_MEMBER_NOT_FOUND` | 404 NOT_FOUND | 커뮤니티 스레드 멤버를 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:57` |
| 164 | community | `COMMUNITY-0036` | `THREAD_CAPACITY_EXCEEDED` | 409 CONFLICT | 커뮤니티 스레드의 최대 인원을 초과했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:58` |
| 165 | community | `COMMUNITY-0037` | `THREAD_DELETED` | 410 GONE | 삭제된 커뮤니티 스레드예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:59` |
| 166 | community | `COMMUNITY-0038` | `THREAD_MEMBER_ALREADY_ACTIVE` | 409 CONFLICT | 이미 커뮤니티 스레드에 참여 중인 멤버예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:60` |
| 167 | community | `COMMUNITY-0039` | `THREAD_MEMBER_KICKED` | 403 FORBIDDEN | 강퇴된 멤버는 커뮤니티 스레드에 다시 초대할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:61` |
| 168 | community | `COMMUNITY-0040` | `THREAD_OWNER_REQUIRED` | 403 FORBIDDEN | 커뮤니티 스레드 소유자만 수행할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:62` |
| 169 | community | `COMMUNITY-0041` | `THREAD_OWNER_CANNOT_LEAVE` | 409 CONFLICT | 소유권을 이전하기 전에는 커뮤니티 스레드를 나갈 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:63` |
| 170 | community | `COMMUNITY-0042` | `THREAD_OWNER_CANNOT_BE_KICKED` | 409 CONFLICT | 커뮤니티 스레드 소유자는 강퇴할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:64` |
| 171 | community | `COMMUNITY-0043` | `THREAD_INVALID_ROLE_CHANGE` | 400 BAD_REQUEST | 커뮤니티 스레드 역할 변경 요청이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:65` |
| 172 | community | `COMMUNITY-0044` | `THREAD_INVITEE_NOT_ELIGIBLE` | 400 BAD_REQUEST | 존재하는 활성 회원만 초대할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:66` |
| 173 | community | `COMMUNITY-0045` | `THREAD_INVALID_COMMAND` | 400 BAD_REQUEST | 커뮤니티 스레드 요청이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/community/domain/exception/CommunityErrorCode.java:67` |

## curriculum

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 174 | curriculum | `CURRICULUM-0001` | `CURRICULUM_NOT_FOUND` | 404 NOT_FOUND | 커리큘럼을 찾을 수 없어요. 선택한 커리큘럼을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:14` |
| 175 | curriculum | `CURRICULUM-0002` | `WORKBOOK_NOT_FOUND` | 404 NOT_FOUND | 워크북을 찾을 수 없어요. 선택한 워크북을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:15` |
| 176 | curriculum | `CURRICULUM-0003` | `MISSION_NOT_FOUND` | 404 NOT_FOUND | 미션을 찾을 수 없어요. 선택한 미션을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:16` |
| 177 | curriculum | `CURRICULUM-0004` | `WORKBOOK_HAS_SUBMISSIONS` | 409 CONFLICT | 제출된 워크북이 있어 삭제할 수 없어요. 제출 내역을 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:17` |
| 178 | curriculum | `CURRICULUM-0005` | `WORKBOOK_NOT_IN_CURRICULUM` | 404 NOT_FOUND | 이 커리큘럼에 포함된 워크북이 아니에요. 워크북을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:18` |
| 179 | curriculum | `CURRICULUM-0006` | `CHALLENGER_WORKBOOK_NOT_FOUND` | 404 NOT_FOUND | 챌린저 워크북을 찾을 수 없어요. 선택한 워크북을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:19` |
| 180 | curriculum | `CURRICULUM-0007` | `SUBMISSION_REQUIRED` | 400 BAD_REQUEST | 제출 내용을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:20` |
| 181 | curriculum | `CURRICULUM-0008` | `INVALID_WORKBOOK_STATUS` | 400 BAD_REQUEST | 워크북 상태가 올바르지 않아요. 상태 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:21` |
| 182 | curriculum | `CURRICULUM-0009` | `WORKBOOK_SUBMISSION_ALREADY_EXISTS` | 409 CONFLICT | 이미 해당 주차의 워크북 미션을 제출했어요. 제출 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:22` |
| 183 | curriculum | `CURRICULUM-0010` | `CURRICULUM_ALREADY_EXISTS` | 409 CONFLICT | 해당 기수와 파트의 커리큘럼이 이미 있어요. 기존 커리큘럼을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:23` |
| 184 | curriculum | `CURRICULUM-0011` | `WORKBOOK_ACCESS_DENIED` | 403 FORBIDDEN | 이 워크북에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:24` |
| 185 | curriculum | `CURRICULUM-0012` | `INVALID_WEEKLY_CURRICULUM_PERIOD` | 400 BAD_REQUEST | 주차 커리큘럼 시작일은 종료일보다 빨라야 해요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:26` |
| 186 | curriculum | `CURRICULUM-0013` | `INVALID_WORKBOOK_STATUS_TRANSITION` | 400 BAD_REQUEST | 현재 상태에서는 워크북 상태를 변경할 수 없어요. 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:27` |
| 187 | curriculum | `CURRICULUM-0014` | `WEEKLY_CURRICULUM_NOT_FOUND` | 404 NOT_FOUND | 주차별 커리큘럼을 찾을 수 없어요. 선택한 주차를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:28` |
| 188 | curriculum | `CURRICULUM-0015` | `CURRICULUM_HAS_WEEKLY_CURRICULUMS` | 409 CONFLICT | 주차별 커리큘럼이 남아 있어 삭제할 수 없어요. 주차별 커리큘럼을 먼저 정리해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:29` |
| 189 | curriculum | `CURRICULUM-0016` | `WEEKLY_CURRICULUM_HAS_WORKBOOKS` | 409 CONFLICT | 원본 워크북이 남아 있어 삭제할 수 없어요. 원본 워크북을 먼저 정리해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:30` |
| 190 | curriculum | `CURRICULUM-0017` | `WEEKLY_CURRICULUM_DATE_LOCKED` | 409 CONFLICT | 배포된 워크북이 있어 주차 기간을 수정할 수 없어요. 배포 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:31` |
| 191 | curriculum | `CURRICULUM-0018` | `WEEKLY_CURRICULUM_ALREADY_EXISTS` | 409 CONFLICT | 동일한 주차와 부록 여부의 주차별 커리큘럼이 이미 있어요. 기존 항목을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:32` |
| 192 | curriculum | `CURRICULUM-0019` | `WEEKLY_CURRICULUM_PERIOD_ALREADY_ENDED` | 400 BAD_REQUEST | 종료된 기간으로는 주차별 커리큘럼을 만들거나 수정할 수 없어요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:33` |
| 193 | curriculum | `CURRICULUM-0020` | `MISSION_HAS_SUBMISSIONS` | 409 CONFLICT | 이미 제출된 미션이 있어 삭제할 수 없어요. 제출 내역을 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:34` |
| 194 | curriculum | `CURRICULUM-0021` | `RELEASED_WORKBOOK_NECESSARY_MISSION_FORBIDDEN` | 400 BAD_REQUEST | 배포된 워크북에는 필수 미션을 추가할 수 없어요. 선택 미션으로 추가해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:35` |
| 195 | curriculum | `CURRICULUM-0022` | `RELEASED_WORKBOOK_MISSION_UPGRADE_FORBIDDEN` | 400 BAD_REQUEST | 배포된 워크북의 미션은 필수에서 선택으로만 변경할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:36` |
| 196 | curriculum | `CURRICULUM-0023` | `MISSION_SUBMISSION_NOT_FOUND` | 404 NOT_FOUND | 미션 제출물을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:37` |
| 197 | curriculum | `CURRICULUM-0024` | `MISSION_FEEDBACK_NOT_FOUND` | 404 NOT_FOUND | 미션 피드백을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:38` |
| 198 | curriculum | `CURRICULUM-0025` | `FEEDBACK_REQUIRED` | 400 BAD_REQUEST | 피드백 내용을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:39` |
| 199 | curriculum | `CURRICULUM-0026` | `FEEDBACK_RESULT_REQUIRED` | 400 BAD_REQUEST | 피드백 평가 결과를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:40` |
| 200 | curriculum | `CURRICULUM-0027` | `SUBMISSION_PERIOD_ENDED` | 409 CONFLICT | 주차별 커리큘럼 제출 기간이 종료되었어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:41` |
| 201 | curriculum | `CURRICULUM-0028` | `SUBMISSION_EDIT_PERIOD_ENDED` | 409 CONFLICT | 미션 제출물 수정 가능 기간이 종료되었어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:42` |
| 202 | curriculum | `CURRICULUM-0029` | `MISSION_SUBMISSION_ALREADY_WITHDRAWN` | 409 CONFLICT | 이미 철회된 미션 제출물이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:43` |
| 203 | curriculum | `CURRICULUM-0030` | `FEEDBACK_EDIT_PERIOD_ENDED` | 409 CONFLICT | 미션 피드백 수정 가능 기간이 종료되었어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:44` |
| 204 | curriculum | `CURRICULUM-0031` | `FEEDBACK_DELETE_PERIOD_ENDED` | 409 CONFLICT | 기수가 종료되어 미션 피드백을 삭제할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:45` |
| 205 | curriculum | `CURRICULUM-0032` | `STUDY_GROUP_NOT_MATCHED` | 409 CONFLICT | 커리큘럼과 일치하는 스터디 그룹을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:46` |
| 206 | curriculum | `CURRICULUM-0033` | `WEEKLY_BEST_ALREADY_EXISTS` | 409 CONFLICT | 해당 그룹과 주차의 베스트 워크북이 이미 선정되었어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:47` |
| 207 | curriculum | `CURRICULUM-0034` | `BEST_WORKBOOK_REQUIREMENTS_NOT_MET` | 409 CONFLICT | 베스트 워크북 선정 조건을 충족하지 못했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:48` |
| 208 | curriculum | `CURRICULUM-0035` | `BEST_WORKBOOK_REASON_REQUIRED` | 400 BAD_REQUEST | 베스트 워크북 선정 사유를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:49` |
| 209 | curriculum | `CURRICULUM-0036` | `CHALLENGER_WORKBOOK_ALREADY_EXISTS` | 409 CONFLICT | 이미 배포된 챌린저 워크북이에요. 기존 워크북을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/curriculum/domain/exception/CurriculumErrorCode.java:50` |

## demoday

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 210 | demoday | `DEMODAY-0100` | `DEMODAY_POLL_INVALID_WINDOW` | 400 BAD_REQUEST | 투표 시작 시각은 종료 시각보다 앞서야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:14` |
| 211 | demoday | `DEMODAY-0103` | `DEMODAY_POLL_OPEN_AT_REQUIRED` | 400 BAD_REQUEST | 데모데이 투표 시작 시간을 입력해주세요 |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:15` |
| 212 | demoday | `DEMODAY-0104` | `DEMODAY_POLL_CLOSE_AT_REQUIRED` | 400 BAD_REQUEST | 데모데이 투표 종료 시간을 입력해주세요 |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:16` |
| 213 | demoday | `DEMODAY-0105` | `DEMODAY_POLL_BOOTH_LOCKED` | 409 CONFLICT | 투표가 시작되어 부스를 추가할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:17` |
| 214 | demoday | `DEMODAY-0106` | `DEMODAY_POLL_INVALID_NAME` | 400 BAD_REQUEST | 데모데이 이름은 1자 이상 100자 이하여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:18` |
| 215 | demoday | `DEMODAY-0107` | `DEMODAY_POLL_GISU_REQUIRED` | 400 BAD_REQUEST | 데모데이가 진행되는 기수를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:19` |
| 216 | demoday | `DEMODAY-0108` | `DEMODAY_POLL_ALREADY_OPEN` | 409 CONFLICT | 이미 오픈된 데모데이 투표 행사 입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:20` |
| 217 | demoday | `DEMODAY-0109` | `DEMODAY_POLL_ALREADY_CLOSED` | 409 CONFLICT | 이미 종료된 데모데이 투표 행사 입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:21` |
| 218 | demoday | `DEMODAY-0110` | `DEMODAY_POLL_NOT_FOUND` | 404 NOT_FOUND | 데모데이 투표 행사를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:22` |
| 219 | demoday | `DEMODAY-0111` | `DEMODAY_POLL_NOT_OPEN` | 404 NOT_FOUND | 데모데이 투표 행사가 아직 시작되지 않았습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:23` |
| 220 | demoday | `DEMODAY-0112` | `DEMODAY_POLL_INVALID_STATUS_TRANSITION` | 409 CONFLICT | 허용되지 않는 데모데이 투표 상태 전이입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:24` |
| 221 | demoday | `DEMODAY-0200` | `DEMODAY_BOOTH_INVALID_IDENTIFIER` | 400 BAD_REQUEST | 부스는 등록된 프로젝트나 표시 이름 중 하나를 가져야 합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:26` |
| 222 | demoday | `DEMODAY-0201` | `DEMODAY_BOOTH_INVALID_NAME` | 400 BAD_REQUEST | 부스 이름은 1자 이상 255자 이하로 작성해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:27` |
| 223 | demoday | `DEMODAY-0202` | `DEMODAY_BOOTH_NOT_FOUND` | 404 NOT_FOUND | 부스를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:28` |
| 224 | demoday | `DEMODAY-0203` | `DEMODAY_BOOTH_INVALID_CODE` | 400 BAD_REQUEST | 부스 코드는 1 이상의 정수여야 합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:29` |
| 225 | demoday | `DEMODAY-0204` | `DEMODAY_BOOTH_CODE_DUPLICATED` | 409 CONFLICT | 같은 데모데이 투표에서 이미 사용 중인 부스 코드입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:30` |
| 226 | demoday | `DEMODAY-0300` | `DEMODAY_ENTRY_CODE_ALREADY_REDEEMED` | 409 CONFLICT | 이미 사용된 인증 코드예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:32` |
| 227 | demoday | `DEMODAY-0301` | `DEMODAY_ENTRY_CODE_ALREADY_BOUND` | 409 CONFLICT | 이미 다른 계정에 연결된 인증 코드예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:33` |
| 228 | demoday | `DEMODAY-0302` | `DEMODAY_ENTRY_CODE_GENERATION_NOT_ALLOWED` | 400 BAD_REQUEST | 행사 진행 전 또는 진행 중에만 생성할 수 있습니다 |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:34` |
| 229 | demoday | `DEMODAY-0303` | `DEMODAY_ENTRY_CODE_NOT_FOUND` | 404 NOT_FOUND | 존재하지 않는 입장 코드예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:35` |
| 230 | demoday | `DEMODAY-0304` | `DEMODAY_ENTRY_CODE_POLL_MISMATCH` | 409 CONFLICT | 이번 데모데이의 입장 코드가 아니에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:36` |
| 231 | demoday | `DEMODAY-0400` | `DEMODAY_VOTE_NOT_OPENED_YET` | 409 CONFLICT | 아직 투표 시간이 아니에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:38` |
| 232 | demoday | `DEMODAY-0401` | `DEMODAY_VOTE_CLOSED` | 409 CONFLICT | 투표가 종료되었어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:39` |
| 233 | demoday | `DEMODAY-0402` | `DEMODAY_VOTE_ALREADY_CAST` | 409 CONFLICT | 이미 투표를 완료했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:40` |
| 234 | demoday | `DEMODAY-0403` | `DEMODAY_VOTE_ALREADY_REVOKED` | 409 CONFLICT | 이미 무효 처리된 표에요 |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:41` |
| 235 | demoday | `DEMODAY-0404` | `DEMODAY_VOTE_POLL_MISMATCH` | 409 CONFLICT | 이번 데모데이의 부스에만 투표할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:42` |
| 236 | demoday | `DEMODAY-0405` | `DEMODAY_VOTE_QR_INVALID` | 401 UNAUTHORIZED | QR 서명이 유효하지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:43` |
| 237 | demoday | `DEMODAY-0406` | `DEMODAY_VOTE_QR_EXPIRED` | 401 UNAUTHORIZED | 만료된 QR이에요. 다시 스캔해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:44` |
| 238 | demoday | `DEMODAY-0407` | `DEMODAY_VOTE_QR_PURPOSE_MISMATCH` | 401 UNAUTHORIZED | 투표 인증 용도의 QR이 아니에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:45` |
| 239 | demoday | `DEMODAY-0408` | `DEMODAY_VOTE_QR_POLL_MISMATCH` | 401 UNAUTHORIZED | 이번 데모데이의 QR이 아니에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:46` |
| 240 | demoday | `DEMODAY-0409` | `DEMODAY_VOTE_INSUFFICIENT_STAMPS` | 403 FORBIDDEN | 투표하려면 스탬프 6개가 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:47` |
| 241 | demoday | `DEMODAY-0410` | `DEMODAY_VOTE_AUTHORIZATION_INVALID` | 401 UNAUTHORIZED | 투표 권한이 유효하지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:48` |
| 242 | demoday | `DEMODAY-0411` | `DEMODAY_VOTE_AUTHORIZATION_EXPIRED` | 401 UNAUTHORIZED | 투표 권한이 만료되었어요. INFO QR을 다시 스캔해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:49` |
| 243 | demoday | `DEMODAY-0412` | `DEMODAY_VOTE_AUTHORIZATION_PURPOSE_MISMATCH` | 401 UNAUTHORIZED | 최종 투표 용도의 권한이 아니에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:50` |
| 244 | demoday | `DEMODAY-0413` | `DEMODAY_VOTE_AUTHORIZATION_PARTICIPANT_MISMATCH` | 403 FORBIDDEN | 다른 참여자에게 발급된 투표 권한이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:51` |
| 245 | demoday | `DEMODAY-0414` | `DEMODAY_VOTE_NOT_FOUND` | 404 NOT_FOUND | 투표 기록을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:52` |
| 246 | demoday | `DEMODAY-0415` | `DEMODAY_VOTE_NOT_REVOKED` | 409 CONFLICT | 무효 처리되지 않은 표에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:53` |
| 247 | demoday | `DEMODAY-0416` | `DEMODAY_VOTE_EXTERNAL_BOOTH_NOT_ALLOWED` | 409 CONFLICT | 외부 부스에는 투표할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:54` |
| 248 | demoday | `DEMODAY-0417` | `DEMODAY_VOTE_OWN_BOOTH_FORBIDDEN` | 403 FORBIDDEN | 소속 부스에는 투표할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:55` |
| 249 | demoday | `DEMODAY-0501` | `DEMODAY_STAMP_ALREADY_COLLECTED` | 409 CONFLICT | 이미 스탬프를 받은 부스예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:57` |
| 250 | demoday | `DEMODAY-0502` | `DEMODAY_STAMP_ALREADY_REVOKED` | 409 CONFLICT | 이미 무효 처리된 스탬프예요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:58` |
| 251 | demoday | `DEMODAY-0503` | `DEMODAY_STAMP_POLL_MISMATCH` | 409 CONFLICT | 이번 데모데이의 부스에만 스탬프를 받을 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:59` |
| 252 | demoday | `DEMODAY-0504` | `DEMODAY_STAMP_CREDENTIAL_INVALID` | 409 CONFLICT | 유효하지 않은 스탬프 QR이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:60` |
| 253 | demoday | `DEMODAY-0505` | `DEMODAY_STAMP_COOLDOWN_ACTIVE` | 409 CONFLICT | 아직 다음 스탬프를 적립할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:61` |
| 254 | demoday | `DEMODAY-0506` | `DEMODAY_STAMP_MAX_COUNT_REACHED` | 409 CONFLICT | 이미 모든 부스의 스탬프를 받았어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:62` |
| 255 | demoday | `DEMODAY-0600` | `DEMODAY_ADMIN_ACCESS_DENIED` | 403 FORBIDDEN | 접근 권한이 없는 사용자입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/demoday/domain/exception/DemodayErrorCode.java:64` |

## feedback

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 256 | feedback | `FEEDBACK-0001` | `USER_FEEDBACK_TEMPLATE_NOT_FOUND` | 404 NOT_FOUND | 피드백 양식을 찾을 수 없어요. 양식을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/feedback/domain/exception/FeedbackErrorCode.java:15` |

## form

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 257 | form | `FORM-0001` | `FORM_NOT_FOUND` | 404 NOT_FOUND | 폼을 찾을 수 없어요. 선택한 폼을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:14` |
| 258 | form | `FORM-0002` | `FORM_NOT_DRAFT` | 409 CONFLICT | 임시저장 상태의 폼만 편집할 수 있어요. 폼 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:15` |
| 259 | form | `FORM-0003` | `QUESTION_NOT_FOUND` | 404 NOT_FOUND | 질문을 찾을 수 없어요. 선택한 질문을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:16` |
| 260 | form | `FORM-0005` | `FORM_ALREADY_PUBLISHED` | 400 BAD_REQUEST | 이미 발행된 폼이에요. 폼 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:17` |
| 261 | form | `FORM-0006` | `FORM_RESPONSE_NOT_FOUND` | 404 NOT_FOUND | 폼 응답을 찾을 수 없어요. 응답 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:18` |
| 262 | form | `FORM-0007` | `QUESTION_IS_NOT_OWNED_BY_FORM` | 400 BAD_REQUEST | 이 폼에 포함된 질문이 아니에요. 질문을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:19` |
| 263 | form | `FORM-0008` | `FORM_RESPONSE_FORBIDDEN` | 403 FORBIDDEN | 이 폼 응답에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:20` |
| 264 | form | `FORM-0009` | `QUESTION_TYPE_MISMATCH` | 400 BAD_REQUEST | 질문 유형이 맞지 않아요. 질문 유형을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:22` |
| 265 | form | `FORM-0010` | `REQUIRED_QUESTION_NOT_ANSWERED` | 400 BAD_REQUEST | 필수 질문에 답변해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:23` |
| 266 | form | `FORM-0011` | `INVALID_ANSWER_FORMAT` | 400 BAD_REQUEST | 응답 형식이 올바르지 않아요. 답변을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:24` |
| 267 | form | `FORM-0012` | `OTHER_OPTION_DUPLICATED` | 400 BAD_REQUEST | '기타' 선택지가 중복됐어요. 선택지를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:25` |
| 268 | form | `FORM-0013` | `OPTION_NOT_IN_QUESTION` | 400 BAD_REQUEST | 해당 질문에 없는 선택지예요. 선택지를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:26` |
| 269 | form | `FORM-0014` | `OPTION_TEXT_REQUIRED` | 400 BAD_REQUEST | '기타' 선택지의 내용을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:27` |
| 270 | form | `FORM-0015` | `INVALID_FORM_ACTIVE_PERIOD` | 400 BAD_REQUEST | 폼 응답 가능 기간이 올바르지 않아요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:28` |
| 271 | form | `FORM-0023` | `INVALID_VOTE_SELECTION` | 400 BAD_REQUEST | 투표 선택이 올바르지 않아요. 선택지를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:30` |
| 272 | form | `FORM-0025` | `INVALID_VOTE_FORM_STRUCTURE` | 400 BAD_REQUEST | 투표 질문 형식이 올바르지 않아요. 투표 구성을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:32` |
| 273 | form | `FORM-0027` | `FORM_RESPONSE_ALREADY_EXISTS` | 400 BAD_REQUEST | 이미 제출한 응답이 있어요. 제출 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:34` |
| 274 | form | `FORM-0028` | `FORM_NOT_PUBLISHED` | 409 CONFLICT | 발행된 폼에만 응답할 수 있어요. 폼 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:35` |
| 275 | form | `FORM-0029` | `QUESTION_OPTION_NOT_FOUND` | 404 NOT_FOUND | 선택지를 찾을 수 없어요. 선택지를 다시 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:36` |
| 276 | form | `FORM-0030` | `ANSWER_NOT_FOUND` | 404 NOT_FOUND | 답변을 찾을 수 없어요. 응답 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:37` |
| 277 | form | `FORM-0031` | `FORM_RESPONSE_NOT_DRAFT` | 409 CONFLICT | 임시저장 상태의 응답에서만 할 수 있는 작업이에요. 응답 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:38` |
| 278 | form | `FORM-0032` | `ANSWER_ALREADY_EXISTS` | 400 BAD_REQUEST | 이미 해당 질문에 대한 답변이 있어요. 기존 답변을 수정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:39` |
| 279 | form | `FORM-0033` | `FORM_RESPONSE_LOOKUP_AMBIGUOUS` | 409 CONFLICT | 중복 응답을 허용하는 폼은 응답을 하나로 특정할 수 없어요. 응답 ID를 사용해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:40` |
| 280 | form | `FORM-0034` | `RESPONDENT_MEMBER_ID_REQUIRED` | 400 BAD_REQUEST | 응답자 정보가 필요해요. 이 문제가 계속되면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:42` |
| 281 | form | `FORM-0035` | `RESPONSE_ACCESS_KEY_REQUIRED` | 400 BAD_REQUEST | 응답 접근 키가 필요해요. 이 문제가 계속되면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:44` |
| 282 | form | `FORM-0036` | `INVALID_SUBMIT_SCOPE` | 400 BAD_REQUEST | 제출 범위가 올바르지 않아요. 이 문제가 계속되면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:46` |
| 283 | form | `FORM-0037` | `INVALID_NEXT_SECTION_SELF_LOOP` | 400 BAD_REQUEST | 조건부 섹션 이동은 자기 자신을 대상으로 할 수 없어요. 이동 대상 섹션을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:48` |
| 284 | form | `FORM-0038` | `FORM_INVALID_TRANSITION` | 409 CONFLICT | 현재 폼 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:50` |
| 285 | form | `FORM-0039` | `FORM_HAS_RESPONSES` | 409 CONFLICT | 응답이 있는 폼은 초안 상태로 되돌릴 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:51` |
| 286 | form | `FORM-0040` | `INVALID_NEXT_SECTION_BACKWARD` | 400 BAD_REQUEST | 조건부 섹션 이동은 뒤 섹션으로만 갈 수 있어요. 앞이나 같은 위치 섹션은 선택할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:52` |
| 287 | form | `FORM-0041` | `MULTIPLE_BRANCHING_QUESTIONS_IN_SECTION` | 400 BAD_REQUEST | 한 섹션에는 조건부 이동을 지정한 질문을 하나만 둘 수 있어요. 다른 질문의 이동 설정을 먼저 해제해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:54` |
| 288 | form | `FORM-0042` | `FORM_RESPONSE_ALREADY_CLAIMED` | 409 CONFLICT | 이미 다른 사용자에게 등록된 응답이에요. 응답을 다시 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:56` |
| 289 | form | `FORM-0043` | `DRAFT_SCHEMA_MISMATCH` | 400 BAD_REQUEST | 폼이 수정되었어요. 아래 질문의 답변을 다시 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:58` |
| 290 | form | `FORM-0044` | `FORM_RESPONSE_CONCURRENT_MODIFICATION` | 409 CONFLICT | 이 응답이 방금 다른 곳에서 수정됐어요. 새로고침 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:60` |
| 291 | form | `FORM-0045` | `FORM_RESPONSE_NOT_IN_FORM` | 400 BAD_REQUEST | 요청한 응답이 이 폼에 속해 있지 않아요. 이 문제가 계속되면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:62` |
| 292 | form | `FORM-0046` | `FORM_RESPONSE_NOT_SUBMITTED` | 400 BAD_REQUEST | 아직 제출되지 않은 응답이 포함되어 있어요. 제출된 응답으로 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/form/domain/exception/FormErrorCode.java:64` |

## global

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 293 | global | `COMMON-0001` | `INTERNAL_SERVER_ERROR` | 500 INTERNAL_SERVER_ERROR | 요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:26` |
| 294 | global | `COMMON-400` | `BAD_REQUEST` | 400 BAD_REQUEST | 요청 값이 올바르지 않아요. 입력한 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:28` |
| 295 | global | `COMMON-401` | `UNAUTHORIZED` | 401 UNAUTHORIZED | 로그인이 필요해요. 로그인 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:29` |
| 296 | global | `COMMON-403` | `FORBIDDEN` | 403 FORBIDDEN | 요청할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:30` |
| 297 | global | `COMMON-404` | `NOT_FOUND` | 404 NOT_FOUND | 요청한 항목을 찾을 수 없어요. 입력한 값을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:31` |
| 298 | global | `COMMON-429` | `TOO_MANY_REQUESTS` | 429 TOO_MANY_REQUESTS | 요청이 너무 많습니다. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:32` |
| 299 | global | `COMMON-501` | `NOT_IMPLEMENTED` | 501 NOT_IMPLEMENTED | 아직 사용할 수 없는 기능이에요. 필요한 기능이라면 서버팀에 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:33` |
| 300 | global | `ENV-0001` | `INVALID_ENV` | 400 BAD_REQUEST | 현재 실행 환경에서는 사용할 수 없는 기능이에요. 환경 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:42` |
| 301 | global | `PE-0001` | `PERMISSION_TYPE_NOT_IMPLEMENTED` | 501 NOT_IMPLEMENTED | 아직 지원하지 않는 권한 확인이에요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:45` |
| 302 | global | `SECURITY-0001` | `SECURITY_NOT_GIVEN` | 401 UNAUTHORIZED | 인증 정보가 없어요. 로그인 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:36` |
| 303 | global | `SECURITY-0002` | `SECURITY_FORBIDDEN` | 403 FORBIDDEN | 권한이 부족해요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:37` |
| 304 | global | `SECURITY-0003` | `SECURITY_WEBSOCKET_BROKER_ACCESS` | 403 FORBIDDEN | 브로커 경로로 직접 메시지를 전송할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:38` |
| 305 | global | `SECURITY-0004` | `SECURITY_WEBSOCKET_INVALID_DESTINATION` | 403 FORBIDDEN | 허용되지 않은 웹소켓 경로입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/global/exception/constant/CommonErrorCode.java:39` |

## llm

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 306 | llm | `LLM-0001` | `CHAT_COMPLETION_FAILED` | 502 BAD_GATEWAY | AI 응답을 생성하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/llm/domain/exception/LlmErrorCode.java:14` |
| 307 | llm | `LLM-0002` | `CHAT_COMPLETION_INVALID_RESPONSE` | 502 BAD_GATEWAY | AI 응답을 읽지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/llm/domain/exception/LlmErrorCode.java:15` |
| 308 | llm | `LLM-0003` | `PROVIDER_NOT_CONFIGURED` | 500 INTERNAL_SERVER_ERROR | AI 제공자 설정이 누락됐어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/llm/domain/exception/LlmErrorCode.java:16` |

## maintenance

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 309 | maintenance | `MAINTENANCE-0001` | `SERVICE_UNDER_MAINTENANCE` | 503 SERVICE_UNAVAILABLE | 서비스 점검 중이에요. 점검이 끝난 뒤 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:14` |
| 310 | maintenance | `MAINTENANCE-0002` | `MAINTENANCE_WINDOW_NOT_FOUND` | 404 NOT_FOUND | 점검 일정을 찾을 수 없어요. 선택한 일정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:15` |
| 311 | maintenance | `MAINTENANCE-0003` | `INVALID_TIME_RANGE` | 400 BAD_REQUEST | 종료 시각은 시작 시각 이후로 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:16` |
| 312 | maintenance | `MAINTENANCE-0004` | `START_AT_IN_PAST` | 400 BAD_REQUEST | 시작 시각은 현재 시각 이후로 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:17` |
| 313 | maintenance | `MAINTENANCE-0005` | `TARGET_DOMAINS_REQUIRED` | 400 BAD_REQUEST | 도메인별 점검은 대상 도메인을 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:18` |
| 314 | maintenance | `MAINTENANCE-0006` | `OVERLAPPING_WINDOW` | 409 CONFLICT | 다른 점검 일정과 시간이 겹쳐요. 시간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:19` |
| 315 | maintenance | `MAINTENANCE-0007` | `ALREADY_ENDED` | 400 BAD_REQUEST | 이미 종료된 점검 일정이에요. 진행 중이거나 예정된 일정을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:20` |
| 316 | maintenance | `MAINTENANCE-0008` | `NOT_SUPER_ADMIN` | 403 FORBIDDEN | 점검을 관리할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/maintenance/exception/MaintenanceErrorCode.java:21` |

## member

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 317 | member | `MEMBER-0001` | `MEMBER_NOT_FOUND` | 404 NOT_FOUND | 사용자를 찾을 수 없어요. 선택한 사용자를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:14` |
| 318 | member | `MEMBER-0002` | `MEMBER_ALREADY_EXISTS` | 409 CONFLICT | 이미 등록된 사용자예요. 기존 계정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:15` |
| 319 | member | `MEMBER-0003` | `EMAIL_ALREADY_EXISTS` | 409 CONFLICT | 이미 사용 중인 이메일이에요. 다른 이메일을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:16` |
| 320 | member | `MEMBER-0004` | `MEMBER_ALREADY_WITHDRAWN` | 400 BAD_REQUEST | 이미 탈퇴한 사용자예요. 다른 계정으로 진행해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:17` |
| 321 | member | `MEMBER-0005` | `INVALID_MEMBER_STATUS` | 400 BAD_REQUEST | 사용자 상태가 올바르지 않아요. 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:18` |
| 322 | member | `MEMBER-0006` | `MEMBER_NOT_ACTIVE` | 400 BAD_REQUEST | 활동 중인 사용자만 이용할 수 있어요. 계정 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:19` |
| 323 | member | `MEMBER-0007` | `MEMBER_ALREADY_REGISTERED` | 400 BAD_REQUEST | 이미 회원가입을 완료한 사용자예요. 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:20` |
| 324 | member | `MEMBER-0008` | `MEMBER_PROFILE_NOT_FOUND` | 404 NOT_FOUND | 프로필을 찾을 수 없어요. 프로필 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:21` |
| 325 | member | `MEMBER-0009` | `MEMBER_SCHOOL_NOT_ASSIGNED` | 400 BAD_REQUEST | 학교가 등록되지 않은 사용자예요. 학교 정보를 먼저 등록해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:22` |
| 326 | member | `MEMBER-0010` | `CREDENTIAL_ALREADY_REGISTERED` | 409 CONFLICT | 이미 로그인 ID와 비밀번호가 등록되어 있어요. 기존 정보로 로그인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:23` |
| 327 | member | `MEMBER-0011` | `CREDENTIAL_NOT_REGISTERED` | 400 BAD_REQUEST | 로그인 ID와 비밀번호가 등록되어 있지 않아요. 먼저 등록해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:24` |
| 328 | member | `MEMBER-0012` | `INVALID_LOGIN_ID` | 400 BAD_REQUEST | 로그인 ID가 올바르지 않아요. 다시 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:25` |
| 329 | member | `MEMBER-0013` | `INVALID_PASSWORD` | 400 BAD_REQUEST | 비밀번호가 올바르지 않아요. 다시 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:26` |
| 330 | member | `MEMBER-0014` | `MEMBER_SEARCH_ACCESS_DENIED` | 403 FORBIDDEN | 챌린저 기록이 있는 회원만 회원 검색을 사용할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/member/domain/exception/MemberErrorCode.java:27` |

## notice

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 331 | notice | `NOTICE-0001` | `NOTICE_NOT_FOUND` | 404 NOT_FOUND | 공지를 찾을 수 없어요. 목록을 새로고침해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:14` |
| 332 | notice | `NOTICE-0002` | `ALREADY_PUBLISHED_NOTICE` | 400 BAD_REQUEST | 이미 게시된 공지예요. 게시 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:15` |
| 333 | notice | `NOTICE-0003` | `INVALID_NOTICE_TITLE` | 400 BAD_REQUEST | 공지 제목이 올바르지 않아요. 제목을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:16` |
| 334 | notice | `NOTICE-0004` | `INVALID_NOTICE_CONTENT` | 400 BAD_REQUEST | 공지 내용이 올바르지 않아요. 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:17` |
| 335 | notice | `NOTICE-0005` | `INVALID_NOTICE_STATUS_FOR_REMINDER` | 400 BAD_REQUEST | 현재 상태에서는 공지 알림을 보낼 수 없어요. 공지 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:18` |
| 336 | notice | `NOTICE-0006` | `AUTHOR_REQUIRED` | 400 BAD_REQUEST | 공지 작성자 정보가 필요해요. 로그인 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:19` |
| 337 | notice | `NOTICE-0007` | `NOTICE_SCOPE_REQUIRED` | 400 BAD_REQUEST | 공지 대상 범위를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:20` |
| 338 | notice | `NOTICE-0008` | `NOTICE_AUTHOR_MISMATCH` | 403 FORBIDDEN | 공지 작성자만 수정할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:21` |
| 339 | notice | `NOTICE-0009` | `NO_WRITE_PERMISSION` | 403 FORBIDDEN | 공지를 작성할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:22` |
| 340 | notice | `NOTICE-0010` | `INVALID_TARGET_SETTING` | 400 BAD_REQUEST | 공지 수신자 설정이 올바르지 않아요. 대상 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:26` |
| 341 | notice | `NOTICE-0011` | `NO_TARGET_FOUND` | 404 NOT_FOUND | 공지 수신 대상을 찾을 수 없어요. 대상 설정을 다시 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:27` |
| 342 | notice | `NOTICE-0012` | `NO_READ_PERMISSION` | 403 FORBIDDEN | 공지를 조회할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:24` |
| 343 | notice | `NOTICE-9999` | `NOT_IMPLEMENTED_YET` | 501 NOT_IMPLEMENTED | 아직 사용할 수 없는 기능이에요. 필요한 기능이라면 서버팀에 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:45` |
| 344 | notice | `NOTICE-CONTENTS-0001` | `VOTE_IDS_REQUIRED` | 400 BAD_REQUEST | 투표를 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:30` |
| 345 | notice | `NOTICE-CONTENTS-0002` | `IMAGE_URLS_REQUIRED` | 400 BAD_REQUEST | 이미지 링크를 1개 이상 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:31` |
| 346 | notice | `NOTICE-CONTENTS-0003` | `LINK_URLS_REQUIRED` | 400 BAD_REQUEST | 공지 링크를 1개 이상 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:32` |
| 347 | notice | `NOTICE-CONTENTS-0004` | `NOTICE_VOTE_NOT_FOUND` | 404 NOT_FOUND | 공지 투표를 찾을 수 없어요. 투표를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:33` |
| 348 | notice | `NOTICE-CONTENTS-0005` | `NOTICE_IMAGE_NOT_FOUND` | 404 NOT_FOUND | 공지 이미지를 찾을 수 없어요. 이미지를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:34` |
| 349 | notice | `NOTICE-CONTENTS-0006` | `NOTICE_LINK_NOT_FOUND` | 404 NOT_FOUND | 공지 링크를 찾을 수 없어요. 링크를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:35` |
| 350 | notice | `NOTICE-CONTENTS-0007` | `IMAGE_LIMIT_EXCEEDED` | 400 BAD_REQUEST | 공지 이미지는 최대 10장까지 등록할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:36` |
| 351 | notice | `NOTICE-CONTENTS-0008` | `VOTE_ALREADY_EXISTS` | 409 CONFLICT | 이 공지에는 이미 투표가 있어요. 기존 투표를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:37` |
| 352 | notice | `NOTICE-CONTENTS-0009` | `INVALID_VOTE_OPTION_COUNT` | 400 BAD_REQUEST | 투표 선택지는 2개 이상 5개 이하로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:38` |
| 353 | notice | `NOTICE-CONTENTS-0010` | `INVALID_VOTE_OPTION_CONTENT` | 400 BAD_REQUEST | 투표 선택지에 빈 값이 있어요. 선택지 내용을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:39` |
| 354 | notice | `NOTICE-CONTENTS-0011` | `VOTE_NOT_STARTED` | 400 BAD_REQUEST | 아직 투표 기간이 시작되지 않았어요. 시작 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:40` |
| 355 | notice | `NOTICE-CONTENTS-0012` | `VOTE_CLOSED` | 400 BAD_REQUEST | 이미 종료된 투표예요. 투표 기간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:41` |
| 356 | notice | `NOTICE-CONTENTS-0013` | `SELECTED_OPTION_IDS_REQUIRED` | 400 BAD_REQUEST | 투표 선택지를 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notice/domain/exception/NoticeErrorCode.java:42` |

## notification

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 357 | notification | `EMAIL-0004` | `EMAIL_TEMPLATE_RENDER_FAILED` | 500 INTERNAL_SERVER_ERROR | 이메일 본문을 만들지 못했어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/EmailErrorCode.java:13` |
| 358 | notification | `EMAIL-0005` | `EMAIL_SEND_FAILED` | 500 INTERNAL_SERVER_ERROR | 이메일을 보내지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/EmailErrorCode.java:14` |
| 359 | notification | `FCM-0001` | `FCM_NOT_FOUND` | 404 NOT_FOUND | 푸시 알림 정보를 찾을 수 없어요. 알림 설정을 다시 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:14` |
| 360 | notification | `FCM-0002` | `USER_FCM_NOT_FOUND` | 404 NOT_FOUND | 사용자의 푸시 알림 정보를 찾을 수 없어요. 알림 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:15` |
| 361 | notification | `FCM-0003` | `FCM_SEND_FAILED` | 500 INTERNAL_SERVER_ERROR | 푸시 알림을 보내지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:16` |
| 362 | notification | `FCM-0004` | `TOPIC_SUBSCRIBE_FAILED` | 500 INTERNAL_SERVER_ERROR | 푸시 알림 주제를 구독하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:17` |
| 363 | notification | `FCM-0005` | `TOPIC_UNSUBSCRIBE_FAILED` | 500 INTERNAL_SERVER_ERROR | 푸시 알림 주제 구독을 해제하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:18` |
| 364 | notification | `FCM-0006` | `TOPIC_SEND_FAILED` | 500 INTERNAL_SERVER_ERROR | 푸시 알림 주제 메시지를 보내지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:19` |
| 365 | notification | `FCM-0007` | `RATE_LIMITED` | 429 TOO_MANY_REQUESTS | 푸시 알림 요청이 너무 많아요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/FcmErrorCode.java:20` |
| 366 | notification | `WEBHOOK-0001` | `WEBHOOK_SEND_FAILED` | 500 INTERNAL_SERVER_ERROR | 웹훅 메시지를 보내지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/WebhookErrorCode.java:14` |
| 367 | notification | `WEBHOOK-0002` | `WEBHOOK_ADAPTER_NOT_FOUND` | 400 BAD_REQUEST | 해당 플랫폼의 웹훅 설정을 찾을 수 없어요. 플랫폼 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/notification/domain/exception/WebhookErrorCode.java:15` |

## organization

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 368 | organization | `ORGANIZATION-0001` | `GISU_REQUIRED` | 400 BAD_REQUEST | 기수를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:14` |
| 369 | organization | `ORGANIZATION-0002` | `ORGAN_NAME_REQUIRED` | 400 BAD_REQUEST | 조직 이름을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:15` |
| 370 | organization | `ORGANIZATION-0003` | `SCHOOL_REQUIRED` | 400 BAD_REQUEST | 학교를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:16` |
| 371 | organization | `ORGANIZATION-0004` | `CHAPTER_REQUIRED` | 400 BAD_REQUEST | 지부를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:17` |
| 372 | organization | `ORGANIZATION-0005` | `GISU_START_AT_REQUIRED` | 400 BAD_REQUEST | 기수 시작일을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:20` |
| 373 | organization | `ORGANIZATION-0006` | `GISU_END_AT_REQUIRED` | 400 BAD_REQUEST | 기수 종료일을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:21` |
| 374 | organization | `ORGANIZATION-0007` | `GISU_PERIOD_INVALID` | 400 BAD_REQUEST | 기수 시작일은 종료일보다 빨라야 해요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:22` |
| 375 | organization | `ORGANIZATION-0008` | `SCHOOL_NAME_REQUIRED` | 400 BAD_REQUEST | 학교 이름을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:24` |
| 376 | organization | `ORGANIZATION-0009` | `SCHOOL_DOMAIN_REQUIRED` | 400 BAD_REQUEST | 학교 이메일 도메인을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:25` |
| 377 | organization | `ORGANIZATION-0010` | `STUDY_GROUP_NAME_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 이름을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:27` |
| 378 | organization | `ORGANIZATION-0011` | `STUDY_GROUP_LEADER_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 리더를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:28` |
| 379 | organization | `ORGANIZATION-0012` | `STUDY_GROUP_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:30` |
| 380 | organization | `ORGANIZATION-0013` | `STUDY_GROUP_MEMBER_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 멤버는 1명 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:31` |
| 381 | organization | `ORGANIZATION-0014` | `STUDY_GROUP_MEMBER_ID_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 멤버를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:32` |
| 382 | organization | `ORGANIZATION-0015` | `STUDY_GROUP_MEMBER_ALREADY_EXISTS` | 400 BAD_REQUEST | 이미 스터디 그룹에 포함된 멤버예요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:33` |
| 383 | organization | `ORGANIZATION-0016` | `STUDY_GROUP_MEMBER_NOT_FOUND` | 404 NOT_FOUND | 스터디 그룹 멤버를 찾을 수 없어요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:34` |
| 384 | organization | `ORGANIZATION-0017` | `CHAPTER_NOT_FOUND` | 404 NOT_FOUND | 지부를 찾을 수 없어요. 선택한 지부를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:36` |
| 385 | organization | `ORGANIZATION-0018` | `SCHOOL_NOT_FOUND` | 404 NOT_FOUND | 학교를 찾을 수 없어요. 선택한 학교를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:37` |
| 386 | organization | `ORGANIZATION-0019` | `GISU_IS_ACTIVE_NOT_FOUND` | 404 NOT_FOUND | 활성화된 기수를 찾을 수 없어요. 기수 설정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:38` |
| 387 | organization | `ORGANIZATION-0020` | `GISU_NOT_FOUND` | 404 NOT_FOUND | 기수를 찾을 수 없어요. 선택한 기수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:39` |
| 388 | organization | `ORGANIZATION-0021` | `PART_REQUIRED` | 400 BAD_REQUEST | 파트를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:40` |
| 389 | organization | `ORGANIZATION-0022` | `STUDY_GROUP_NAME_INVALID` | 400 BAD_REQUEST | 스터디 그룹 이름이 올바르지 않아요. 이름을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:41` |
| 390 | organization | `ORGANIZATION-0023` | `STUDY_GROUP_NOT_FOUND` | 400 BAD_REQUEST | 스터디 그룹을 찾을 수 없어요. 선택한 그룹을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:42` |
| 391 | organization | `ORGANIZATION-0024` | `STUDY_GROUP_CHALLENGER_INVALID` | 400 BAD_REQUEST | 스터디 그룹 리더 또는 멤버에 존재하지 않는 챌린저가 있어요. 구성원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:44` |
| 392 | organization | `ORGANIZATION-0025` | `LEADER_CANNOT_BE_MEMBER` | 400 BAD_REQUEST | 스터디 그룹 리더는 멤버로 중복 등록할 수 없어요. 구성원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:46` |
| 393 | organization | `ORGANIZATION-0026` | `STUDY_GROUP_MEMBER_DUPLICATED` | 400 BAD_REQUEST | 스터디 그룹 멤버가 중복됐어요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:47` |
| 394 | organization | `ORGANIZATION-0027` | `NO_SUCH_CHAPTER_SCHOOL` | 404 NOT_FOUND | 학교와 지부 연결 정보를 찾을 수 없어요. 배정 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:48` |
| 395 | organization | `ORGANIZATION-0028` | `GISU_ALREADY_EXISTS` | 409 CONFLICT | 이미 존재하는 기수예요. 기존 기수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:49` |
| 396 | organization | `ORGANIZATION-0029` | `SCHOOL_ALREADY_ASSIGNED_TO_CHAPTER` | 409 CONFLICT | 해당 기수에서 이미 다른 지부에 배정된 학교가 있어요. 학교 배정 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:50` |
| 397 | organization | `ORGANIZATION-0030` | `CHAPTER_NAME_DUPLICATED` | 409 CONFLICT | 해당 기수에 같은 이름의 지부가 이미 있어요. 다른 이름을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:52` |
| 398 | organization | `ORGANIZATION-0031` | `STUDY_GROUP_ACCESS_DENIED` | 403 FORBIDDEN | 스터디 그룹을 조회할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:54` |
| 399 | organization | `ORGANIZATION-0032` | `GISU_HAS_ASSOCIATED_CHAPTERS` | 409 CONFLICT | 연결된 지부 또는 학교가 있어 기수를 삭제할 수 없어요. 연결 정보를 먼저 정리해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:56` |
| 400 | organization | `ORGANIZATION-0033` | `STUDY_GROUP_MENTOR_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 파트장은 1명 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:58` |
| 401 | organization | `ORGANIZATION-0034` | `STUDY_GROUP_MENTOR_ID_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 파트장을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:59` |
| 402 | organization | `ORGANIZATION-0035` | `STUDY_GROUP_MEMBER_ALREADY_IN_PART_STUDY` | 409 CONFLICT | 다른 스터디 그룹에 이미 속한 멤버가 있어요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:61` |
| 403 | organization | `ORGANIZATION-0036` | `STUDY_GROUP_MENTOR_DUPLICATED` | 400 BAD_REQUEST | 이미 해당 스터디에 속한 파트장이에요. 파트장 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:62` |
| 404 | organization | `ORGANIZATION-0037` | `STUDY_GROUP_MENTOR_NOT_FOUND` | 404 NOT_FOUND | 스터디 그룹 파트장 정보를 찾을 수 없어요. 파트장 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:63` |
| 405 | organization | `ORGANIZATION-0038` | `STUDY_GROUP_SCHEDULE_ATTENDANCE_POLICY_REQUIRED` | 400 BAD_REQUEST | 스터디 그룹 일정에는 출석 정책이 필요해요. 출석 정책을 설정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:65` |
| 406 | organization | `ORGANIZATION-0039` | `STUDY_GROUP_SCHEDULE_ALREADY_EXISTS` | 409 CONFLICT | 해당 스터디 그룹과 주차에 연결된 일정이 이미 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:67` |
| 407 | organization | `ORGANIZATION-0045` | `UMC_PRODUCT_MEMBER_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 인원은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:70` |
| 408 | organization | `ORGANIZATION-0046` | `UMC_PRODUCT_MEMBER_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT 인원을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:71` |
| 409 | organization | `ORGANIZATION-0047` | `UMC_PRODUCT_MEMBER_ALREADY_EXISTS` | 409 CONFLICT | 이미 등록된 UMC PRODUCT 인원입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:72` |
| 410 | organization | `ORGANIZATION-0048` | `UMC_PRODUCT_MEMBER_ID_REQUIRED` | 400 BAD_REQUEST | 회원 ID는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:73` |
| 411 | organization | `ORGANIZATION-0051` | `UMC_PRODUCT_ROLE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 직책은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:74` |
| 412 | organization | `ORGANIZATION-0052` | `UMC_PRODUCT_POSITION_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 포지션은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:75` |
| 413 | organization | `ORGANIZATION-0053` | `UMC_PRODUCT_ACCESS_DENIED` | 403 FORBIDDEN | UMC PRODUCT 관리 권한이 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:76` |
| 414 | organization | `ORGANIZATION-0058` | `UMC_PRODUCT_SQUAD_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Squad는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:77` |
| 415 | organization | `ORGANIZATION-0059` | `UMC_PRODUCT_SQUAD_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Squad를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:78` |
| 416 | organization | `ORGANIZATION-0060` | `UMC_PRODUCT_SQUAD_CODE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Squad 코드는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:79` |
| 417 | organization | `ORGANIZATION-0061` | `UMC_PRODUCT_SQUAD_NAME_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Squad 이름은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:80` |
| 418 | organization | `ORGANIZATION-0065` | `GISU_QUERY_CONDITION_INVALID` | 400 BAD_REQUEST | 기수 조회 조건이 올바르지 않습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:81` |
| 419 | organization | `ORGANIZATION-0066` | `UMC_PRODUCT_START_DATE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 활동 시작일은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:83` |
| 420 | organization | `ORGANIZATION-0067` | `UMC_PRODUCT_PERIOD_INVALID` | 400 BAD_REQUEST | UMC PRODUCT 활동 종료일은 시작일보다 빠를 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:85` |
| 421 | organization | `ORGANIZATION-0068` | `UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT 멤버 활동 기간은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:87` |
| 422 | organization | `ORGANIZATION-0069` | `UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE` | 400 BAD_REQUEST | 활동 기간은 멤버 활동 기간과 상위 활동 기간 안에 있어야 합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:89` |
| 423 | organization | `ORGANIZATION-0070` | `UMC_PRODUCT_ACTIVITY_PERIOD_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT 멤버 활동 기간을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:91` |
| 424 | organization | `ORGANIZATION-0071` | `UMC_PRODUCT_ACTIVITY_PERIOD_OVERLAPPED` | 409 CONFLICT | UMC PRODUCT 멤버 활동 기간은 겹치거나 빈 날짜 없이 이어질 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:93` |
| 425 | organization | `ORGANIZATION-0072` | `UMC_PRODUCT_ACTIVITY_PERIOD_HAS_ASSOCIATIONS` | 409 CONFLICT | 연결된 활동 이력이 있어 멤버 활동 기간을 삭제할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:95` |
| 426 | organization | `ORGANIZATION-0073` | `UMC_PRODUCT_CHAPTER_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Chapter는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:97` |
| 427 | organization | `ORGANIZATION-0074` | `UMC_PRODUCT_CHAPTER_CODE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Chapter 코드는 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:99` |
| 428 | organization | `ORGANIZATION-0075` | `UMC_PRODUCT_CHAPTER_NAME_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Chapter 이름은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:101` |
| 429 | organization | `ORGANIZATION-0076` | `UMC_PRODUCT_CHAPTER_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Chapter를 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:103` |
| 430 | organization | `ORGANIZATION-0077` | `UMC_PRODUCT_CHAPTER_ALREADY_EXISTS` | 409 CONFLICT | 이미 존재하는 UMC PRODUCT Chapter 코드입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:105` |
| 431 | organization | `ORGANIZATION-0078` | `UMC_PRODUCT_CHAPTER_HAS_MEMBERSHIPS` | 409 CONFLICT | 연결된 소속 이력이 있어 UMC PRODUCT Chapter를 삭제할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:107` |
| 432 | organization | `ORGANIZATION-0086` | `UMC_PRODUCT_CHAPTER_MEMBERSHIP_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Chapter 소속 이력을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:109` |
| 433 | organization | `ORGANIZATION-0087` | `UMC_PRODUCT_CHAPTER_MEMBERSHIP_OVERLAPPED` | 409 CONFLICT | 동일한 UMC PRODUCT Chapter 소속 활동 기간이 겹칩니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:111` |
| 434 | organization | `ORGANIZATION-0089` | `UMC_PRODUCT_LEADERSHIP_ROLE_REQUIRED` | 400 BAD_REQUEST | UMC PRODUCT Leadership 역할은 필수입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:113` |
| 435 | organization | `ORGANIZATION-0090` | `UMC_PRODUCT_LEADERSHIP_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Leadership 이력을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:115` |
| 436 | organization | `ORGANIZATION-0091` | `UMC_PRODUCT_LEADERSHIP_OVERLAPPED` | 409 CONFLICT | 해당 기간에 중복되는 UMC PRODUCT Leadership이 존재합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:117` |
| 437 | organization | `ORGANIZATION-0092` | `UMC_PRODUCT_SQUAD_PARTICIPANT_NOT_FOUND` | 404 NOT_FOUND | UMC PRODUCT Squad 참여 이력을 찾을 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:119` |
| 438 | organization | `ORGANIZATION-0093` | `UMC_PRODUCT_SQUAD_PARTICIPATION_OVERLAPPED` | 409 CONFLICT | 동일 멤버의 UMC PRODUCT Squad 참여 기간이 겹칩니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:121` |
| 439 | organization | `ORGANIZATION-0094` | `UMC_PRODUCT_SQUAD_LEAD_OVERLAPPED` | 409 CONFLICT | 해당 기간에 이미 UMC PRODUCT Squad Lead가 존재합니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:123` |
| 440 | organization | `ORGANIZATION-0095` | `UMC_PRODUCT_SQUAD_HAS_PARTICIPANTS` | 409 CONFLICT | 연결된 참여 이력이 있어 UMC PRODUCT Squad를 삭제할 수 없습니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:125` |
| 441 | organization | `ORGANIZATION-0096` | `UMC_PRODUCT_SQUAD_ALREADY_EXISTS` | 409 CONFLICT | 이미 존재하는 UMC PRODUCT Squad 코드입니다. |  |  |  | false |  |  | `src/main/java/com/umc/product/organization/exception/OrganizationErrorCode.java:127` |

## project

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 442 | project | `PROJECT-0001` | `PROJECT_NOT_FOUND` | 404 NOT_FOUND | 프로젝트를 찾을 수 없어요. 선택한 프로젝트를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:15` |
| 443 | project | `PROJECT-0002` | `ALREADY_COMPLETED_PROJECT` | 400 BAD_REQUEST | 이미 완료된 프로젝트예요. 프로젝트 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:16` |
| 444 | project | `PROJECT-0003` | `PROJECT_ABORT_UNAVAILABLE` | 400 BAD_REQUEST | 이 프로젝트는 중단할 수 없어요. 프로젝트 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:17` |
| 445 | project | `PROJECT-0004` | `APPLICATION_NOT_SUBMITTED` | 400 BAD_REQUEST | 제출된 지원서에서만 할 수 있는 작업이에요. 지원서 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:20` |
| 446 | project | `PROJECT-0005` | `APPLICATION_SUBMIT_NOT_AVAILABLE` | 400 BAD_REQUEST | 이미 제출했거나 평가가 끝난 지원서예요. 지원서 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:21` |
| 447 | project | `PROJECT-0006` | `APPLICATION_FORM_NOT_FOUND` | 404 NOT_FOUND | 프로젝트 지원 폼을 찾을 수 없어요. 선택한 프로젝트를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:29` |
| 448 | project | `PROJECT-0007` | `APPLICATION_FORM_ACCESS_NOT_ALLOWED` | 403 FORBIDDEN | 이 지원 폼 섹션에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:30` |
| 449 | project | `PROJECT-0008` | `PROJECT_DRAFT_ALREADY_IN_PROGRESS` | 409 CONFLICT | 작성 중인 프로젝트가 있어 새로 시작할 수 없어요. 기존 초안을 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:40` |
| 450 | project | `PROJECT-0009` | `PROJECT_INVALID_STATE` | 400 BAD_REQUEST | 현재 상태에서는 할 수 없는 작업이에요. 프로젝트 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:41` |
| 451 | project | `PROJECT-0010` | `PROJECT_OWNER_NOT_PLAN_CHALLENGER` | 400 BAD_REQUEST | 프로젝트 PO는 PLAN 파트 챌린저만 맡을 수 있어요. PO 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:42` |
| 452 | project | `PROJECT-0011` | `PROJECT_SUBMIT_VALIDATION_FAILED` | 400 BAD_REQUEST | 제출에 필요한 정보가 부족해요. 필수 항목을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:43` |
| 453 | project | `PROJECT-0012` | `PROJECT_ACCESS_DENIED` | 403 FORBIDDEN | 이 프로젝트에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:44` |
| 454 | project | `PROJECT-0013` | `APPLICATION_FORM_POLICY_PARTS_EMPTY` | 400 BAD_REQUEST | 파트 섹션에는 파트를 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:32` |
| 455 | project | `PROJECT-0014` | `APPLICATION_FORM_INVALID_SECTION_ID` | 400 BAD_REQUEST | 현재 폼에 없는 섹션이에요. 섹션을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:33` |
| 456 | project | `PROJECT-0015` | `APPLICATION_FORM_INVALID_QUESTION_ID` | 400 BAD_REQUEST | 해당 섹션에 없는 질문이에요. 질문을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:34` |
| 457 | project | `PROJECT-0016` | `APPLICATION_FORM_INVALID_OPTION_ID` | 400 BAD_REQUEST | 해당 질문에 없는 선택지예요. 선택지를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:35` |
| 458 | project | `PROJECT-0017` | `APPLICATION_FORM_OPTIONS_NOT_ALLOWED` | 400 BAD_REQUEST | 선택형 질문에만 선택지를 추가할 수 있어요. 질문 유형을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:36` |
| 459 | project | `PROJECT-0018` | `APPLICATION_FORM_OPTIONS_REQUIRED` | 400 BAD_REQUEST | 선택형 질문에는 선택지가 1개 이상 필요해요. 선택지를 추가해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:37` |
| 460 | project | `PROJECT-0019` | `APPLICATION_DRAFT_NOT_EXPOSABLE` | 500 INTERNAL_SERVER_ERROR | 임시저장 지원서를 운영진 응답으로 보여줄 수 없어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:22` |
| 461 | project | `PROJECT-0020` | `APPLICATION_DRAFT_FILTER_NOT_ALLOWED` | 400 BAD_REQUEST | 운영진 지원자 목록에서는 임시저장 상태를 필터로 사용할 수 없어요. 다른 상태를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:24` |
| 462 | project | `PROJECT-0021` | `PROJECT_APPLICATION_NOT_FOUND` | 404 NOT_FOUND | 지원서를 찾을 수 없어요. 선택한 지원서를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:26` |
| 463 | project | `PROJECT-0022` | `PROJECT_DELETE_NOT_ALLOWED_IN_STATUS` | 409 CONFLICT | 프로젝트는 DRAFT 또는 PENDING_REVIEW 상태에서만 삭제할 수 있어요. 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:46` |
| 464 | project | `PROJECT-0023` | `PROJECT_ABORT_REASON_REQUIRED` | 400 BAD_REQUEST | 프로젝트 중단 사유를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:48` |
| 465 | project | `PROJECT-0100` | `PROJECT_MEMBER_NOT_FOUND` | 404 NOT_FOUND | 프로젝트 멤버를 찾을 수 없어요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:51` |
| 466 | project | `PROJECT-0101` | `PROJECT_MEMBER_ALREADY_EXISTS` | 409 CONFLICT | 이미 이 프로젝트의 멤버예요. 멤버 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:52` |
| 467 | project | `PROJECT-0102` | `PROJECT_MAIN_PM_REMOVAL_REQUIRES_TRANSFER` | 400 BAD_REQUEST | 메인 PM은 팀원 제거가 아니라 소유권 양도로 변경해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:53` |
| 468 | project | `PROJECT-0200` | `PROJECT_PART_QUOTA_INVALID` | 400 BAD_REQUEST | 파트 정원은 1명 이상으로 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:56` |
| 469 | project | `PROJECT-0202` | `PROJECT_PART_QUOTA_REQUIRED` | 400 BAD_REQUEST | 프로젝트를 공개하려면 파트별 정원을 1개 이상 등록해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:57` |
| 470 | project | `PROJECT-0203` | `PROJECT_PART_QUOTA_DUPLICATE` | 400 BAD_REQUEST | 동일한 파트가 중복됐어요. 파트별 정원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:58` |
| 471 | project | `PROJECT-0204` | `PROJECT_DRAFT_APPLICATION_NOT_FOUND` | 404 NOT_FOUND | 작성 중인 지원서를 찾을 수 없어요. 지원서 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:86` |
| 472 | project | `PROJECT-0205` | `PROJECT_APPLICATION_PART_NOT_ALLOWED` | 403 FORBIDDEN | 이 프로젝트에 지원할 수 있는 파트가 아니에요. 지원 가능한 파트를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:87` |
| 473 | project | `PROJECT-0206` | `PROJECT_APPLICATION_MEMBER_ALREADY_IN_TEAM` | 409 CONFLICT | 이미 해당 기수에 소속된 팀이 있어 지원할 수 없어요. 팀 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:88` |
| 474 | project | `PROJECT-0207` | `PROJECT_APPLICATION_DUPLICATE_SUBMISSION` | 409 CONFLICT | 동일한 매칭 차수에 이미 제출한 지원서가 있어요. 기존 지원서를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:89` |
| 475 | project | `PROJECT-0208` | `PROJECT_APPLICATION_ROUND_NOT_OPEN` | 400 BAD_REQUEST | 현재는 해당 매칭 차수의 지원 기간이 아니에요. 지원 기간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:90` |
| 476 | project | `PROJECT-0209` | `PROJECT_APPLICATION_ROUND_TYPE_MISMATCH` | 400 BAD_REQUEST | 선택한 매칭 차수가 내 파트와 맞지 않아요. 매칭 차수를 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:91` |
| 477 | project | `PROJECT-0210` | `PROJECT_APPLICATION_ALREADY_EXISTS` | 409 CONFLICT | 이미 작성 중인 지원서가 있어요. 기존 지원서를 이어서 작성해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:92` |
| 478 | project | `PROJECT-0211` | `PROJECT_APPLICATION_SELF_APPLY_NOT_ALLOWED` | 403 FORBIDDEN | 내가 운영하는 프로젝트에는 지원할 수 없어요. 다른 프로젝트를 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:93` |
| 479 | project | `PROJECT-0212` | `PROJECT_APPLICATION_DECISION_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 상태에서는 합격 여부를 변경할 수 없어요. 지원서 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:94` |
| 480 | project | `PROJECT-0213` | `PROJECT_APPLICATION_QUOTA_EXCEEDED` | 409 CONFLICT | 해당 파트의 남은 자리를 초과해 합격 처리할 수 없어요. 파트 정원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:96` |
| 481 | project | `PROJECT-0214` | `PROJECT_APPLICATION_CANCEL_NOT_ALLOWED` | 400 BAD_REQUEST | 이미 종결된 지원서는 철회할 수 없어요. 지원서 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:98` |
| 482 | project | `PROJECT-0215` | `PROJECT_APPLICATION_CANCEL_ROUND_CLOSED` | 400 BAD_REQUEST | 매칭 차수가 종료되어 지원서를 철회할 수 없어요. 차수 기간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:99` |
| 483 | project | `PROJECT-0216` | `PROJECT_APPLICATION_MINIMUM_SELECTION_REQUIRED` | 409 CONFLICT | 매칭 규칙의 최소 선발 인원을 충족하지 않아 불합격 처리할 수 없어요. 합격 인원을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:100` |
| 484 | project | `PROJECT-0300` | `PROJECT_MATCHING_ROUND_NOT_FOUND` | 404 NOT_FOUND | 매칭 차수를 찾을 수 없어요. 선택한 차수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:61` |
| 485 | project | `PROJECT-0301` | `PROJECT_MATCHING_ROUND_INVALID_PERIOD` | 400 BAD_REQUEST | 매칭 차수 기간은 시작, 종료, 결정 마감 순서여야 해요. 시간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:62` |
| 486 | project | `PROJECT-0302` | `PROJECT_MATCHING_ROUND_PERIOD_OVERLAPPED` | 409 CONFLICT | 같은 지부의 다른 매칭 차수와 기간이 겹쳐요. 기간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:64` |
| 487 | project | `PROJECT-0303` | `PROJECT_MATCHING_ROUND_ACCESS_DENIED` | 403 FORBIDDEN | 이 매칭 차수를 관리할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:66` |
| 488 | project | `PROJECT-0304` | `PROJECT_MATCHING_ROUND_DELETE_CONFLICT` | 409 CONFLICT | 연결된 지원서가 있는 매칭 차수는 삭제할 수 없어요. 지원서를 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:68` |
| 489 | project | `PROJECT-0305` | `PROJECT_MATCHING_ROUND_TIME_REQUIRES_CHAPTER` | 400 BAD_REQUEST | 시간 기준으로 조회하려면 지부를 함께 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:70` |
| 490 | project | `PROJECT-0306` | `PROJECT_MATCHING_ROUND_LOCKED` | 400 BAD_REQUEST | 결정 마감 시각이 지나 결정을 변경할 수 없어요. 차수 기간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:72` |
| 491 | project | `PROJECT-0307` | `PROJECT_MATCHING_ROUND_NOT_FINALIZABLE` | 400 BAD_REQUEST | 결정 마감 시각이 지난 뒤 자동 선발을 실행할 수 있어요. 마감 시각을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:78` |
| 492 | project | `PROJECT-0308` | `PROJECT_MATCHING_ROUND_POLICY_NOT_FOUND` | 500 INTERNAL_SERVER_ERROR | 이 매칭 종류의 자동 선발 정책을 찾지 못했어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:80` |
| 493 | project | `PROJECT-0309` | `PROJECT_MATCHING_ROUND_PHASE_SEQUENCE_INVALID` | 409 CONFLICT | 매칭 차수는 FIRST, SECOND, THIRD 순서로 배치하고 이전 차수 결정 마감 이후 1분 이상 간격을 둬야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:82` |
| 494 | project | `PROJECT-0310` | `PROJECT_MATCHING_ROUND_NOT_ENDED` | 400 BAD_REQUEST | 아직 지원 기간이 끝나지 않아 결정을 변경할 수 없어요. 지원 종료 시각을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:74` |
| 495 | project | `PROJECT-0311` | `PROJECT_MATCHING_ROUND_APPLICANTS_NOT_VIEWABLE` | 400 BAD_REQUEST | 아직 지원 기간이 끝나지 않아 지원서를 조회할 수 없어요. 지원 종료 시각을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/project/domain/exception/ProjectErrorCode.java:76` |

## recruiting

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 496 | recruiting | `RECRUITING-0001` | `RECRUITING_SEASON_NOT_FOUND` | 404 NOT_FOUND | 모집 시즌을 찾을 수 없어요. 모집 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:14` |
| 497 | recruiting | `RECRUITING-0002` | `RECRUITING_ROUND_NOT_FOUND` | 404 NOT_FOUND | 모집 차수를 찾을 수 없어요. 모집 차수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:15` |
| 498 | recruiting | `RECRUITING-0003` | `RECRUITING_APPLICATION_FORM_NOT_FOUND` | 404 NOT_FOUND | 지원 폼을 찾을 수 없어요. 지원 폼을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:16` |
| 499 | recruiting | `RECRUITING-0004` | `RECRUITING_APPLICATION_NOT_FOUND` | 404 NOT_FOUND | 지원서를 찾을 수 없어요. 지원서 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:17` |
| 500 | recruiting | `RECRUITING-0006` | `RECRUITING_INTERVIEW_SCHEDULE_NOT_FOUND` | 404 NOT_FOUND | 면접 일정을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:18` |
| 501 | recruiting | `RECRUITING-0007` | `RECRUITING_APPLICATION_EVALUATION_NOT_FOUND` | 404 NOT_FOUND | 지원서 평가를 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:19` |
| 502 | recruiting | `RECRUITING-0008` | `RECRUITING_ROUND_EVALUATOR_NOT_FOUND` | 404 NOT_FOUND | 모집 차수 평가자를 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:20` |
| 503 | recruiting | `RECRUITING-0009` | `RECRUITING_ROUND_INTERVIEW_QUESTION_NOT_FOUND` | 404 NOT_FOUND | 공통 면접 질문을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:21` |
| 504 | recruiting | `RECRUITING-0010` | `RECRUITING_APPLICATION_INTERVIEW_QUESTION_NOT_FOUND` | 404 NOT_FOUND | 개별 면접 질문을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:22` |
| 505 | recruiting | `RECRUITING-0100` | `RECRUITING_ROUND_INVALID_ROUND_NO` | 400 BAD_REQUEST | 추가모집 차수는 1 이상이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:23` |
| 506 | recruiting | `RECRUITING-0101` | `RECRUITING_SEASON_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 모집 시즌 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:24` |
| 507 | recruiting | `RECRUITING-0102` | `RECRUITING_ROUND_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 모집 차수 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:25` |
| 508 | recruiting | `RECRUITING-0103` | `RECRUITING_SEASON_ALREADY_EXISTS` | 409 CONFLICT | 이미 같은 학교와 기수의 모집 시즌이 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:26` |
| 509 | recruiting | `RECRUITING-0104` | `RECRUITING_ROUND_ALREADY_EXISTS` | 409 CONFLICT | 이미 같은 시즌의 모집 차수가 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:27` |
| 510 | recruiting | `RECRUITING-0105` | `RECRUITING_SEASON_REQUIRED_FIELD` | 400 BAD_REQUEST | 모집 시즌 생성에는 기수와 학교가 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:28` |
| 511 | recruiting | `RECRUITING-0106` | `RECRUITING_QUOTA_INVALID_TARGET_COUNT` | 400 BAD_REQUEST | 모집 목표 인원은 0명 이상이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:29` |
| 512 | recruiting | `RECRUITING-0107` | `RECRUITING_QUOTA_UNSUPPORTED_TRACK` | 400 BAD_REQUEST | 해당 트랙은 모집 목표 인원을 설정할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:31` |
| 513 | recruiting | `RECRUITING-0108` | `RECRUITING_QUOTA_DUPLICATE_TRACK` | 409 CONFLICT | 같은 트랙의 모집 목표 인원을 중복 설정할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:32` |
| 514 | recruiting | `RECRUITING-0109` | `RECRUITING_ROUND_INVALID_TRACKS` | 400 BAD_REQUEST | 모집 차수의 트랙 구성이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:33` |
| 515 | recruiting | `RECRUITING-0110` | `RECRUITING_ROUND_TRACK_NOT_IN_SEASON` | 400 BAD_REQUEST | 모집 차수 트랙은 시즌 모집 트랙에 포함되어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:34` |
| 516 | recruiting | `RECRUITING-0111` | `RECRUITING_ROUND_INVALID_SCHEDULE` | 400 BAD_REQUEST | 모집 차수 일정 순서가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:35` |
| 517 | recruiting | `RECRUITING-0112` | `RECRUITING_SEASON_CREATION_FORBIDDEN` | 403 FORBIDDEN | 해당 기수와 학교의 모집 시즌을 생성할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:36` |
| 518 | recruiting | `RECRUITING-0113` | `RECRUITING_ROUND_RECRUITMENT_POLICY_LOCKED` | 409 CONFLICT | 지원서가 있거나 모집 중인 차수의 트랙과 2지망 정책은 변경할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:37` |
| 519 | recruiting | `RECRUITING-0114` | `RECRUITING_ROUND_INVALID_TITLE` | 400 BAD_REQUEST | 모집 제목은 1자 이상 100자 이하여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:38` |
| 520 | recruiting | `RECRUITING-0115` | `RECRUITING_ROUND_TITLE_ALREADY_EXISTS` | 409 CONFLICT | 같은 시즌에 동일한 모집 제목이 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:39` |
| 521 | recruiting | `RECRUITING-0116` | `RECRUITING_ROUND_NO_SEQUENCE_CONFLICT` | 409 CONFLICT | 추가모집 차수는 이전 차수 다음 번호여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:40` |
| 522 | recruiting | `RECRUITING-0117` | `RECRUITING_ROUND_DELETE_CONFLICT` | 409 CONFLICT | 초안이며 지원서와 Form 응답이 없는 모집만 삭제할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:41` |
| 523 | recruiting | `RECRUITING-0118` | `RECRUITING_ROUND_UNPUBLISH_CONFLICT` | 409 CONFLICT | 지원서 또는 Form 응답이 있는 모집은 비공개할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:42` |
| 524 | recruiting | `RECRUITING-0119` | `RECRUITING_CHAPTER_QUOTA_INVALID_TARGET_COUNT` | 400 BAD_REQUEST | 지부 전체 모집 목표 인원은 0명 이상이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:30` |
| 525 | recruiting | `RECRUITING-0120` | `RECRUITING_ROUND_NOT_DELETED` | 409 CONFLICT | 삭제된 모집만 복구할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:43` |
| 526 | recruiting | `RECRUITING-0121` | `RECRUITING_ROUND_RESTORE_CONFLICT` | 409 CONFLICT | 같은 번호나 제목의 모집이 이미 있어 복구할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:44` |
| 527 | recruiting | `RECRUITING-0200` | `RECRUITING_APPLICATION_FORM_INVALID` | 400 BAD_REQUEST | 지원 폼 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:45` |
| 528 | recruiting | `RECRUITING-0201` | `RECRUITING_APPLICATION_FORM_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 지원 폼 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:46` |
| 529 | recruiting | `RECRUITING-0202` | `RECRUITING_APPLICATION_FORM_ALREADY_EXISTS` | 409 CONFLICT | 이미 해당 모집 차수에 연결된 지원 폼이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:47` |
| 530 | recruiting | `RECRUITING-0203` | `RECRUITING_APPLICATION_FORM_NOT_PUBLISHED` | 400 BAD_REQUEST | 게시된 지원 폼에만 지원할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:48` |
| 531 | recruiting | `RECRUITING-0204` | `RECRUITING_FORM_SECTION_POLICY_INVALID` | 400 BAD_REQUEST | 지원 폼 섹션 정책이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:49` |
| 532 | recruiting | `RECRUITING-0205` | `RECRUITING_FORM_SECTION_POLICY_INVALID_TRACK` | 400 BAD_REQUEST | 지원 폼 섹션의 모집 트랙이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:50` |
| 533 | recruiting | `RECRUITING-0206` | `RECRUITING_APPLICATION_FORM_TRACK_SECTION_REQUIRED` | 400 BAD_REQUEST | 모든 모집 트랙에 해당하는 지원 폼 섹션이 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:51` |
| 534 | recruiting | `RECRUITING-0300` | `RECRUITING_APPLICATION_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 지원서 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:52` |
| 535 | recruiting | `RECRUITING-0301` | `RECRUITING_APPLICATION_ALREADY_EXISTS` | 409 CONFLICT | 이미 같은 모집 차수에 제출한 지원서가 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:53` |
| 536 | recruiting | `RECRUITING-0302` | `RECRUITING_APPLICATION_DIFFERENT_SCHOOL_EXISTS` | 409 CONFLICT | 같은 기수의 다른 학교 모집에 이미 지원했어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:54` |
| 537 | recruiting | `RECRUITING-0303` | `RECRUITING_APPLICATION_REAPPLICATION_BLOCKED` | 409 CONFLICT | 진행 중이거나 합격한 지원서가 있어 재지원할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:55` |
| 538 | recruiting | `RECRUITING-0304` | `RECRUITING_APPLICATION_FINAL_PASS_ALREADY_EXISTS` | 409 CONFLICT | 이미 최종 합격한 지원서가 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:56` |
| 539 | recruiting | `RECRUITING-0305` | `RECRUITING_REGISTRATION_FORBIDDEN` | 403 FORBIDDEN | 중앙운영사무국 총괄단 이상만 최종 등록을 확정할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:57` |
| 540 | recruiting | `RECRUITING-0306` | `RECRUITING_APPLICATION_MEMBER_REQUIRED` | 400 BAD_REQUEST | 챌린저 등록에는 연결된 회원 정보가 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:58` |
| 541 | recruiting | `RECRUITING-0307` | `RECRUITING_APPLICATION_REQUIRED_FIELD` | 400 BAD_REQUEST | 지원서 필수 정보가 누락되었어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:59` |
| 542 | recruiting | `RECRUITING-0308` | `RECRUITING_APPLICATION_INVALID_APPLICANT_NAME` | 400 BAD_REQUEST | 지원자 이름에는 공백을 사용할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:60` |
| 543 | recruiting | `RECRUITING-0309` | `RECRUITING_APPLICATION_INVALID_EMAIL` | 400 BAD_REQUEST | 지원자 이메일이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:61` |
| 544 | recruiting | `RECRUITING-0310` | `RECRUITING_APPLICATION_INVALID_FIRST_CHOICE` | 400 BAD_REQUEST | 1지망은 현재 모집 중인 트랙이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:62` |
| 545 | recruiting | `RECRUITING-0311` | `RECRUITING_APPLICATION_INVALID_SECOND_CHOICE` | 400 BAD_REQUEST | 2지망 선택이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:63` |
| 546 | recruiting | `RECRUITING-0312` | `RECRUITING_APPLICATION_INVALID_ACCEPTED_TRACK` | 400 BAD_REQUEST | 합격 트랙은 지원한 트랙 중 하나여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:64` |
| 547 | recruiting | `RECRUITING-0313` | `RECRUITING_APPLICATION_INVALID_PRIVACY_CONSENT` | 400 BAD_REQUEST | 개인정보 동의 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:65` |
| 548 | recruiting | `RECRUITING-0314` | `RECRUITING_APPLICATION_INVALID_KEY` | 400 BAD_REQUEST | 지원 키 형식이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:66` |
| 549 | recruiting | `RECRUITING-0315` | `RECRUITING_APPLICATION_KEY_ISSUE_FAILED` | 409 CONFLICT | 지원 키를 발급하지 못했어요. 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:67` |
| 550 | recruiting | `RECRUITING-0316` | `RECRUITING_APPLICATION_APPLICANT_MISMATCH` | 403 FORBIDDEN | 본인의 지원서만 변경할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:68` |
| 551 | recruiting | `RECRUITING-0317` | `RECRUITING_FINAL_DECISION_FORBIDDEN` | 403 FORBIDDEN | 학교 회장단 또는 중앙 총괄단만 최종 판정할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:69` |
| 552 | recruiting | `RECRUITING-0318` | `RECRUITING_QUOTA_NOT_FOUND` | 404 NOT_FOUND | 합격 트랙의 모집 정원을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:70` |
| 553 | recruiting | `RECRUITING-0319` | `RECRUITING_QUOTA_EXCEEDED` | 409 CONFLICT | 합격 트랙의 모집 정원이 모두 예약되었어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:71` |
| 554 | recruiting | `RECRUITING-0320` | `RECRUITING_QUOTA_BELOW_RESERVED` | 409 CONFLICT | 모집 정원을 현재 예약 및 등록 인원보다 작게 줄일 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:72` |
| 555 | recruiting | `RECRUITING-0321` | `RECRUITING_APPLICATION_PERIOD_CLOSED` | 400 BAD_REQUEST | 현재 지원서를 작성하거나 제출할 수 있는 기간이 아니에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:74` |
| 556 | recruiting | `RECRUITING-0322` | `RECRUITING_CONCURRENCY_LOCK_TIMEOUT` | 409 CONFLICT | 동시 요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:75` |
| 557 | recruiting | `RECRUITING-0323` | `RECRUITING_SUMMARY_ACCESS_DENIED` | 403 FORBIDDEN | 해당 기수의 지원 현황을 조회할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:76` |
| 558 | recruiting | `RECRUITING-0324` | `RECRUITING_APPLICATION_ANSWER_OUT_OF_SCOPE` | 400 BAD_REQUEST | 선택한 지원 트랙에 포함되지 않은 문항에는 응답할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:77` |
| 559 | recruiting | `RECRUITING-0325` | `RECRUITING_EVALUATION_STATISTICS_ACCESS_DENIED` | 403 FORBIDDEN | 해당 기수의 평가 현황을 조회할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:78` |
| 560 | recruiting | `RECRUITING-0326` | `RECRUITING_DECISION_HISTORY_ACCESS_DENIED` | 403 FORBIDDEN | 해당 기수의 평가 이력을 조회할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:79` |
| 561 | recruiting | `RECRUITING-0327` | `RECRUITING_DECISION_HISTORY_INVALID` | 400 BAD_REQUEST | 판정 이력 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:80` |
| 562 | recruiting | `RECRUITING-0328` | `RECRUITING_DECISION_HISTORY_EXPORT_TOO_LARGE` | 400 BAD_REQUEST | 다운로드 대상이 너무 많아요. 지부·학교·트랙 등 조건을 좁혀주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:81` |
| 563 | recruiting | `RECRUITING-0329` | `RECRUITING_DECISION_HISTORY_INVALID_PAGE_SIZE` | 400 BAD_REQUEST | 평가 이력 조회 size는 1 이상 100 이하여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:82` |
| 564 | recruiting | `RECRUITING-0330` | `RECRUITING_CHAPTER_QUOTA_TOTAL_MISMATCH` | 400 BAD_REQUEST | 지부 전체 모집 목표 인원은 지부 학교별 파트 목표 인원의 합계와 같아야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:73` |
| 565 | recruiting | `RECRUITING-0401` | `RECRUITING_EVALUATION_ALREADY_SUBMITTED` | 409 CONFLICT | 이미 확정한 지원자 평가가 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:83` |
| 566 | recruiting | `RECRUITING-0402` | `RECRUITING_EVALUATION_ACCESS_DENIED` | 403 FORBIDDEN | 지원자 평가를 조회할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:84` |
| 567 | recruiting | `RECRUITING-0403` | `RECRUITING_EVALUATION_INVALID` | 400 BAD_REQUEST | 지원자 평가 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:85` |
| 568 | recruiting | `RECRUITING-0404` | `RECRUITING_EVALUATION_DECISION_REQUIRED` | 400 BAD_REQUEST | 평가 확정에는 결정이 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:86` |
| 569 | recruiting | `RECRUITING-0405` | `RECRUITING_EVALUATION_COMMENT_TOO_LONG` | 400 BAD_REQUEST | 평가 의견은 2000자 이하여야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:87` |
| 570 | recruiting | `RECRUITING-0410` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID` | 400 BAD_REQUEST | 면접 일정 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:88` |
| 571 | recruiting | `RECRUITING-0411` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_TRANSITION` | 400 BAD_REQUEST | 현재 면접 일정 상태에서는 할 수 없는 작업이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:89` |
| 572 | recruiting | `RECRUITING-0412` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_RESPONSE` | 400 BAD_REQUEST | 면접 가능 시간 응답 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:90` |
| 573 | recruiting | `RECRUITING-0413` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_PERIOD` | 400 BAD_REQUEST | 면접 일정 시간이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:91` |
| 574 | recruiting | `RECRUITING-0414` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_LOCATION` | 400 BAD_REQUEST | 면접 장소가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:92` |
| 575 | recruiting | `RECRUITING-0415` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_CONTACT` | 400 BAD_REQUEST | 면접 연락처가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:93` |
| 576 | recruiting | `RECRUITING-0416` | `RECRUITING_INTERVIEW_SCHEDULE_INVALID_MAIL_STATE` | 400 BAD_REQUEST | 면접 안내 메일 상태가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:94` |
| 577 | recruiting | `RECRUITING-0417` | `RECRUITING_INTERVIEW_SCHEDULE_ALREADY_EXISTS` | 409 CONFLICT | 이미 면접 일정이 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:95` |
| 578 | recruiting | `RECRUITING-0418` | `RECRUITING_INTERVIEW_SCHEDULE_ACCESS_DENIED` | 403 FORBIDDEN | 면접 일정을 조회할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:96` |
| 579 | recruiting | `RECRUITING-0419` | `RECRUITING_INTERVIEW_AVAILABILITY_NOT_IMPLEMENTED` | 501 NOT_IMPLEMENTED | 면접 가능 일정 응답 제출 기능은 아직 사용할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:97` |
| 580 | recruiting | `RECRUITING-0420` | `RECRUITING_INTERVIEW_SESSION_NOT_FOUND` | 404 NOT_FOUND | 면접 세션을 찾을 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:98` |
| 581 | recruiting | `RECRUITING-0421` | `RECRUITING_INTERVIEW_SESSION_INVALID` | 400 BAD_REQUEST | 면접 세션 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:99` |
| 582 | recruiting | `RECRUITING-0422` | `RECRUITING_INTERVIEW_SESSION_INVALID_SLOT` | 400 BAD_REQUEST | 면접 세션 슬롯이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:100` |
| 583 | recruiting | `RECRUITING-0423` | `RECRUITING_INTERVIEW_SESSION_CONFIRMED_SCHEDULE_EXISTS` | 409 CONFLICT | 확정된 면접 일정이 있는 세션은 변경할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:101` |
| 584 | recruiting | `RECRUITING-0424` | `RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_CONFLICT` | 409 CONFLICT | 이미 확정된 면접 세션 슬롯이에요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:102` |
| 585 | recruiting | `RECRUITING-0425` | `RECRUITING_INTERVIEW_SCHEDULE_ASSIGNMENT_LOCK_TIMEOUT` | 409 CONFLICT | 면접 일정 배정 잠금을 획득하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:103` |
| 586 | recruiting | `RECRUITING-0500` | `RECRUITING_ROUND_EVALUATOR_INVALID` | 400 BAD_REQUEST | 모집 차수 평가자 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:104` |
| 587 | recruiting | `RECRUITING-0501` | `RECRUITING_ROUND_EVALUATOR_ALREADY_EXISTS` | 409 CONFLICT | 이미 해당 모집 차수의 평가자로 등록되어 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:105` |
| 588 | recruiting | `RECRUITING-0502` | `RECRUITING_INTERVIEW_QUESTION_INVALID_TARGET` | 400 BAD_REQUEST | 면접 질문 대상이 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:106` |
| 589 | recruiting | `RECRUITING-0503` | `RECRUITING_INTERVIEW_QUESTION_INVALID_CONTENT` | 400 BAD_REQUEST | 면접 질문 내용을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:107` |
| 590 | recruiting | `RECRUITING-0504` | `RECRUITING_INTERVIEW_QUESTION_INVALID_ORDER_NO` | 400 BAD_REQUEST | 면접 질문 순서는 0 이상이어야 해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:108` |
| 591 | recruiting | `RECRUITING-0505` | `RECRUITING_INTERVIEW_QUESTION_IMMUTABLE` | 409 CONFLICT | 면접 평가 제출 후에는 질문을 변경할 수 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:109` |
| 592 | recruiting | `RECRUITING-0506` | `RECRUITING_INTERVIEW_QUESTION_ACCESS_DENIED` | 403 FORBIDDEN | 면접 질문을 변경할 권한이 없어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:110` |
| 593 | recruiting | `RECRUITING-0507` | `RECRUITING_INTERVIEW_QUESTION_INVALID_ACTOR` | 400 BAD_REQUEST | 면접 질문 변경자 정보가 올바르지 않아요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:111` |
| 594 | recruiting | `RECRUITING-0508` | `RECRUITING_APPLICANT_IDENTITY_REQUIRED` | 500 INTERNAL_SERVER_ERROR | 지원자를 식별하려면 기수와 회원 또는 이메일이 필요해요. |  |  |  | false |  |  | `src/main/java/com/umc/product/recruiting/domain/exception/RecruitingErrorCode.java:112` |

## schedule

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 595 | schedule | `SCHEDULE-0006` | `INVALID_TIME_RANGE` | 400 BAD_REQUEST | 시작 시간은 종료 시간보다 빨라야 해요. 시간을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:14` |
| 596 | schedule | `SCHEDULE-0009` | `SCHEDULE_NOT_FOUND` | 404 NOT_FOUND | 일정을 찾을 수 없어요. 선택한 일정을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:16` |
| 597 | schedule | `SCHEDULE-0010` | `TAG_REQUIRED` | 400 BAD_REQUEST | 태그를 1개 이상 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:18` |
| 598 | schedule | `SCHEDULE-0011` | `NOT_FIRST_ATTENDANCE_REQUEST` | 400 BAD_REQUEST | 이미 출석 요청이 있어요. 기존 요청을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:20` |
| 599 | schedule | `SCHEDULE-0012` | `NO_ATTENDANCE_RECORD` | 404 NOT_FOUND | 출석 요청이 없어요. 출석 요청을 먼저 생성해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:22` |
| 600 | schedule | `SCHEDULE-0013` | `INVALID_ATTENDANCE_STATUS_FOR_EXCUSE` | 400 BAD_REQUEST | 첫 요청, 결석 또는 지각 상태에서만 출석 사유를 제출할 수 있어요. 출석 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:24` |
| 601 | schedule | `SCHEDULE-0014` | `INVALID_ATTENDANCE_STATUS_FOR_APPROVAL` | 400 BAD_REQUEST | 현재 출석 상태에서는 승인할 수 없어요. 출석 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:27` |
| 602 | schedule | `SCHEDULE-0015` | `INVALID_ATTENDANCE_STATUS_FOR_REJECT` | 400 BAD_REQUEST | 현재 출석 상태에서는 거절할 수 없어요. 출석 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:29` |
| 603 | schedule | `SCHEDULE-0016` | `NO_EXCUSE_REASON_GIVEN` | 400 BAD_REQUEST | 출석 인정을 요청하려면 사유를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:31` |
| 604 | schedule | `SCHEDULE-0017` | `ATTENDANCE_NOT_REQUIRES_CONFIRM` | 400 BAD_REQUEST | 운영진 확인이 필요한 출석 요청이 아니에요. 출석 상태를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:33` |
| 605 | schedule | `SCHEDULE-0018` | `SCHEDULE_ENDED` | 400 BAD_REQUEST | 종료된 일정에는 출석을 요청할 수 없어요. 일정 시간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:36` |
| 606 | schedule | `SCHEDULE-0019` | `CHECK_IN_TOO_EARLY` | 400 BAD_REQUEST | 아직 출석할 수 있는 시간이 아니에요. 출석 가능 시간 이후에 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:38` |
| 607 | schedule | `SCHEDULE-0020` | `OFFLINE_SCHEDULE_REQUIRES_LOCATION` | 400 BAD_REQUEST | 대면 일정에는 위치 정보가 필요해요. 위치를 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:40` |
| 608 | schedule | `SCHEDULE-0021` | `SCHEDULE_ATTENDANCE_POLICY_NOT_EXIST` | 400 BAD_REQUEST | 출석 정책이 없는 일정이에요. 출석 정책을 먼저 설정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:42` |
| 609 | schedule | `SCHEDULE-0022` | `PARTICIPANT_NOT_FOUND` | 400 BAD_REQUEST | 일정 참석자 정보를 찾을 수 없어요. 참석자 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:44` |
| 610 | schedule | `SCHEDULE-0023` | `LOCATION_NOT_VERIFIED` | 400 BAD_REQUEST | 출석 인증 범위 안에 있는지 확인하지 못했어요. 위치를 확인한 뒤 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:46` |
| 611 | schedule | `SCHEDULE-0024` | `ONLINE_SCHEDULE_SHOULD_NOT_HAVE_LOCATION` | 400 BAD_REQUEST | 비대면 일정에는 위치 정보를 포함할 수 없어요. 위치 정보를 제거해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:48` |
| 612 | schedule | `SCHEDULE-0025` | `NOT_ACTIVE_GISU_SCHEDULE` | 400 BAD_REQUEST | 현재 기수의 일정만 만들 수 있어요. 기수를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:51` |
| 613 | schedule | `SCHEDULE-0026` | `NOT_SCHEDULE_PARTICIPANT` | 400 BAD_REQUEST | 일정 참여자만 출석할 수 있어요. 참여자 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:54` |
| 614 | schedule | `SCHEDULE-0027` | `ATTENDANCE_POLICY_REQUIRED` | 400 BAD_REQUEST | 출석이 필요한 일정에는 출석 정책을 설정해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:56` |
| 615 | schedule | `SCHEDULE-0028` | `STARTED_SCHEDULE_CANT_BE_EDITED` | 400 BAD_REQUEST | 이미 시작된 일정은 수정할 수 없어요. 일정 시간을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:58` |
| 616 | schedule | `SCHEDULE-0029` | `CANNOT_CREATE_SCHEDULE` | 403 FORBIDDEN | 일정을 만들려면 챌린저 활동 이력이 필요해요. 활동 기록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:60` |
| 617 | schedule | `SCHEDULE-0030` | `EXCEEDED_MAX_PARTICIPANTS` | 400 BAD_REQUEST | 초대 가능한 참여자 수를 초과했어요. 참여자를 줄여주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:62` |
| 618 | schedule | `SCHEDULE-0031` | `CANNOT_CREATE_ATTENDANCE_REQUIRED_SCHEDULE` | 403 FORBIDDEN | 출석이 필요한 일정을 만들 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:64` |
| 619 | schedule | `SCHEDULE-0032` | `INVALID_MEMBER_INVITE` | 400 BAD_REQUEST | 초대할 수 없는 참여자가 포함되어 있어요. 참여자 목록을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:67` |
| 620 | schedule | `SCHEDULE-0033` | `SCHEDULE_HAS_ATTENDANCE_RECORD` | 400 BAD_REQUEST | 출석 기록이 있는 일정은 삭제할 수 없어요. 출석 기록을 먼저 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/schedule/domain/exception/ScheduleErrorCode.java:69` |

## storage

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 621 | storage | `STORAGE-0001` | `FILE_NOT_FOUND` | 404 NOT_FOUND | 파일을 찾을 수 없어요. 선택한 파일을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:15` |
| 622 | storage | `STORAGE-0002` | `FILE_UPLOAD_NOT_COMPLETED` | 400 BAD_REQUEST | 파일 업로드가 아직 끝나지 않았어요. 업로드를 완료한 뒤 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:16` |
| 623 | storage | `STORAGE-0003` | `FILE_ALREADY_UPLOADED` | 400 BAD_REQUEST | 이미 업로드가 끝난 파일이에요. 파일 정보를 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:17` |
| 624 | storage | `STORAGE-0004` | `INVALID_FILE_EXTENSION` | 400 BAD_REQUEST | 지원하지 않는 파일 형식이에요. 다른 파일을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:23` |
| 625 | storage | `STORAGE-0005` | `FILE_SIZE_EXCEEDED` | 400 BAD_REQUEST | 파일 크기가 너무 커요. 더 작은 파일을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:24` |
| 626 | storage | `STORAGE-0006` | `INVALID_CONTENT_TYPE` | 400 BAD_REQUEST | 파일 형식 정보가 올바르지 않아요. 파일을 다시 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:25` |
| 627 | storage | `STORAGE-0007` | `STORAGE_UPLOAD_FAILED` | 500 INTERNAL_SERVER_ERROR | 파일을 업로드하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:29` |
| 628 | storage | `STORAGE-0008` | `STORAGE_DELETE_FAILED` | 500 INTERNAL_SERVER_ERROR | 파일을 삭제하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:30` |
| 629 | storage | `STORAGE-0009` | `STORAGE_URL_GENERATION_FAILED` | 500 INTERNAL_SERVER_ERROR | 파일 접근 링크를 만들지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:31` |
| 630 | storage | `STORAGE-0010` | `CDN_SIGNING_FAILED` | 500 INTERNAL_SERVER_ERROR | CDN 접근 링크를 만들지 못했어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:36` |
| 631 | storage | `STORAGE-0011` | `NO_ENV_KEYS` | 500 INTERNAL_SERVER_ERROR | CDN 설정이 누락됐어요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:37` |
| 632 | storage | `STORAGE-0012` | `INVALID_SPRING_PROFILE` | 500 INTERNAL_SERVER_ERROR | 서버 실행 환경이 올바르지 않아요. 관리자에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:38` |
| 633 | storage | `STORAGE-0013` | `FILE_DELETE_FORBIDDEN` | 403 FORBIDDEN | 파일을 삭제할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:18` |
| 634 | storage | `STORAGE-0014` | `FILE_SIZE_MISMATCH` | 400 BAD_REQUEST | 요청한 파일 크기와 실제 업로드된 파일 크기가 달라요. 다시 업로드해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:26` |
| 635 | storage | `STORAGE-0015` | `STORAGE_METADATA_READ_FAILED` | 500 INTERNAL_SERVER_ERROR | 파일 정보를 확인하지 못했어요. 잠시 후 다시 시도해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:32` |
| 636 | storage | `STORAGE-0016` | `FILE_USE_FORBIDDEN` | 403 FORBIDDEN | 이 파일을 사용할 권한이 없어요. 본인이 업로드한 파일만 사용할 수 있어요. |  |  |  | false |  |  | `src/main/java/com/umc/product/storage/domain/exception/StorageErrorCode.java:20` |

## term

| 순번 | 도메인 | 코드 | 이름 | HTTP 상태 | 메시지 | 사용자 행동 | 재시도 | 심각도 | 사용 중단 | 담당자 | 태그 | 원본 |
|---:|---|---|---|---|---|---|---|---|---|---|---|---|
| 637 | term | `TERMS-0001` | `TERMS_NOT_FOUND` | 404 NOT_FOUND | 약관을 찾을 수 없어요. 선택한 약관을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:14` |
| 638 | term | `TERMS-0002` | `TERMS_TYPE_REQUIRED` | 400 BAD_REQUEST | 약관 타입을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:15` |
| 639 | term | `TERMS-0003` | `TERMS_TITLE_REQUIRED` | 400 BAD_REQUEST | 약관 제목을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:16` |
| 640 | term | `TERMS-0004` | `TERMS_CONTENT_REQUIRED` | 400 BAD_REQUEST | 약관 내용을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:17` |
| 641 | term | `TERMS-0005` | `TERMS_VERSION_REQUIRED` | 400 BAD_REQUEST | 약관 버전을 입력해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:18` |
| 642 | term | `TERMS-0006` | `TERMS_CONSENT_NOT_FOUND` | 404 NOT_FOUND | 약관 동의 정보를 찾을 수 없어요. 동의 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:20` |
| 643 | term | `TERMS-0007` | `TERMS_CONSENT_ALREADY_EXISTS` | 400 BAD_REQUEST | 이미 동의한 약관이에요. 동의 내역을 확인해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:21` |
| 644 | term | `TERMS-0008` | `MEMBER_ID_REQUIRED` | 400 BAD_REQUEST | 회원을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:22` |
| 645 | term | `TERMS-0009` | `TERM_ID_REQUIRED` | 400 BAD_REQUEST | 약관을 선택해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:23` |
| 646 | term | `TERMS-0010` | `MANDATORY_TERMS_NOT_AGREED` | 400 BAD_REQUEST | 필수 약관에 모두 동의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:24` |
| 647 | term | `TERMS-0011` | `TERM_PERMISSION_DENIED` | 403 FORBIDDEN | 약관을 관리할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요. |  |  |  | false |  |  | `src/main/java/com/umc/product/term/domain/exception/TermErrorCode.java:26` |

