# PR #1171 도메인 테스트 검증 목록

> 대상 PR: `feature/#1167-domain-test-coverage` → `develop`<br>
> 비교 기준: `origin/develop...HEAD`<br>
> 대상 도메인: `member`, `challenger`, `authentication`, `authorization`, `organization`

## 1. 요약

이 PR은 다섯 도메인의 production class에 대해 JaCoCo **Line 100% / Class 100%**를 달성하고,
성공 경로뿐 아니라 입력 경계, 권한 거부, 외부 연동 실패, 누락 데이터, 중복·정렬·배치 처리와 같은
회귀 위험이 높은 상황을 테스트로 고정한다.

| 도메인 | 변경 테스트 클래스 | 추가 테스트 선언 | Line | Class | Branch |
|---|---:|---:|---:|---:|---:|
| authorization | 16 | 88 | 840/840 (100%) | 44/44 (100%) | 97.3% |
| member | 20 | 73 | 1,446/1,446 (100%) | 89/89 (100%) | 90.5% |
| authentication | 17 | 125 | 1,679/1,679 (100%) | 128/128 (100%) | 85.0% |
| challenger | 12 | 87 | 1,268/1,268 (100%) | 76/76 (100%) | 89.0% |
| organization | 17 | 120 | 3,083/3,083 (100%) | 241/241 (100%) | 86.5% |
| **합계** | **82** | **493** | **8,316/8,316 (100%)** | **578/578 (100%)** | **88.5%** |

추가 테스트 선언은 diff에 새로 추가된 `@Test`, `@ParameterizedTest`, `@RepeatedTest` 수다.
Parameterized Test의 인자별 실행과 기존 테스트의 실행도 포함되므로 실제 Gradle 실행 건수와는 다르다.

## 2. 공통 검증 원칙

- 정상 동작뿐 아니라 `null`, 빈 문자열·컬렉션, 잘못된 ID, 미존재 데이터와 같은 입력 경계를 검증한다.
- 권한 검사는 미존재 사용자와 알 수 없는 권한에 대해 fail-closed로 동작하는지 검증한다.
- 배치 처리는 빈 입력 단축, 중복 제거, 입력 순서 보존, 일부 데이터 누락을 검증한다.
- Controller/Assembler/DTO는 command 변환, 응답 매핑, 민감 정보 마스킹과 호환 계약을 검증한다.
- Persistence Adapter는 Port 위임, not-found 계약, 빈 입력 최적화와 QueryDSL 실제 조회를 검증한다.
- OAuth/OIDC 외부 연동은 provider별 정상 응답과 네트워크·파싱·서명·claim 실패를 함께 검증한다.
- 테스트 데이터는 도메인별 fixture를 재사용해 Given/When/Then 의도가 드러나도록 구성한다.

## 3. 도메인별 테스트 목록

### 3.1 authorization

| 테스트 클래스 | 검증 상황 |
|---|---|
| `AccessControlAspectTest` | 비인증·잘못된 principal 거부, null/blank 표현식, resource ID 해석, 권한 거부 시 대상 메서드 미호출 |
| `ChallengerRoleControllerUnitTest` | 역할 생성 command 및 ID 응답, 기수 포함 조회 응답, 삭제 command 매핑 |
| `ChallengerRoleAdapterTest` | 회원·기수별 조회 위임, batch 조회, not-found, 저장·일괄 저장·삭제 |
| `ChallengerRoleQueryRepositoryTest` | 회원별 및 회원+기수별 역할 join 조회 |
| `AuthoritySnapshotCacheSerializerTest` | null/blank/손상 JSON, 직렬화 실패, null tree와 null collection 정규화 |
| `AuthorizationServiceCacheTest` | 캐시 역직렬화 실패 시 fresh subject 재조회 및 권한 판정 지속 |
| `AuthorizationServicePolicyTest` | evaluator 선택·위임·허용·거부, evaluator 누락·중복, 권한 metric 기록 |
| `AuthoritySnapshotCacheCommandServiceTest` | null/빈 member ID 입력 시 불필요한 eviction 생략 |
| `ChallengerRoleAnyGisuAuthorityTest` | 미존재 회원 fail-closed, `SUPER_ADMIN` 전역 우회, 잘못된 system role, 역할 계층·조직 범위 |
| `ChallengerRoleGisuAuthorityTest` | null 기수·조직, null 역할, ANY/ALL 의미, 정확한 기수 scope, `SUPER_ADMIN` 우회 |
| `ChallengerRoleListQueryTest` | 회원·기수별 목록, 경량 scope, batch null/empty, 중복 제거와 안정적 정렬 |
| `ChallengerRoleQueryServiceTest` | legacy facade 위임, 지원하지 않는 system role 거부, 지부장 호환 판정 |
| `CheckResourcePermissionValidationTest` | query/resourceIds/permissions의 null·empty·null element 거부, 전체 권한 결과 순서 |
| `AuthoritySnapshotMatrixTest` | snapshot 불변성·왕복 변환, null collection, 중앙·학교·지부 scope 권한 matrix |
| `AuthorizationResidualContractTest` | legacy Info factory 호환, 조직 ID 필수 조건, 중앙·지부 정책 잔여 분기 |
| `ResourcePermissionContractTest` | type code 왕복, permission set 불변성, 미지원 권한, ID 변환, evaluator 계약 |

