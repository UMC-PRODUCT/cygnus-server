# ADR-022: 약관 동의 버전 증빙 모델

## Status

Accepted

## Context

2026년 5월 기준 `term` 도메인은 약관을 `TermType` 단위로 활성/비활성 처리하고, 회원 동의는 `term_consent.member_id + term_type`으로 저장한다.

- 약관 생성: `TermCommandService#createTerms`는 같은 `TermType`의 기존 활성 약관을 비활성화하고 새 `Term`을 저장한다.
- OAuth 회원가입: `MemberService#register`는 회원 생성 후 `ManageTermAgreementUseCase#createTermConsent`를 호출한다.
- 이메일 회원가입: `EmailMemberRegisterService#registerInternal`은 필수 약관 동의 여부를 검증하지만 동의 저장을 호출하지 않는다.
- 동의 모델: `TermConsent`와 `TermConsentLog`는 `termType`만 저장하므로 “어떤 약관 row에 동의했는지”를 증빙할 수 없다.
- 조회 모델: `TermAgreementQueryService#getAgreedTermsByMemberId`는 저장된 `termType`으로 현재 활성 약관을 다시 조회한다. 과거 동의가 최신 활성 약관 동의처럼 보일 수 있다.

이 프로젝트는 Hexagonal Architecture를 따른다. `member` 도메인은 `term` 도메인의 내부 repository에 접근하지 않고, 공개 UseCase인 `ManageTermAgreementUseCase`만 호출해야 한다. 도메인 간 참조는 Aggregate 객체가 아니라 ID 참조로 유지한다.

### 문제점

1. 이메일 회원가입은 약관 동의가 저장되지 않는다. 가입 직후 JWT가 발급되지만 `term_consent`와 `term_consent_log`에는 증빙이 남지 않는다.
2. 현재 동의 데이터는 `termType` 기준이라 약관 변경 후 재동의 여부를 정확히 판단할 수 없다. 동일 타입의 새 약관이 만들어져도 기존 사용자의 동의가 어느 약관 버전에 대한 것인지 알 수 없다.
3. 약관 동의 조회가 현재 활성 약관을 기준으로 재해석된다. 운영/감사 화면에서 과거 동의를 최신 약관 동의로 오인할 가능성이 있다.
4. 재동의 미완료 사용자의 API 차단을 구현하려면 “현재 필수 활성 약관 ID”와 “회원이 동의한 약관 ID”를 비교할 수 있어야 한다.

### 결정이 필요한 이유

약관 변경 시 미동의 사용자의 API 호출 제한, 선택 동의 철회, 마케팅 수신 동의 관리, 감사 로그 확장을 안전하게 구현하려면 동의 증빙의 기준을 `TermType`에서 `Term` row ID로 올려야 한다. 이 작업을 뒤로 미루면 API 차단 로직을 추가하더라도 과거/현재 약관을 구분할 수 없어 잘못된 차단 또는 미차단이 발생한다.

## Decision

우리는 약관 동의를 `termId` 기준으로 증빙하도록 변경하기로 결정한다.

1. `term_consent`와 `term_consent_log`에 `term_id`를 추가한다.
2. 신규 동의 저장 시 `TermAgreementCommandService`는 조회한 `Term`의 `id`와 `type`을 함께 저장한다.
3. 중복 동의 검사는 `memberId + termType`이 아니라 `memberId + termId` 기준으로 수행한다. 이렇게 해야 같은 `TermType`의 새 약관 재동의를 저장할 수 있다.
4. 동의 약관 조회는 저장된 `termId`로 원본 약관을 조회한다. 현재 활성 약관으로 재해석하지 않는다.
5. 이메일 회원가입도 OAuth 회원가입과 동일하게 `ManageTermAgreementUseCase`를 통해 동의 정보를 저장한다.
6. AccessToken 발급 시점에 현재 활성 필수 약관 충족 여부를 `requiredTermsAgreed` boolean snapshot claim으로 저장한다. 약관 ID 목록은 토큰에 넣지 않는다.
7. 재동의 차단은 REST, GraphQL, STOMP에 동일하게 적용하되 `app.terms.reconsent.enabled` feature flag로 실제 차단만 제어한다. claim은 flag와 무관하게 항상 발급한다.
8. 재동의 저장은 회원가입 동의와 분리된 UseCase로 처리한다. 대상 약관 row를 shared lock으로 조회해 활성 상태 검증과 비활성화를 직렬화하고, `member_id + term_id` unique index와 `INSERT ... ON CONFLICT DO NOTHING`으로 반복·동시 요청을 멱등 처리한다. 실제 신규 row가 저장된 경우에만 법적 동의 로그를 남긴다.
9. 토큰은 동의 상태의 snapshot이므로 재동의 완료 후에도 기존 AccessToken은 갱신하지 않는다. 클라이언트는 기존 token renew API로 새 AccessToken을 받아야 한다.

