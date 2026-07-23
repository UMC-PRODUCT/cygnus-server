# Term Domain

## 역할

`term` 도메인은 약관, 약관 버전, 필수 동의 여부, 회원의 약관 동의 상태를 관리한다.

## 책임

- 약관을 생성, 수정, 조회한다.
- 필수 약관과 선택 약관을 구분한다.
- 회원 가입과 재동의 흐름에서 약관 동의 상태를 검증한다.
- 약관 관리 권한을 평가한다.

## 경계

term은 약관 문서와 동의 상태를 소유한다. 회원 가입 절차는 `member` 도메인이 진행하지만, 필수 약관 충족 여부는 term의 공개 UseCase로 확인한다.

## 재동의 강제 계약

AccessToken은 발급 시점의 `requiredTermsAgreed` boolean snapshot을 가진다. claim이 없는 기존 토큰은 호환성을 위해 `true`로 처리한다. 요청마다 약관 DB를 조회하지 않으므로 새 필수 약관은 AccessToken TTL인 최대 1시간 뒤 반영될 수 있다. `TERMS_RECONSENT_ENFORCEMENT_ENABLED` 기본값은 `false`이며, 클라이언트 복구 흐름 배포 후 활성화한다.

미동의 사용자가 `TERMS-0012`를 받으면 다음 순서로 복구한다.

1. `GET /api/v1/terms/consent-status/me`로 누락 약관을 조회한다.
2. 누락된 각 활성 필수 약관을 `POST /api/v1/terms/agreements`의 `{termsId, isAgreed: true}` 형식으로 제출한다.
3. 기존 token renew API를 호출하고 새 AccessToken으로 교체한다. 기존 false token은 동의 저장 후에도 계속 차단된다.

명시적 REST 우회는 아래 method/path 조합뿐이다. 다른 `@Public` 쓰기 API와 관리자 회원 삭제는 우회하지 않는다.

| Method | Path | 목적 |
|---|---|---|
| `GET` | `/api/v1/terms`, `/api/v1/terms/**` | 약관 및 누락 동의 상태 조회 |
| `POST` | `/api/v1/terms/agreements` | 활성 필수 약관 재동의 |
| `POST` | `/api/v1/auth/token/renew` | 동의 완료 상태가 반영된 AccessToken 발급 |
| `POST` | `/api/v1/auth/logout`, `/api/v1/auth/sso/logout` | 일반·SSO 로그아웃 |
| `DELETE` | `/api/v1/member` | 본인 탈퇴 |
| 모든 method | `/actuator/health`, `/actuator/health/**`, `/error` | health 및 오류 처리 |
| 모든 method | `/docs`, `/docs/**`, `/docs-json`, `/docs-json/**`, `/webjars/markdown-it/**`, `/umc-logo.svg` | 문서 리소스 |

`/graphql`, `/graphiql`, `/graphiql/**`, `/ws/**`는 REST 필터에서 넘긴다. GraphQL 요청과 STOMP 메시지는 각각 채널 전용 interceptor가 처리한다.

- REST: HTTP 403과 일반 API 오류 body의 `code=TERMS-0012`
- GraphQL: resolver 실행 전 error의 `extensions.code=TERMS-0012`, `extensions.httpStatus=403`
- STOMP: CONNECT/STOMP/SEND/SUBSCRIBE에서 기존 JSON ERROR frame의 `code=TERMS-0012`; 동의 완료 연결도 AccessToken 만료 시 종료 후 재연결
- 재동의 저장: 비활성 또는 선택 약관은 HTTP 400 `TERMS-0013`; 동일 요청은 성공으로 멱등 처리

## UX Writing Notes

약관은 법적 의미가 있으므로 짧고 정확하게 쓴다. 사용자 행동은 `필수 약관에 모두 동의해주세요`, 운영자 작업은 `약관 제목을 입력해주세요`처럼 구체화한다.