핵심적으로 기수 범위 역할과 전역 `SUPER_ADMIN`을 분리하고, 존재하지 않는 회원·리소스·권한은
허용되지 않도록 검증한다. 캐시가 손상돼도 권한 검사가 잘못 허용되는 대신 원본 데이터를 다시 읽는다.

### 3.2 member

| 테스트 클래스 | 검증 상황 |
|---|---|
| `MemberGraphQlControllerTest` | 빈 batch 단축, 중복 key가 있을 때 첫 결과 보존 |
| `MemberSchoolGraphQlResponseTest` | link·시간이 null인 학교와 값이 채워진 학교의 GraphQL 응답 변환 |
| `MemberCommandControllerUnitTest` | OAuth·email·Apple 가입 command, 프로필 이미지·링크, 본인/관리자 탈퇴 token 전달 |
| `MemberQueryControllerTest` | ID 기반 공개 프로필 응답 |
| `MemberInfoResponseAssemblerTest` | challenger 미존재 batch 단축, 기수 중복 제거, 공개 정보 마스킹, 학교 누락 |
| `MemberInfoResponseTest` | legacy/current factory 호환, 학교 필수, 공개 응답 마스킹, 프로필 누락·존재 |
| `MemberV2ResponseContractTest` | 현재·활동·전체 기수 이력, 활동 challenger 없음, 검색 응답과 email 마스킹 |
| `MemberPersistenceAdaptersTest` | get/query/save/delete/search/count/profile/system-role 위임과 빈 입력 guard |
| `MemberQueryRepositoryTest` | count·lock·nickname 조회와 chapter filter |
| `EmailMemberRegisterServiceTest` | 빈 batch 단축과 입력 순서를 보존하는 batch ID 반환 |
| `MemberCredentialCommandServiceTest` | 비밀번호 변경, 미존재 회원, null ID 방어 |
| `MemberCredentialQueryServiceTest` | 잘못된 인증 입력, password hash 없음, 마스킹된 credential DTO |
| `MemberPermissionEvaluatorTest` | 회원 read/delete 정책과 미지원 permission fail-closed |
| `MemberQueryServiceEdgeCaseTest` | 학교·프로필·역할 조합, storage 누락, Optional/get 계약, 빈 batch와 일부 데이터 누락 |
| `MemberRegistrationValidatorTest` | 선택 프로필 파일, 파일·학교 존재, 필수 약관 전체/일부/미동의 |
| `MemberSearchServiceResidualTest` | null/empty ID, challenger·기수 데이터 누락 시 검색 결과 조립 |
| `MemberServiceTest` | 가입 orchestration·event, 검증 실패, bulk 순서, 프로필 수정, 탈퇴·OAuth·권한 캐시 |
| `MemberSummaryV2QueryServiceTest` | 학교·기수 정보 누락 시 안전한 summary 생성과 정렬 |
| `MemberProfileTest` | LinkedIn·Instagram·GitHub·Blog·Personal 링크 전체 갱신 |
| `MemberTest` | nickname/file ID를 각각 변경하는 profile overload의 상태 보존 |

개인정보 노출 표면은 공개 응답에서 email 등 민감 값이 마스킹되는지 확인한다. 여러 도메인의 데이터를
합치는 조회에서는 학교·기수·challenger·storage 데이터가 일부 없어도 결과 전체가 깨지지 않는지 검증한다.

### 3.3 authentication