### 단계적 진행 / PR 분할

- **Phase 1 (이 PR / 본 ADR)**: 이메일 회원가입 동의 저장 누락 수정, `term_consent`/`term_consent_log.term_id` 추가, 신규 동의 저장과 조회를 `termId` 기준으로 전환한다.
- **Phase 2 (별도 PR)**: 현재 활성 필수 약관 대비 회원의 미동의 상태를 계산하는 Query UseCase를 추가한다.
- **Phase 3 (PR #917)**: JWT 인증 이후 REST·GraphQL·STOMP에서 재동의 미완료 사용자를 제한하고, 명시적 복구 API allowlist와 feature flag를 구성한다. WebSocket session은 AccessToken 만료 시 종료한다.
- **Phase 4 (별도 PR)**: 선택 약관 철회, 마케팅 수신 동의 처리 결과 통지, 관리자용 약관 변경 메타데이터를 확장한다.

## Alternatives Considered

### 대안 A: 현행 `termType` 기준 유지

현재 구조를 유지하고 재동의 여부도 `termType`만으로 판단한다.

장점:

- DB 마이그레이션이 없다.
- 기존 코드 변경량이 가장 작다.

단점:

- 약관 변경 전/후 동의를 구분할 수 없다.
- 재동의 차단 로직이 정확하지 않다.
- 감사 증빙으로 사용할 데이터가 부족하다.

선택하지 않은 이유:

- 사용자가 명시적으로 약관 변경 후 미동의자 제한을 요구했고, 이 요구는 `termType`만으로 구현할 수 없다.

### 대안 B: `term_consent`는 유지하고 로그에만 `termId` 추가

최신 상태 테이블은 `termType` 기준으로 유지하고, 감사 로그에만 `termId`를 남긴다.

장점:

- 상태 테이블 변경이 작다.
- 과거 로그를 기준으로 일부 증빙은 가능하다.

단점:

- 매 요청에서 재동의 여부를 판단할 때 로그를 집계해야 한다.
- 상태 테이블과 로그의 의미가 달라져 조회/운영 로직이 복잡해진다.

선택하지 않은 이유:

- 재동의 차단은 고빈도 요청 경로에 붙을 가능성이 높으므로 최신 상태 테이블만으로 빠르게 판단할 수 있어야 한다.

### 대안 C: `termId` 기준 상태 테이블과 불변 로그 병행

`term_consent`와 `term_consent_log` 모두 `termId`를 저장한다.

장점:

- 현재 동의 상태 조회와 감사 증빙을 모두 안정적으로 지원한다.
- 같은 타입의 새 약관 재동의를 자연스럽게 저장할 수 있다.
- Phase 2/3의 재동의 판정과 API 차단 구현이 단순해진다.

단점:

- DB 마이그레이션과 기존 테스트/Fixture 수정이 필요하다.
- 기존 row의 정확한 역사적 `termId`는 복구할 수 없으므로 backfill은 현재 활성 약관 또는 같은 타입의 최신 약관 기준으로만 가능하다.

선택한 이유:

- 요구사항의 핵심인 “약관 변경 후 재동의 대상자 판정”을 가장 단순하고 검증 가능하게 만든다.

## Consequences

### Positive

- 이메일 회원가입도 약관 동의 증빙이 남는다.
- 약관 변경 후 같은 `TermType`의 새 `termId`에 대한 재동의를 저장할 수 있다.
- 동의 조회가 과거 동의를 최신 약관으로 오인하지 않는다.
- Phase 2/3에서 미동의 사용자 API 제한을 정확하게 구현할 수 있다.

### Negative

- 기존 데이터는 과거 실제 동의 약관 ID를 완벽히 복구할 수 없다. 마이그레이션은 같은 타입의 활성 약관을 우선 매핑하고, 없으면 최신 약관을 사용한다.
- `term_consent` row 수가 약관 버전이 늘어날수록 증가한다.
- 새 필수 약관 활성화 직전에 발급된 AccessToken은 현재 TTL인 최대 1시간 동안 이전 동의 snapshot을 유지할 수 있다. 요청마다 DB를 조회하지 않는 대신 이 지연을 수용한다.
- 분산 캐시나 authority version을 도입하지 않으므로 즉시 강제 재검증은 이번 범위에 포함하지 않는다.

### Neutral / Trade-offs

- `Term`에 명시적 `version` 컬럼을 바로 추가하지 않고 `termId`를 버전 식별자로 사용한다. 운영자에게 사람이 읽는 버전명을 제공하는 기능은 Phase 4에서 별도 확장한다.
- `TERMS_RECONSENT_ENFORCEMENT_ENABLED` 기본값은 `false`다. 클라이언트가 `TERMS-0012` 복구 흐름을 배포한 후 환경별로 활성화한다.

## Implementation Notes

### 변경 영역 요약

1. **도메인** (`com.umc.product.term.domain.*`): `TermConsent`, `TermConsentLog`에 `termId` 필드를 추가한다.
2. **응용 / Port** (`...application.service.*`, `...application.port.*`): `TermAgreementCommandService`는 `termId` 기준 중복 검사/저장을 수행한다. `LoadTermPort`는 `termId` 목록 조회를 제공한다.
3. **어댑터 (out)** (`...adapter.out.persistence.*`): `TermRepository`, `TermConsentRepository`, `TermPersistenceAdapter`, `TermConsentPersistenceAdapter`에 `termId` 기반 조회를 추가한다.
4. **DB / 마이그레이션** (`src/main/resources/db/migration/V*__*.sql`): `term_consent.term_id`, `term_consent_log.term_id`를 추가하고 기존 row를 backfill한다.
5. **테스트** (`src/test/...`): 이메일 회원가입 동의 저장 테스트, `termId` 기반 중복/조회 테스트를 추가 또는 갱신한다.
6. **채널별 강제**: REST filter는 JWT와 maintenance 다음에 동작하고, GraphQL `WebGraphQlInterceptor`와 STOMP `ChannelInterceptor`는 resolver/message 처리 전에 같은 snapshot을 검사한다.

### 롤백 시 주의할 점

- Phase 1 롤백 시 신규 코드가 저장한 `term_id` 컬럼은 제거하지 말고 무시하는 방향이 안전하다. 이미 저장된 감사 증빙을 삭제하면 운영상 추적성이 손상된다.
- `member_id + term_id` unique index를 제거하면 동일 약관 중복 동의가 다시 허용될 수 있다.

## References

- [개인정보 보호법 시행령 제17조](https://www.law.go.kr/LSW/lsLawLinkInfo.do?chrClsCd=010202&lsJoLnkSeq=1000572094)
- [약관의 규제에 관한 법률 제3조](https://www.law.go.kr/LSW/lsLawLinkInfo.do?chrClsCd=010202&lsJoLnkSeq=1000526624)
- [정보통신망법 제50조](https://www.law.go.kr/lsLinkProc.do?chrClsCd=010202&joLnkStr=%EC%A0%9C50%EC%A1%B0+%EB%98%90%EB%8A%94+%EC%A0%9C50%EC%A1%B0%EC%9D%988&joNo=005000000%5E005000000%5E005002000%5E005003000%5E005004000%5E005005000%5E005006000%5E005007000%5E005008000&lsId=000030&lsNm=%EC%A0%95%EB%B3%B4%ED%86%B5%EC%8B%A0%EB%A7%9D+%EC%9D%B4%EC%9A%A9%EC%B4%89%EC%A7%84+%EB%B0%8F+%EC%A0%95%EB%B3%B4%EB%B3%B4%ED%98%B8+%EB%93%B1%EC%97%90+%EA%B4%80%ED%95%9C+%EB%B2%95%EB%A5%A0&mode=2)
