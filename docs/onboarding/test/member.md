# Member 테스트 케이스

- 테스트 파일: 20개
- 테스트 케이스: 100개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| Controller / Inbound Adapter | 8 |
| UseCase / Application Service | 71 |
| Repository / Outbound Persistence | 1 |
| E2E / Integration | 1 |
| Domain | 19 |

## Controller / Inbound Adapter

### MemberCommandControllerTest
- 테스트 설명: MemberCommandController
- 위치: `src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [91](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerTest.java#L91) | MemberCommandController / 이메일 회원가입 성공 시 토큰과 memberId를 반환한다 | HTTP POST /api/v1/member/register/email; body { "rawPassword": "Password123!", "name": "홍길동", "nickname": "길동", "emailVerificationToken": "email-token", "schoolId": 1, "termsAgreements": [ {"termsId": 10, "isAgreed": true} ] } | 성공: HTTP 200 OK |
| [122](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerTest.java#L122) | 이메일 회원가입 요청의 rawPassword가 blank이면 400 | HTTP POST /api/v1/member/register/email; body { "rawPassword": " ", "name": "홍길동", "nickname": "길동", "emailVerificationToken": "email-token", "schoolId": 1, "termsAgreements": [ {"termsId": 10, "isAgreed": true} ] } | 실패: HTTP 400 Bad Request |
| [144](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerTest.java#L144) | 내 회원 정보 수정은 로그인 회원 ID로 UseCase를 호출한다 | HTTP PATCH /api/v1/member | 성공: HTTP 200 OK |
| [161](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerTest.java#L161) | 내 이메일 변경은 CHANGE_EMAIL 토큰을 검증하고 로그인 회원 ID로 UseCase를 호출한다 | HTTP PATCH /api/v1/member/email; body { "emailVerificationToken": "change-email-token" } | 성공: HTTP 200 OK; 검증 assertThat(captor.getValue().memberId()).isEqualTo(TEST_MEMBER_ID); assertThat(captor.getValue().email()).isEqualTo("new@example.com"); |

### MemberQueryControllerTest
- 테스트 설명: MemberQueryController
- 위치: `src/test/java/com/umc/product/member/adapter/in/web/MemberQueryControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [62](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberQueryControllerTest.java#L62) | MemberQueryController / 내 프로필을 조회한다 | HTTP GET /api/v1/member/me | 성공: HTTP 200 OK |
| [77](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberQueryControllerTest.java#L77) | MemberQueryController / 회원 검색 응답은 이메일을 마스킹한다 | HTTP GET /api/v1/member/search; param page="0"; param size="10" | 성공: HTTP 200 OK |

### MemberQueryV2ControllerTest
- 테스트 설명: MemberQueryV2Controller
- 위치: `src/test/java/com/umc/product/member/adapter/in/web/v2/MemberQueryV2ControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [65](../../../src/test/java/com/umc/product/member/adapter/in/web/v2/MemberQueryV2ControllerTest.java#L65) | MemberQueryV2Controller / 내 v2 종합 정보를 조회한다 | HTTP GET /api/v2/member/me | 성공: HTTP 200 OK |
| [92](../../../src/test/java/com/umc/product/member/adapter/in/web/v2/MemberQueryV2ControllerTest.java#L92) | MemberQueryV2Controller / 회원 검색 v2 응답은 이메일을 마스킹한다 | HTTP GET /api/v2/member/search; param page="0"; param size="10" | 성공: HTTP 200 OK |

## UseCase / Application Service

### ChallengerSearchV2Test
- 테스트 설명: MemberSearchService.searchChallengersByV2 — 챌린저 단위 v2 검색
- 위치: `src/test/java/com/umc/product/member/application/service/ChallengerSearchV2Test.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [80](../../../src/test/java/com/umc/product/member/application/service/ChallengerSearchV2Test.java#L80) | MemberSearchService.searchChallengersByV2 — 챌린저 단위 v2 검색 / 챌린저 단위 검색은 같은 회원이라도 여러 row로 반환된다 | 조건 MemberSearchService.searchChallengersByV2 — 챌린저 단위 v2 검색 / 챌린저 단위 검색은 같은 회원이라도 여러 row로 반환된다 | 성공: 검증 assertThat(content).hasSize(3); assertThat(content).extracting(ChallengerSearchItemV2Info::memberId); .containsExactly(10L, 10L, 10L); assertThat(content).extracting(ChallengerSearchItemV2Info::challengerId) |
| [109](../../../src/test/java/com/umc/product/member/application/service/ChallengerSearchV2Test.java#L109) | challengerStatus와 isAdminInActiveGisu가 매핑된다 | 조건 challengerStatus와 isAdminInActiveGisu가 매핑된다 | 성공: 검증 assertThat(item.challengerStatus()).isEqualTo(ChallengerStatus.ACTIVE); assertThat(item.roleTypes()).containsExactly(ChallengerRoleType.SCHOOL_PRESIDENT); assertThat(item.isAdminInActiveGisu()).isTrue(); |
| [137](../../../src/test/java/com/umc/product/member/application/service/ChallengerSearchV2Test.java#L137) | 같은 회원의 과거 기수 행도 활성기수 운영진이면 isAdminInActiveGisu true이다 | 조건 같은 회원의 과거 기수 행도 활성기수 운영진이면 isAdminInActiveGisu true이다 | 성공: 검증 assertThat(content).extracting(ChallengerSearchItemV2Info::isAdminInActiveGisu); .containsExactly(true, true); |
| [168](../../../src/test/java/com/umc/product/member/application/service/ChallengerSearchV2Test.java#L168) | 활성기수 행이 현재 페이지에 없어도 활성기수 운영진이면 isAdminInActiveGisu true이다 | 조건 활성기수 행이 현재 페이지에 없어도 활성기수 운영진이면 isAdminInActiveGisu true이다 | 성공: 검증 assertThat(result.page().getContent().get(0).isAdminInActiveGisu()).isTrue(); |

### EmailMemberRegisterServiceTest
- 위치: `src/test/java/com/umc/product/member/application/service/EmailMemberRegisterServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [56](../../../src/test/java/com/umc/product/member/application/service/EmailMemberRegisterServiceTest.java#L56) | 이메일 회원가입 성공 시 약관 동의 정보를 저장한다 | EmailRegisterMemberCommand {rawPassword="Password123!", name="홍길동", nickname="길동", email="gildong@example.com", schoolId=1L, termConsents=List.of( TermConsents.builder(, ...}; 호출 register(command) | 성공: 검증 assertThat(memberId).isEqualTo(100L); |
| [98](../../../src/test/java/com/umc/product/member/application/service/EmailMemberRegisterServiceTest.java#L98) | 필수 약관 검증 실패 시 회원과 자격증명을 저장하지 않는다 | EmailRegisterMemberCommand {rawPassword="Password123!", name="홍길동", nickname="길동", email="gildong@example.com", schoolId=1L, termConsents=List.of( TermConsents.builder(, ...}; 호출 register(command)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.MANDATORY_TERMS_NOT_AGREED; 검증 .isEqualTo(TermErrorCode.MANDATORY_TERMS_NOT_AGREED); |

### GetMemberUseCaseTest
- 위치: `src/test/java/com/umc/product/member/application/port/in/query/GetMemberUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [95](../../../src/test/java/com/umc/product/member/application/port/in/query/GetMemberUseCaseTest.java#L95) | getById | 호출 getById(1L) | 성공: 검증 assertThat(result.id()).isEqualTo(1L); assertThat(result.name()).isEqualTo("홍길동"); assertThat(result.nickname()).isEqualTo("길동"); assertThat(result.email()).isEqualTo("test@example.com"); |
| [114](../../../src/test/java/com/umc/product/member/application/port/in/query/GetMemberUseCaseTest.java#L114) | getById / 회원이 존재하지 않으면 예외 | 호출 getById(999L)) | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_FOUND; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND); |
| [129](../../../src/test/java/com/umc/product/member/application/port/in/query/GetMemberUseCaseTest.java#L129) | getById / 학교, 프로필 이미지, 역할 모두 있을 때 프로필 조회 성공 | 호출 getById(1L) | 성공: 검증 assertThat(result.name()).isEqualTo("홍길동"); assertThat(result.schoolName()).isEqualTo("한양대학교ERICA"); assertThat(result.profileImageLink()).isEqualTo("https://cdn.example.com/profile.jpg"); assertThat(result.roles()).i... |
| [153](../../../src/test/java/com/umc/product/member/application/port/in/query/GetMemberUseCaseTest.java#L153) | getProfile / schoolId가 null이면 학교 조회를 하지 않는다 | 호출 getById(1L) | 성공: 검증 assertThat(result.schoolName()).isNull(); |
| [172](../../../src/test/java/com/umc/product/member/application/port/in/query/GetMemberUseCaseTest.java#L172) | getProfile / profileImageId가 null이면 파일 조회를 하지 않는다 | 호출 getById(1L) | 성공: 검증 assertThat(result.profileImageLink()).isNull(); |
| [190](../../../src/test/java/com/umc/product/member/application/port/in/query/GetMemberUseCaseTest.java#L190) | schoolId와 profileImageId 모두 null이면 둘 다 조회하지 않는다 | 호출 getById(1L) | 성공: 검증 assertThat(result.schoolName()).isNull(); assertThat(result.profileImageLink()).isNull(); |
| [209](../../../src/test/java/com/umc/product/member/application/port/in/query/GetMemberUseCaseTest.java#L209) | 회원이 존재하지 않으면 예외 | 호출 getById(999L)) | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_FOUND; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND); |
| [224](../../../src/test/java/com/umc/product/member/application/port/in/query/GetMemberUseCaseTest.java#L224) | getSchoolDetail이 null을 반환하면 schoolName은 null이다 | 호출 getById(1L) | 성공: 검증 assertThat(result.schoolName()).isNull(); |

### ManageMemberUseCaseTest
- 위치: `src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [75](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L75) | 일반적인 회원가입 성공 | 호출 register(command) | 성공: 검증 assertThat(memberId).isEqualTo(1L); |
| [105](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L105) | 학교가 존재하지 않으면 예외 | 호출 register(command)) | 실패: 예외 OrganizationDomainException; 에러코드 OrganizationErrorCode.SCHOOL_NOT_FOUND; 검증 .isEqualTo(OrganizationErrorCode.SCHOOL_NOT_FOUND); |
| [123](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L123) | 제공된 프로필 이미지가 존재하지 않으면 에러 발생 | 호출 register(command)) | 실패: 예외 StorageException; 에러코드 StorageErrorCode.FILE_NOT_FOUND; 검증 .isEqualTo(StorageErrorCode.FILE_NOT_FOUND); |
| [143](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L143) | 프로필 이미지 ID가 null이면 검증 스킵 | 호출 register(command) | 성공: 검증 assertThat(memberId).isEqualTo(1L); |
| [166](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L166) | 필수 약관 미동의시 예외 | 호출 register(command)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.MANDATORY_TERMS_NOT_AGREED; 검증 .isEqualTo(TermErrorCode.MANDATORY_TERMS_NOT_AGREED); |
| [186](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L186) | 필수 약관 일부만 동의시 예외 | 호출 register(command)) | 실패: 예외 TermDomainException; 에러코드 TermErrorCode.MANDATORY_TERMS_NOT_AGREED; 검증 .isEqualTo(TermErrorCode.MANDATORY_TERMS_NOT_AGREED); |
| [206](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L206) | 선택 약관만 미동의해도 성공 | 호출 register(command) | 성공: 검증 assertThat(memberId).isEqualTo(1L); |
| [260](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L260) | updateMember | 호출 updateMember(command) | 성공: 검증 assertThat(member.getProfileImageId()).isEqualTo("new_image_id"); |
| [278](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L278) | updateMember / 존재하지 않는 회원이면 예외 | 호출 updateMember(command)) | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_FOUND; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND); |
| [292](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L292) | updateMember / 존재하지 않는 프로필 이미지 ID면 예외 | 호출 updateMember(command)) | 실패: 예외 StorageException; 에러코드 StorageErrorCode.FILE_NOT_FOUND; 검증 .isEqualTo(StorageErrorCode.FILE_NOT_FOUND); |
| [308](../../../src/test/java/com/umc/product/member/application/port/in/command/ManageMemberUseCaseTest.java#L308) | updateMember / 프로필 이미지 ID가 null이면 검증 스킵 | 호출 updateMember(command) | 성공: updateMember / 프로필 이미지 ID가 null이면 검증 스킵 |

### MemberCredentialCommandServiceTest
- 테스트 설명: MemberCredentialCommandService
- 위치: `src/test/java/com/umc/product/member/application/service/MemberCredentialCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [24](../../../src/test/java/com/umc/product/member/application/service/MemberCredentialCommandServiceTest.java#L24) | credential 상태 조회는 member row를 lock으로 조회한다 | 호출 getCredentialStatusForUpdate(1L) | 성공: 검증 assertThat(result.memberId()).isEqualTo(1L); assertThat(result.hasCredential()).isTrue(); |
| [46](../../../src/test/java/com/umc/product/member/application/service/MemberCredentialCommandServiceTest.java#L46) | MemberCredentialCommandService / passwordHash가 없으면 credential 미등록 상태를 반환한다 | 호출 getCredentialStatusForUpdate(1L) | 성공: 검증 assertThat(result.memberId()).isEqualTo(1L); assertThat(result.hasCredential()).isFalse(); |
| [59](../../../src/test/java/com/umc/product/member/application/service/MemberCredentialCommandServiceTest.java#L59) | MemberCredentialCommandService / member가 없으면 MEMBER_NOT_FOUND를 던진다 | 호출 getCredentialStatusForUpdate(1L)) | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_FOUND; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND); |

### MemberEmailCommandServiceTest
- 테스트 설명: MemberEmailCommandService
- 위치: `src/test/java/com/umc/product/member/application/service/MemberEmailCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [25](../../../src/test/java/com/umc/product/member/application/service/MemberEmailCommandServiceTest.java#L25) | 새 이메일이 사용 가능하면 회원 이메일을 변경한다 | 호출 changeEmail(ChangeMemberEmailCommand.of(MEMBER_ID, NEW_EMAIL)) | 성공: 검증 assertThat(member.getEmail()).isEqualTo(NEW_EMAIL); |
| [52](../../../src/test/java/com/umc/product/member/application/service/MemberEmailCommandServiceTest.java#L52) | MemberEmailCommandService / 현재 이메일과 새 이메일이 같으면 중복 조회 없이 성공 처리한다 | 호출 changeEmail(ChangeMemberEmailCommand.of(MEMBER_ID, OLD_EMAIL)) | 실패: 검증 assertThat(member.getEmail()).isEqualTo(OLD_EMAIL); |
| [67](../../../src/test/java/com/umc/product/member/application/service/MemberEmailCommandServiceTest.java#L67) | MemberEmailCommandService / 새 이메일이 이미 사용 중이면 EMAIL_ALREADY_EXISTS 예외를 던지고 변경하지 않는다 | 호출 changeEmail(ChangeMemberEmailCommand.of(MEMBER_ID, NEW_EMAIL))) | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.EMAIL_ALREADY_EXISTS; 검증 .isEqualTo(MemberErrorCode.EMAIL_ALREADY_EXISTS); assertThat(member.getEmail()).isEqualTo(OLD_EMAIL); |
| [84](../../../src/test/java/com/umc/product/member/application/service/MemberEmailCommandServiceTest.java#L84) | MemberEmailCommandService / 회원이 없으면 MEMBER_NOT_FOUND 예외를 던진다 | 호출 changeEmail(ChangeMemberEmailCommand.of(MEMBER_ID, NEW_EMAIL))) | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_FOUND; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND); |

### MemberProfileCommandServiceTest
- 테스트 설명: MemberProfileCommandService
- 위치: `src/test/java/com/umc/product/member/application/service/MemberProfileCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [31](../../../src/test/java/com/umc/product/member/application/service/MemberProfileCommandServiceTest.java#L31) | 프로필이 없으면 새 프로필을 생성해 회원에 할당한다 | UpsertMemberProfileCommand {memberId=1L, of=new LinkTypeAndLink(MemberProfileLinkType.GITHUB, "https://github.com/umc"}; 호출 upsert(UpsertMemberProfileCommand.builder() | 성공: 검증 assertThat(member.getProfile()).isNotNull(); assertThat(member.getProfile().getGithub()).isEqualTo("https://github.com/umc"); |
| [62](../../../src/test/java/com/umc/product/member/application/service/MemberProfileCommandServiceTest.java#L62) | MemberProfileCommandService / 프로필이 있으면 기존 링크를 갱신한다 | UpsertMemberProfileCommand {memberId=1L, of=new LinkTypeAndLink(MemberProfileLinkType.BLOG, "https://blog.example.com"}; 호출 upsert(UpsertMemberProfileCommand.builder() | 성공: 검증 assertThat(profile.getGithub()).isNull(); assertThat(profile.getBlog()).isEqualTo("https://blog.example.com"); |
| [83](../../../src/test/java/com/umc/product/member/application/service/MemberProfileCommandServiceTest.java#L83) | MemberProfileCommandService / 프로필이 없으면 삭제할 수 없다 | 호출 delete(1L)) | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_PROFILE_NOT_FOUND; 검증 .isEqualTo(MemberErrorCode.MEMBER_PROFILE_NOT_FOUND); |
| [98](../../../src/test/java/com/umc/product/member/application/service/MemberProfileCommandServiceTest.java#L98) | MemberProfileCommandService / 프로필을 삭제하면 회원 연결을 제거하고 프로필을 삭제한다 | 호출 delete(1L) | 성공: 검증 assertThat(member.getProfile()).isNull(); |

### MemberQueryServiceBatchTest
- 테스트 설명: MemberQueryService batch 조회
- 위치: `src/test/java/com/umc/product/member/application/service/MemberQueryServiceBatchTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [31](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceBatchTest.java#L31) | findAllByIds는 빈 입력이면 외부 조회 없이 빈 Map을 반환한다 | 호출 findAllByIds(Set.of())).isEmpty(); 호출 findAllByIds(null)).isEmpty() | 성공: 검증 assertThat(sut.findAllByIds(Set.of())).isEmpty(); assertThat(sut.findAllByIds(null)).isEmpty(); |
| [57](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceBatchTest.java#L57) | MemberQueryService batch 조회 / findAllByIds는 같은 schoolId와 profileImageId를 한 번만 조회한다 | 호출 findAllByIds(Set.of(1L, 2L)) | 성공: 검증 assertThat(result).containsKeys(1L, 2L); assertThat(result.get(1L).schoolName()).isEqualTo("테스트대학교"); assertThat(result.get(2L).profileImageLink()).isEqualTo("https://cdn.example.com/profile-file-id"); |
| [75](../../../src/test/java/com/umc/product/member/application/service/MemberQueryServiceBatchTest.java#L75) | MemberQueryService batch 조회 / listIdsBySchoolIds는 조회 결과가 없는 학교도 빈 Set으로 채운다 | 호출 listIdsBySchoolIds(Set.of(10L, 20L)) | 성공: 검증 assertThat(result.get(10L)).containsExactlyInAnyOrder(1L, 2L); assertThat(result.get(20L)).isEmpty(); |

### MemberSearchServiceTest
- 위치: `src/test/java/com/umc/product/member/application/service/MemberSearchServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [109](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceTest.java#L109) | search | 호출 searchBy(defaultQuery, pageable) | 성공: 검증 assertThat(result.page().getContent()).hasSize(4); assertThat(result.page().getTotalElements()).isEqualTo(6); assertThat(result.page().getTotalPages()).isEqualTo(2); |
| [132](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceTest.java#L132) | search / 동일 회원의 다른 기수 챌린저가 각각 별도 행으로 반환된다 | 호출 searchBy(defaultQuery, pageable) | 성공: 검증 assertThat(content).hasSize(6); assertThat(memberTenCount).isEqualTo(2); |
| [157](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceTest.java#L157) | search / 챌린저별 역할 정보가 올바르게 매핑된다 | 호출 searchBy(defaultQuery, pageable) | 성공: 검증 assertThat(content.get(0).roleTypes()); .containsExactly(ChallengerRoleType.CENTRAL_PRESIDENT); assertThat(content.get(1).roleTypes()).isEmpty(); assertThat(content.get(2).roleTypes()) |
| [184](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceTest.java#L184) | gisuId에 따라 기수 번호가 올바르게 매핑된다 | 호출 searchBy(defaultQuery, pageable) | 성공: 검증 assertThat(content.get(0).gisu()).isEqualTo(7L); assertThat(content.get(1).gisu()).isEqualTo(7L); assertThat(content.get(2).gisu()).isEqualTo(7L); assertThat(content.get(3).gisu()).isEqualTo(8L); |
| [210](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceTest.java#L210) | 멤버 프로필이 없으면 null로 채워진다 | 호출 searchBy(defaultQuery, pageable) | 성공: 검증 assertThat(content.get(0).name()).isNull(); assertThat(content.get(0).nickname()).isNull(); assertThat(content.get(0).email()).isNull(); assertThat(content.get(0).schoolName()).isNull(); |
| [232](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceTest.java#L232) | 조회 결과가 없으면 빈 페이지를 반환한다 | 호출 searchBy(defaultQuery, pageable) | 성공: 검증 assertThat(result.page().getContent()).isEmpty(); assertThat(result.page().getTotalElements()).isZero(); |
| [248](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceTest.java#L248) | 회원 정보와 챌린저 정보가 올바르게 조합된다 | 호출 searchBy(defaultQuery, pageable) | 성공: 검증 assertThat(first.memberId()).isEqualTo(10L); assertThat(first.name()).isEqualTo("홍길동"); assertThat(first.nickname()).isEqualTo("hong"); assertThat(first.email()).isEqualTo("umcproduct@hanyang.ac.kr"); |

### MemberSearchServiceV2Test
- 테스트 설명: MemberSearchService.searchByV2 — 회원 단위 v2 검색
- 위치: `src/test/java/com/umc/product/member/application/service/MemberSearchServiceV2Test.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [74](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceV2Test.java#L74) | MemberSearchService.searchByV2 — 회원 단위 v2 검색 / 같은 회원의 여러 기수 챌린저는 하나의 row로 묶여 반환된다 | 조건 MemberSearchService.searchByV2 — 회원 단위 v2 검색 / 같은 회원의 여러 기수 챌린저는 하나의 row로 묶여 반환된다 | 성공: 검증 assertThat(content).hasSize(1); assertThat(content.get(0).memberId()).isEqualTo(10L); assertThat(content.get(0).participations()).hasSize(3); assertThat(content.get(0).primaryChallenger().challengerId()).isEqualTo(103... |
| [112](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceV2Test.java#L112) | 활성 기수 챌린저가 없으면 가장 최신 기수 챌린저가 대표로 선택된다 | 조건 활성 기수 챌린저가 없으면 가장 최신 기수 챌린저가 대표로 선택된다 | 성공: 검증 assertThat(item.primaryChallenger().challengerId()).isEqualTo(102L); assertThat(item.primaryChallenger().generation()).isEqualTo(8L); assertThat(item.isAdminInActiveGisu()).isFalse(); |
| [143](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceV2Test.java#L143) | 활성 기수 챌린저가 운영진이면 isAdminInActiveGisu가 true다 | 조건 활성 기수 챌린저가 운영진이면 isAdminInActiveGisu가 true다 | 성공: 검증 assertThat(result.page().getContent().get(0).isAdminInActiveGisu()).isTrue(); |
| [167](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceV2Test.java#L167) | 휴지기에는 isAdminInActiveGisu가 항상 false이다 | 조건 휴지기에는 isAdminInActiveGisu가 항상 false이다 | 성공: 검증 assertThat(result.page().getContent().get(0).isAdminInActiveGisu()).isFalse(); |
| [187](../../../src/test/java/com/umc/product/member/application/service/MemberSearchServiceV2Test.java#L187) | 검색결과가 비어있으면 빈 페이지를 반환한다 | 조건 검색결과가 비어있으면 빈 페이지를 반환한다 | 성공: 검증 assertThat(result.page().getContent()).isEmpty(); assertThat(result.page().getTotalElements()).isZero(); |

### MemberSummaryV2QueryServiceTest
- 테스트 설명: MemberSummaryV2QueryService — BFF 조립
- 위치: `src/test/java/com/umc/product/member/application/service/MemberSummaryV2QueryServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [91](../../../src/test/java/com/umc/product/member/application/service/MemberSummaryV2QueryServiceTest.java#L91) | MemberSummaryV2QueryService — BFF 조립 / 활성기수에 ACTIVE 챌린저와 운영진 기록이 있으면 그 정보가 세팅된다 | 호출 getSummaryByMemberId(100L) | 성공: 검증 assertThat(info.totalActivityDays()).isEqualTo(45L); assertThat(info.currentGisuMembership()).isNotNull(); assertThat(info.currentGisuMembership().gisuId()).isEqualTo(7L); assertThat(info.currentGisuMembership().chall... |
| [126](../../../src/test/java/com/umc/product/member/application/service/MemberSummaryV2QueryServiceTest.java#L126) | 활성기수가 없으면 currentGisuMembership은 null이다 | 호출 getSummaryByMemberId(100L) | 성공: 검증 assertThat(info.currentGisuMembership()).isNull(); assertThat(info.totalActivityDays()).isEqualTo(180L); assertThat(info.challengerHistory()).hasSize(1); |
| [157](../../../src/test/java/com/umc/product/member/application/service/MemberSummaryV2QueryServiceTest.java#L157) | 활성기수 챌린저가 EXPELLED면 challenger는 null이고 isAdmin은 운영진 기록을 따른다 | 호출 getSummaryByMemberId(100L) | 성공: 검증 assertThat(info.currentGisuMembership()).isNotNull(); assertThat(info.currentGisuMembership().challenger()).isNull(); assertThat(info.currentGisuMembership().isAdmin()).isFalse(); assertThat(info.totalActivityDays()).... |
| [187](../../../src/test/java/com/umc/product/member/application/service/MemberSummaryV2QueryServiceTest.java#L187) | 챌린저 이력이 없는 신규 회원도 정상 응답된다 | 호출 getSummaryByMemberId(100L) | 성공: 검증 assertThat(info.challengerHistory()).isEmpty(); assertThat(info.totalActivityDays()).isZero(); assertThat(info.currentGisuMembership()).isNotNull(); assertThat(info.currentGisuMembership().challenger()).isNull(); |
| [209](../../../src/test/java/com/umc/product/member/application/service/MemberSummaryV2QueryServiceTest.java#L209) | 로컬 자격증명이 있는 회원은 hasLocalCredential이 true이다 | 호출 getSummaryByMemberId(100L) | 성공: 검증 assertThat(info.hasLocalCredential()).isTrue(); |
| [227](../../../src/test/java/com/umc/product/member/application/service/MemberSummaryV2QueryServiceTest.java#L227) | 로컬 자격증명이 없는 회원은 hasLocalCredential이 false이다 | 호출 getSummaryByMemberId(100L) | 성공: 검증 assertThat(info.hasLocalCredential()).isFalse(); |

### SearchMemberUseCaseTest
- 위치: `src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [89](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L89) | 조건 없이 전체 검색 | 조건 조건 없이 전체 검색 | 성공: 검증 assertThat(result.page().getContent()).hasSize(2); assertThat(result.page().getTotalElements()).isEqualTo(2); |
| [114](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L114) | 조건 없이 전체 검색 / 한 회원이 여러 기수에 활동하면 각각 별도 행으로 반환된다 | 조건 조건 없이 전체 검색 / 한 회원이 여러 기수에 활동하면 각각 별도 행으로 반환된다 | 성공: 검증 assertThat(result.page().getContent()).hasSize(2); assertThat(result.page().getContent()) |
| [140](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L140) | 조건 없이 전체 검색 / 키워드 검색 | 조건 조건 없이 전체 검색 / 키워드 검색 | 성공: 검증 assertThat(result.page().getContent()).hasSize(1); assertThat(result.page().getContent().get(0).name()).isEqualTo("홍길동"); |
| [165](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L165) | 키워드 검색 / 이메일로 검색한다 | 조건 키워드 검색 / 이메일로 검색한다 | 성공: 검증 assertThat(result.page().getContent()).hasSize(1); assertThat(result.page().getContent().get(0).email()).isEqualTo("hong@umc.com"); |
| [188](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L188) | 키워드 검색 / 학교명으로 검색한다 | 조건 키워드 검색 / 학교명으로 검색한다 | 성공: 검증 assertThat(result.page().getContent()).hasSize(1); assertThat(result.page().getContent().get(0).name()).isEqualTo("홍길동"); |
| [211](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L211) | 닉네임으로 검색한다 | 조건 닉네임으로 검색한다 | 성공: 검증 assertThat(result.page().getContent()).hasSize(1); assertThat(result.page().getContent().get(0).nickname()).isEqualTo("hong123"); |
| [234](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L234) | 검색 결과가 없으면 빈 페이지를 반환한다 | 조건 검색 결과가 없으면 빈 페이지를 반환한다 | 성공: 검증 assertThat(result.page().getContent()).isEmpty(); assertThat(result.page().getTotalElements()).isZero(); |
| [256](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L256) | 필터링 | 조건 필터링 | 성공: 검증 assertThat(result.page().getContent()).hasSize(1); assertThat(result.page().getContent().get(0).name()).isEqualTo("홍길동"); |
| [281](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L281) | 필터링 / 파트로 필터링한다 | 조건 필터링 / 파트로 필터링한다 | 성공: 검증 assertThat(result.page().getContent()).hasSize(1); assertThat(result.page().getContent().get(0).name()).isEqualTo("김철수"); assertThat(result.page().getContent().get(0).part()).isEqualTo(ChallengerPart.WEB); |
| [305](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L305) | 필터링 / 학교로 필터링한다 | 조건 필터링 / 학교로 필터링한다 | 성공: 검증 assertThat(result.page().getContent()).hasSize(1); assertThat(result.page().getContent().get(0).name()).isEqualTo("김철수"); |
| [328](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L328) | 지부로 필터링한다 | 조건 지부로 필터링한다 | 성공: 검증 assertThat(result.page().getContent()).hasSize(1); assertThat(result.page().getContent().get(0).name()).isEqualTo("홍길동"); |
| [351](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L351) | 여러 필터를 동시에 적용한다 | 조건 여러 필터를 동시에 적용한다 | 성공: 검증 assertThat(result.page().getContent()).hasSize(2); assertThat(result.page().getContent()) |
| [382](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L382) | 역할 정보 매핑 | 조건 역할 정보 매핑 | 성공: 검증 assertThat(result.page().getContent()).hasSize(1); assertThat(result.page().getContent().get(0).roleTypes()); .containsExactly(ChallengerRoleType.SCHOOL_PRESIDENT); |
| [412](../../../src/test/java/com/umc/product/member/application/port/in/query/SearchMemberUseCaseTest.java#L412) | 역할 정보 매핑 / 페이징 | 조건 역할 정보 매핑 / 페이징 | 성공: 검증 assertThat(firstResult.page().getContent()).hasSize(3); assertThat(firstResult.page().getTotalElements()).isEqualTo(5); assertThat(firstResult.page().getTotalPages()).isEqualTo(2); assertThat(firstResult.page().hasNex... |

## Repository / Outbound Persistence

### MemberQueryRepositoryTest
- 테스트 설명: MemberQueryRepository 검색
- 위치: `src/test/java/com/umc/product/member/adapter/out/persistence/MemberQueryRepositoryTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [22](../../../src/test/java/com/umc/product/member/adapter/out/persistence/MemberQueryRepositoryTest.java#L22) | keyword는 학교명만 일치하는 회원을 검색하지 않는다 | 호출 searchBy(query, pageable); 호출 searchMemberIdsBy(query, pageable) | 성공: 검증 assertThat(challengers.getContent()); assertThat(challengers.getTotalElements()).isZero(); assertThat(memberIds.getContent()).isEmpty(); assertThat(memberIds.getTotalElements()).isZero(); |

## E2E / Integration

### MemberCommandControllerIntegrationTest
- 테스트 설명: MemberCommandController 통합 테스트
- 위치: `src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerIntegrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [39](../../../src/test/java/com/umc/product/member/adapter/in/web/MemberCommandControllerIntegrationTest.java#L39) | 이메일 회원가입에 성공하면 회원, 자격증명, 필수 약관 동의가 저장된다 | HTTP POST /api/v1/member/register/email | 성공: HTTP 200 OK; 검증 assertThat(savedMember.getName()).isEqualTo("홍길동"); assertThat(savedMember.getNickname()).isEqualTo("길동"); assertThat(savedMember.getSchoolId()).isEqualTo(school.getId()); assertThat(savedMember.getPasswordHash()) |

## Domain

### MemberCredentialTest
- 위치: `src/test/java/com/umc/product/member/domain/MemberCredentialTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [33](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L33) | 자격증명이 없는 회원은 최초 등록에 성공한다 | 조건 자격증명이 없는 회원은 최초 등록에 성공한다 | 성공: 검증 assertThat(member.getPasswordHash()).isEqualTo(ENCODED_PASSWORD); assertThat(member.hasCredential()).isTrue(); |
| [46](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L46) | 회원 자격 증명 등록 (이메일 기반) / 이미 자격증명이 등록된 회원은 재등록을 거부한다 | 조건 회원 자격 증명 등록 (이메일 기반) / 이미 자격증명이 등록된 회원은 재등록을 거부한다 | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.CREDENTIAL_ALREADY_REGISTERED; 검증 .isEqualTo(MemberErrorCode.CREDENTIAL_ALREADY_REGISTERED); |
| [59](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L59) | 회원 자격 증명 등록 (이메일 기반) / 비활성화 회원은 자격증명 등록을 거부한다 | 조건 회원 자격 증명 등록 (이메일 기반) / 비활성화 회원은 자격증명 등록을 거부한다 | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_ACTIVE; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_ACTIVE); |
| [72](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L72) | 회원 자격 증명 등록 (이메일 기반) / encodedPassword 가 null 이나 blank 이면 등록을 거부한다 | 조건 회원 자격 증명 등록 (이메일 기반) / encodedPassword 가 null 이나 blank 이면 등록을 거부한다 | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.INVALID_PASSWORD; 검증 .isEqualTo(MemberErrorCode.INVALID_PASSWORD); .isEqualTo(MemberErrorCode.INVALID_PASSWORD); |
| [89](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L89) | 회원 자격 증명 등록 (이메일 기반) / 자격증명이 등록된 회원은 비밀번호를 변경할 수 있다 | 조건 회원 자격 증명 등록 (이메일 기반) / 자격증명이 등록된 회원은 비밀번호를 변경할 수 있다 | 성공: 검증 assertThat(member.getPasswordHash()).isEqualTo(newEncoded); |
| [105](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L105) | changePassword / 자격증명이 등록되지 않은 회원은 비밀번호 변경을 거부한다 | 조건 changePassword / 자격증명이 등록되지 않은 회원은 비밀번호 변경을 거부한다 | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.CREDENTIAL_NOT_REGISTERED; 검증 .isEqualTo(MemberErrorCode.CREDENTIAL_NOT_REGISTERED); |
| [115](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L115) | changePassword / 비활성화 회원은 비밀번호 변경을 거부한다 | 조건 changePassword / 비활성화 회원은 비밀번호 변경을 거부한다 | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_ACTIVE; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_ACTIVE); |
| [129](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L129) | changePassword / 변경할 비밀번호가 null 이나 blank 이면 거부한다 | 조건 changePassword / 변경할 비밀번호가 null 이나 blank 이면 거부한다 | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.INVALID_PASSWORD; 검증 .isEqualTo(MemberErrorCode.INVALID_PASSWORD); .isEqualTo(MemberErrorCode.INVALID_PASSWORD); |
| [150](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L150) | changePassword / passwordHash 가 있으면 true 를 반환한다 | 조건 changePassword / passwordHash 가 있으면 true 를 반환한다 | 성공: 검증 assertThat(member.hasCredential()).isTrue(); |
| [160](../../../src/test/java/com/umc/product/member/domain/MemberCredentialTest.java#L160) | 회원 자격 증명 보유 관련 검증 / OAuth 전용 회원처럼 자격증명이 없으면 false 를 반환한다 | 조건 회원 자격 증명 보유 관련 검증 / OAuth 전용 회원처럼 자격증명이 없으면 false 를 반환한다 | 성공: 검증 assertThat(member.hasCredential()).isFalse(); |

### MemberProfileTest
- 테스트 설명: MemberProfile 도메인
- 위치: `src/test/java/com/umc/product/member/domain/MemberProfileTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [8](../../../src/test/java/com/umc/product/member/domain/MemberProfileTest.java#L8) | fromLinks는 링크 타입별 컬럼에 값을 매핑한다 | 조건 fromLinks는 링크 타입별 컬럼에 값을 매핑한다 | 성공: 검증 assertThat(profile.getLinkedIn()).isEqualTo("https://linkedin.com/in/umc"); assertThat(profile.getInstagram()).isEqualTo("https://instagram.com/umc"); assertThat(profile.getGithub()).isEqualTo("https://github.com/umc")... |
| [29](../../../src/test/java/com/umc/product/member/domain/MemberProfileTest.java#L29) | MemberProfile 도메인 / updateLinks는 기존 링크를 모두 초기화한 뒤 제공된 링크만 반영한다 | 조건 MemberProfile 도메인 / updateLinks는 기존 링크를 모두 초기화한 뒤 제공된 링크만 반영한다 | 성공: 검증 assertThat(profile.getGithub()).isNull(); assertThat(profile.getBlog()).isNull(); assertThat(profile.getPersonal()).isEqualTo("https://new.example.com"); |

### MemberTest
- 테스트 설명: Member 도메인
- 위치: `src/test/java/com/umc/product/member/domain/MemberTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [13](../../../src/test/java/com/umc/product/member/domain/MemberTest.java#L13) | create는 ACTIVE 상태 회원을 생성한다 | 조건 create는 ACTIVE 상태 회원을 생성한다 | 성공: 검증 assertThat(member.getName()).isEqualTo("홍길동"); assertThat(member.getNickname()).isEqualTo("길동"); assertThat(member.getEmail()).isEqualTo("gildong@example.com"); assertThat(member.getSchoolId()).isEqualTo(1L); |
| [31](../../../src/test/java/com/umc/product/member/domain/MemberTest.java#L31) | Member 도메인 / 활성 회원은 이메일을 변경할 수 있다 | 조건 Member 도메인 / 활성 회원은 이메일을 변경할 수 있다 | 성공: 검증 assertThat(member.getEmail()).isEqualTo("new@example.com"); |
| [43](../../../src/test/java/com/umc/product/member/domain/MemberTest.java#L43) | changeEmail / 비활성 회원은 이메일을 변경할 수 없다 | 조건 changeEmail / 비활성 회원은 이메일을 변경할 수 없다 | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_ACTIVE; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_ACTIVE); |
| [58](../../../src/test/java/com/umc/product/member/domain/MemberTest.java#L58) | changeEmail / nickname과 profileImageId를 부분 수정한다 | 조건 changeEmail / nickname과 profileImageId를 부분 수정한다 | 성공: 검증 assertThat(member.getNickname()).isEqualTo("하늘"); assertThat(member.getProfileImageId()).isEqualTo("new-file-id"); |
| [72](../../../src/test/java/com/umc/product/member/domain/MemberTest.java#L72) | updateProfile / 비활성 회원은 프로필을 수정할 수 없다 | 조건 updateProfile / 비활성 회원은 프로필을 수정할 수 없다 | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_ACTIVE; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_ACTIVE); |
| [87](../../../src/test/java/com/umc/product/member/domain/MemberTest.java#L87) | updateProfile / 프로필을 할당하고 제거할 수 있다 | 조건 updateProfile / 프로필을 할당하고 제거할 수 있다 | 성공: 검증 assertThat(member.getProfile()).isSameAs(profile); assertThat(member.getProfile()).isNull(); |
| [104](../../../src/test/java/com/umc/product/member/domain/MemberTest.java#L104) | profile association / 비활성 회원은 프로필을 할당할 수 없다 | 조건 profile association / 비활성 회원은 프로필을 할당할 수 없다 | 실패: 예외 MemberDomainException; 에러코드 MemberErrorCode.MEMBER_NOT_ACTIVE; 검증 .isEqualTo(MemberErrorCode.MEMBER_NOT_ACTIVE); |