| 테스트 클래스 | 검증 상황 |
|---|---|
| `AuthenticationResidualContractTest` | SSO client·origin 필수값, provider 등록, 설정 기본값, DTO 마스킹, OAuth token 우선순위·redirect 검증 |
| `EmailVerificationRetentionSchedulerTest` | 삭제 대상 없음, 정리 실패 시 실패 metric 기록과 예외 재전파 |
| `AuthenticationControllerUnitTest` | Google/Kakao/Apple 기존·신규 회원, access/auth code, Apple refresh token 갱신·보존 |
| `AuthenticationWebResidualTest` | token 재발급 매핑, SSO cookie domain·max-age, Referer origin 파싱 |
| `CredentialAuthenticationControllerUnitTest` | 회원가입, 비밀번호 변경·초기화, 사용 가능 여부, email login command |
| `EmailAuthenticationControllerUnitTest` | email 인증 확인·발송·재발송 command |
| `MemberOAuthControllerUnitTest` | OAuth 연결·해제, token 유무, 연결 provider 목록 |
| `AppleTokenVerifierEdgeCaseTest` | email/issuer/audience/kid 누락·불일치, client 구분, code 교환·revoke·client-secret·서명 오류 |
| `GoogleTokenVerifierEdgeCaseTest` | issuer 변형, JWT 오류, opaque tokeninfo fallback, 빈 token, audience·claim·revoke 실패 |
| `KakaoTokenVerifierEdgeCaseTest` | redirect whitelist, userinfo fallback, client secret 없음, token/응답/ID/profile·issuer·audience·kid 오류 |
| `OAuthTokenVerificationAdapterTest` | provider별 verifier 위임, Apple audience, code 교환 제한, unlink/revoke 흐름 |
| `OAuthVerifierFailureNormalizationTest` | Kakao/Google/Apple 네트워크·응답 파싱 실패를 공통 인증 예외로 정규화 |
| `OidcPublicKeyResolverTest` | kid 누락, lock 안에서 JWKS cache 충전, JWKS 오류·빈 key set |
| `AuthenticationPersistenceResidualTest` | MemberOAuth·EmailVerification adapter 계약과 최신 인증 row QueryDSL 조회 |
| `AuthenticationApplicationResidualTest` | token row 발급, SHA-256·rehash, email event, query not-found, 재발송 throttling |
| `OAuthAuthenticationServiceEdgeCaseTest` | 기존·신규 로그인, access/code, 중복 연결, Apple refresh, bulk 원자성, unlink 성공·실패 |
| `SsoTokenExchangeCommandServiceTest` | redirect whitelist 적용과 Android client token 매핑 |

외부 provider는 정상 응답만 mock하지 않고, claim 누락·잘못된 issuer/audience·JWKS 문제·통신 실패까지
검증한다. 실패는 provider 구현 예외가 새지 않고 애플리케이션의 공통 인증 실패 의미로 변환된다.

### 3.4 challenger

| 테스트 클래스 | 검증 상황 |
|---|---|
| `ChallengerControllerResidualTest` | 일괄 생성, 비활성화, 파트 변경, 물리 삭제, 활동 기록 조회·소비·생성·일괄 처리 |
| `ChallengerRecordResponseAssemblerTest` | 기수·학교·지부를 포함한 활동 기록 code/ID 응답 조립 |
| `ChallengerResponseAssemblerTest` | member·gisu·chapter 집계, 빈 입력·중복·학교/지부 누락, null gisu 손상 데이터 |
| `ChallengerWebDtoResidualTest` | cursor size 정규화·상태, 전역 검색, 수정 command, cursor 응답, 기록 조직 규칙 |
| `ChallengerInfoResponseTest` | legacy alias, 역할 응답, 공개 정보 마스킹 |
| `ChallengerPersistenceAdapterTest` | Optional/get not-found, 회원·기수 query, null/empty guard, exists/save/delete/search/count |
| `ChallengerPersistenceResidualTest` | 활동 기록·점수 adapter와 점수 QueryDSL 잔여 분기 |
| `ChallengerQueryRepositoryIntegrationTest` | 실제 DB 필터·paging·part·point·최신 기수, 잘못된 cursor, 지부 존재 조회 |
| `ChallengerApplicationResidualTest` | 상태·점수·삭제, bulk 중복, production guard, 기록 bulk/delete와 DTO 변환 |
| `ChallengerQueryServiceEdgeCaseTest` | get/find/null·미존재 조합, 활동 상태·점수, 빈 batch와 조회 계약 경계 |
| `ChallengerSearchServiceTest` | 빈 입력 단축, batch 집계, `size + 1` cursor, 다음 페이지 없음, 전역·offset·cursor 검색 |
| `ChallengerEvaluatorResidualTest` | 생성·수정·삭제, 기록·점수 권한의 중앙/학교 scope, 학교 누락·미지원 권한 |

검색과 batch 조회에서는 데이터 순서·cursor 경계·누락 참조를 다루고, 활동 기록과 점수 변경은
리소스의 기수·학교 scope에 맞는 권한인지 검증한다.

### 3.5 organization

