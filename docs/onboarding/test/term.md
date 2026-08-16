# Term 테스트 케이스

- 테스트 파일: 8개
- 테스트 케이스: 23개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 1 |
| UseCase / Application Service | 17 |
| Repository / Outbound Persistence | 1 |
| E2E / Integration | 4 |

## Controller / Inbound Adapter

### TermControllerTest
- 테스트 설명: TermController
- 위치: `src/test/java/com/umc/product/term/adapter/in/web/TermControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [30](../../../src/test/java/com/umc/product/term/adapter/in/web/TermControllerTest.java#L30) | GET /api/v1/terms 활성 약관 전체 목록을 반환한다 | HTTP GET /api/v1/terms | 성공: HTTP 200 OK |

## UseCase / Application Service

### GetRequiredTermConsentStatusUseCaseTest
- 위치: `src/test/java/com/umc/product/term/application/port/in/query/GetRequiredTermConsentStatusUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [35](../../../src/test/java/com/umc/product/term/application/port/in/query/GetRequiredTermConsentStatusUseCaseTest.java#L35) | 현재 활성 필수 약관 중 미동의 약관을 반환한다 | 호출 getRequiredTermConsentStatus(100L) | 성공: 검증 assertThat(result.needsReconsent()).isTrue(); assertThat(result.missingRequiredTerms()); .containsExactly(2L); |
| [56](../../../src/test/java/com/umc/product/term/application/port/in/query/GetRequiredTermConsentStatusUseCaseTest.java#L56) | 현재 활성 필수 약관을 모두 동의한 경우 재동의가 필요하지 않다 | 호출 getRequiredTermConsentStatus(100L) | 성공: 검증 assertThat(result.needsReconsent()).isFalse(); assertThat(result.missingRequiredTerms()).isEmpty(); |

### GetTermUseCaseTest
- 위치: `src/test/java/com/umc/product/term/application/port/in/query/GetTermUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [35](../../../src/test/java/com/umc/product/term/application/port/in/query/GetTermUseCaseTest.java#L35) | 타입으로 활성 약관을 조회한다 | 호출 getTermsByType(TermType.SERVICE) | 성공: 검증 assertThat(result.type()).isEqualTo(TermType.SERVICE); assertThat(result.isMandatory()).isTrue(); |
| [50](../../../src/test/java/com/umc/product/term/application/port/in/query/GetTermUseCaseTest.java#L50) | 타입이 null이면 예외 | 호출 getTermsByType(null)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.TERMS_TYPE_REQUIRED; 검증 .isEqualTo(TermErrorCode.TERMS_TYPE_REQUIRED); |
| [59](../../../src/test/java/com/umc/product/term/application/port/in/query/GetTermUseCaseTest.java#L59) | 활성 약관이 없으면 예외 | 호출 getTermsByType(TermType.SERVICE)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.TERMS_NOT_FOUND; 검증 .isEqualTo(TermErrorCode.TERMS_NOT_FOUND); |
| [71](../../../src/test/java/com/umc/product/term/application/port/in/query/GetTermUseCaseTest.java#L71) | ID로 약관을 조회한다 | 호출 getTermsById(1L) | 성공: 검증 assertThat(result.type()).isEqualTo(TermType.PRIVACY); |
| [85](../../../src/test/java/com/umc/product/term/application/port/in/query/GetTermUseCaseTest.java#L85) | ID가 null이면 예외 | 호출 getTermsById(null)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.TERM_ID_REQUIRED; 검증 .isEqualTo(TermErrorCode.TERM_ID_REQUIRED); |
| [94](../../../src/test/java/com/umc/product/term/application/port/in/query/GetTermUseCaseTest.java#L94) | 존재하지 않는 ID로 조회하면 예외 | 호출 getTermsById(999L)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.TERMS_NOT_FOUND; 검증 .isEqualTo(TermErrorCode.TERMS_NOT_FOUND); |
| [106](../../../src/test/java/com/umc/product/term/application/port/in/query/GetTermUseCaseTest.java#L106) | 활성 약관 전체 목록을 조회한다 | 호출 listActiveTerms() | 성공: 검증 assertThat(result).hasSize(2); assertThat(result); .containsExactly(TermType.SERVICE, TermType.MARKETING); assertThat(result) |
| [132](../../../src/test/java/com/umc/product/term/application/port/in/query/GetTermUseCaseTest.java#L132) | 활성 약관이 없으면 빈 목록을 반환한다 | 호출 listActiveTerms() | 성공: 검증 assertThat(result).isEmpty(); |

### ManageTermAgreementUseCaseTest
- 위치: `src/test/java/com/umc/product/term/application/port/in/command/ManageTermAgreementUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [52](../../../src/test/java/com/umc/product/term/application/port/in/command/ManageTermAgreementUseCaseTest.java#L52) | 약관에 동의한다 | CreateTermConsentCommand {memberId=100L, termId=1L, isAgreed=true}; 호출 createTermConsent(command) | 성공: 검증 assertThat(savedConsent.getMemberId()).isEqualTo(100L); assertThat(savedConsent.getTermId()).isEqualTo(1L); assertThat(savedConsent.getTermType()).isEqualTo(TermType.SERVICE); assertThat(savedLog.getMemberId()).isEqua... |
| [88](../../../src/test/java/com/umc/product/term/application/port/in/command/ManageTermAgreementUseCaseTest.java#L88) | 이미 동의한 약관에 다시 동의하면 예외 | CreateTermConsentCommand {memberId=100L, termId=1L, isAgreed=true}; 호출 createTermConsent(command)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.TERMS_CONSENT_ALREADY_EXISTS; 검증 .isEqualTo(TermErrorCode.TERMS_CONSENT_ALREADY_EXISTS); |
| [111](../../../src/test/java/com/umc/product/term/application/port/in/command/ManageTermAgreementUseCaseTest.java#L111) | 동의하지 않은 약관은 아무것도 저장하지 않는다 | CreateTermConsentCommand {memberId=100L, termId=2L, isAgreed=false}; 호출 createTermConsent(command) | 성공: 동의하지 않은 약관은 아무것도 저장하지 않는다 |
| [135](../../../src/test/java/com/umc/product/term/application/port/in/command/ManageTermAgreementUseCaseTest.java#L135) | 존재하지 않는 약관에 동의하면 예외 | CreateTermConsentCommand {memberId=100L, termId=999L, isAgreed=true}; 호출 createTermConsent(command)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.TERMS_NOT_FOUND; 검증 .isEqualTo(TermErrorCode.TERMS_NOT_FOUND); |

### ManageTermUseCaseTest
- 위치: `src/test/java/com/umc/product/term/application/port/in/command/ManageTermUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [32](../../../src/test/java/com/umc/product/term/application/port/in/command/ManageTermUseCaseTest.java#L32) | 필수 약관을 생성한다 | CreateTermCommand {type=TermType.SERVICE, link="https://example.com/terms/service", required=true}; 호출 createTerms(command) | 성공: 검증 assertThat(termsId).isEqualTo(1L); |
| [56](../../../src/test/java/com/umc/product/term/application/port/in/command/ManageTermUseCaseTest.java#L56) | 선택 약관을 생성한다 | CreateTermCommand {type=TermType.MARKETING, link="https://example.com/terms/marketing", required=false}; 호출 createTerms(command) | 성공: 검증 assertThat(termsId).isEqualTo(2L); |

### TermAgreementQueryServiceTest
- 위치: `src/test/java/com/umc/product/term/application/port/in/query/TermAgreementQueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [35](../../../src/test/java/com/umc/product/term/application/port/in/query/TermAgreementQueryServiceTest.java#L35) | 동의한 약관은 현재 활성 약관이 아니라 저장된 약관 ID 기준으로 조회한다 | 호출 getAgreedTermsByMemberId(100L) | 성공: 검증 assertThat(result); .containsExactly(10L, 20L); |

## Repository / Outbound Persistence

### TermPersistenceAdapterTest
- 테스트 설명: TermPersistenceAdapter
- 위치: `src/test/java/com/umc/product/term/adapter/out/persistence/TermPersistenceAdapterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [20](../../../src/test/java/com/umc/product/term/adapter/out/persistence/TermPersistenceAdapterTest.java#L20) | listActive는 활성 약관만 ID 오름차순으로 반환한다 | 호출 listActive() | 성공: 검증 assertThat(resultIds); .contains(serviceTerm.getId(), privacyTerm.getId()); assertThat(result); .containsOnly(true); |

## E2E / Integration

### TermAgreementIntegrationTest
- 위치: `src/test/java/com/umc/product/integration/term/TermAgreementIntegrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [49](../../../src/test/java/com/umc/product/integration/term/TermAgreementIntegrationTest.java#L49) | 회원이 약관에 동의하면 동의 정보가 저장되고 조회된다 | CreateTermConsentCommand {memberId=member.getId(, termId=term.getId(, isAgreed=true}; 호출 createTermConsent(command) | 성공: 검증 assertThat(loadTermConsentPort.existsByMemberIdAndTermId(member.getId(), term.getId())); .isTrue(); assertThat(agreed); .containsExactly(TermType.SERVICE); |
| [75](../../../src/test/java/com/umc/product/integration/term/TermAgreementIntegrationTest.java#L75) | 동일 약관에 중복 동의하면 TERMS CONSENT ALREADY EXISTS 예외가 발생한다 | CreateTermConsentCommand {memberId=member.getId(, termId=term.getId(, isAgreed=true}; 호출 createTermConsent(command)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.TERMS_CONSENT_ALREADY_EXISTS; 검증 .isEqualTo(TermErrorCode.TERMS_CONSENT_ALREADY_EXISTS); |
| [95](../../../src/test/java/com/umc/product/integration/term/TermAgreementIntegrationTest.java#L95) | 같은 타입의 새 약관에는 재동의할 수 있다 | CreateTermConsentCommand {memberId=member.getId(, termId=newTerm.getId(, isAgreed=true}; 호출 createTermConsent(command) | 성공: 검증 assertThat(agreed); .containsExactlyInAnyOrder(oldTerm.getId(), newTerm.getId()); |
| [119](../../../src/test/java/com/umc/product/integration/term/TermAgreementIntegrationTest.java#L119) | 존재하지 않는 약관 ID로 동의를 시도하면 TERMS NOT FOUND 예외가 발생한다 | CreateTermConsentCommand {memberId=member.getId(, termId=nonExistentTermId, isAgreed=true}; 호출 createTermConsent(command)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.TERMS_NOT_FOUND; 검증 .isEqualTo(TermErrorCode.TERMS_NOT_FOUND); |