| 테스트 클래스 | 검증 상황 |
|---|---|
| `OrganizationDtoResidualTest` | request command 변환, 불변 member 교체, 학교·지부·study·page·UMC PRODUCT 응답, null link/time |
| `OrganizationGraphQlControllerResidualTest` | batch source/model 중복 시 첫 결과 보존 |
| `OrganizationControllerResidualTest` | study·chapter·school·schedule·gisu API 위임과 비인증 UMC PRODUCT command |
| `OrganizationPersistenceAdapterResidualTest` | study/school/chapter/chapter-school/gisu adapter 계약, not-found와 입력 guard |
| `OrganizationQueryRepositoryIntegrationTest` | 학교 paging/filter/active, 기수 날짜 경계, 상세·링크·join, UMC PRODUCT 검색 filter |
| `StudyGroupQueryRepositoryTest` | scope별 이름·null scope, 동일 기수/파트 참여 충돌과 수정 대상 제외 |
| `UmcProductPersistenceAdapterResidualTest` | member/chapter/membership/activity/leadership/squad/participant adapter와 제약 |
| `OrganizationCoreServiceResidualTest` | chapter 조회·기수 불일치, 학교 상세·logo 없음·grouping, study lifecycle·충돌 |
| `OrganizationCommandServiceResidualTest` | chapter가 있는 gisu 삭제 거부·동일 active no-op, school 삭제 null/empty ID |
| `SchoolQueryServiceTest` | 모든 학교 logo가 null이면 storage batch 조회 생략 |
| `StudyGroupQueryServiceResidualTest` | 내 study batch, 권한·part leader, scope·기수/파트, 빈 member와 상세 조회 |
| `OrganizationRemainingServiceTest` | history 정렬, chapter/gisu/school/schedule lifecycle·validation, chapter/squad query·access policy |
| `UmcProductMemberCommandServiceEdgeCaseTest` | 기간 경계·중복·null/empty·열린 종료일, 파일·권한, membership/leadership 겹침과 lifecycle |
| `UmcProductMemberQueryServiceTest` | 전체 이력 집계, 누락 데이터, 빈 page, 정렬·미존재 참조, 기준일 active filter |
| `UmcProductSquadCommandServiceEdgeCaseTest` | squad 생성·수정 권한/중복/기간, 열린 종료일 포함, participant lifecycle·겹침 |
| `OrganizationPermissionEvaluatorResidualTest` | 지원 resource/permission 조합과 미지원 조합 fail-closed |
| `OrganizationDomainResidualTest` | 필수 연관·ID, 날짜 경계, 상태 변경, activity/role/position 미존재 도메인 규칙 |

조직 도메인의 핵심 경계인 기수·활동 기간은 시작일/종료일 포함 여부, 열린 종료일, 중첩 기간을
집중 검증한다. 학교·지부·스쿼드·멤버십 참조가 누락되거나 충돌하는 경우도 명시적으로 다룬다.

## 4. 추가·확장 fixture

| fixture | 사용 목적 |
|---|---|
| `AuthenticationFixture` | OAuth/OIDC provider payload, 인증 token·command의 반복 생성을 통일 |
| `AuthorizationFixture` | 역할, authority snapshot, resource permission 입력을 선언적으로 생성 |
| `ChallengerUnitFixture` | challenger·기수·파트·활동 기록의 기본값과 변형 지점을 제공 |
| `MemberUnitFixture` | member·profile·school·credential·응답 조립 데이터를 재사용 |
| `OrganizationUnitFixture` | 학교·지부·기수·study·UMC PRODUCT membership/squad 기간 데이터를 재사용 |

fixture는 유효한 기본 객체를 먼저 만들고, 각 테스트가 검증하려는 값만 명시적으로 바꾸도록 구성했다.
그 결과 생성자 인자 나열보다 실패 원인과 기대 동작을 읽기 쉽게 유지한다.

## 5. 실행 및 커버리지 측정

검증 명령:

```bash
./gradlew test \
  --tests 'com.umc.product.authorization.**' \
  --tests 'com.umc.product.member.**' \
  --tests 'com.umc.product.authentication.**' \
  --tests 'com.umc.product.challenger.**' \
  --tests 'com.umc.product.organization.**'
```

실행 결과:

- 총 1,220건 실행 대상
- 1,180건 성공
- 40건 ignored
- 실패 0건
- `BUILD SUCCESSFUL`

JaCoCo 결과는 `build/reports/jacoco/test/html/index.html`을 기준으로 확인했다.
QueryDSL 생성물은 실제 생성 경로인 `build/generated/querydsl` source tree에 속한 class만 제외하며,
애플리케이션이 직접 작성한 `Query*`, `Question*` class는 커버리지 대상에 포함한다.

## 6. 해석 시 주의사항

- Line/Class 100%는 대상 production class가 테스트 실행 중 모두 로드되고 각 line이 실행됐다는 뜻이다.
- Branch는 조건식 조합과 컴파일러 생성 분기 때문에 100%가 아니며, 이 PR의 목표는 발생 가능성이 높은
  비즈니스·보안·외부 연동 경계를 우선 고정하는 것이다.
- ignored 40건은 실행 결과에서 별도로 집계했으며 성공 건수에 포함하지 않았다.
- 이 문서는 PR diff 기준 목록이다. 이후 `develop` 변경으로 테스트나 production class가 추가되면
  커버리지를 다시 측정해야 한다.
