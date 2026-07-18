# Recruiting 테스트 케이스 카탈로그

이 문서는 `src/test/java/com/umc/product/recruiting` 아래의 `@Test`와 `@ParameterizedTest`를 전부 열거한다. 테스트를 추가하거나 제거하면 이 문서도 같은 PR에서 갱신한다.

- 테스트 클래스: 85개
- 실행 케이스: 381개
- 기준: 테스트 소스의 `@DisplayName`과 메서드명

## 계층별 현황

| 계층 | 클래스 | 케이스 |
|---|---:|---:|
| Domain | 10 | 76 |
| Application Command | 23 | 109 |
| Application Query | 9 | 28 |
| REST Adapter | 11 | 59 |
| GraphQL Adapter | 13 | 40 |
| Persistence / Migration | 16 | 54 |
| Outbound Adapter | 1 | 2 |
| Other | 2 | 13 |

## Domain

### `RecruitingCoreDomainTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingCoreDomainTest.java`

1. 학교와 기수를 기준으로 모집 시즌을 생성한다 (`학교와_기수를_기준으로_모집_시즌을_생성한다`, L24)
2. 본모집 차수는 1차로 고정된다 (`본모집_차수는_1차로_고정된다`, L33)
3. 추가모집 차수는 학교별 차수 번호를 가진다 (`추가모집_차수는_학교별_차수_번호를_가진다`, L42)
4. 추가모집 차수 번호는 1 이상이어야 한다 (`추가모집_차수_번호는_1_이상이어야_한다`, L51)
5. 지원 Form은 모집 차수와 form id를 가진다 (`지원_Form은_모집_차수와_form_id를_가진다`, L60)
6. 지원서는 draft에서 submitted로 제출된다 (`지원서는_draft에서_submitted로_제출된다`, L71)
7. 제출된 지원서는 서류 합격 후 최종 합격과 등록 대기 상태가 분리된다 (`제출된_지원서는_서류_합격_후_최종_합격과_등록_대기_상태가_분리된다`, L83)
8. 면접을 진행하지 않는 학교는 서류 합격 후 바로 최종 결정을 할 수 있다 (`면접을_진행하지_않는_학교는_서류_합격_후_바로_최종_결정을_할_수_있다`, L96)
9. 최종 합격 전에는 등록 대기 상태로 변경할 수 없다 (`최종_합격_전에는_등록_대기_상태로_변경할_수_없다`, L108)
10. 불합격과 철회만 다음 차수 재지원을 허용한다 (`불합격과_철회만_다음_차수_재지원을_허용한다`, L119)

### `RecruitingEvaluationDomainTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingEvaluationDomainTest.java`

1. 평가 확정에는 결정이 필요하다 (`평가_확정에는_결정이_필요하다`, L23)
2. 평가는 APPROVED 또는 REJECTED 결정으로 즉시 확정된다 (`평가는_APPROVED_또는_REJECTED_결정으로_즉시_확정된다`, L38)
3. 평가 의견은 2000자를 초과할 수 없다 (`평가_의견은_2000자를_초과할_수_없다`, L62)
4. 평가 의견은 2000자까지 저장할 수 있다 (`평가_의견은_2000자까지_저장할_수_있다`, L77)

### `RecruitingEvaluatorQuestionDomainTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingEvaluatorQuestionDomainTest.java`

1. 평가자는 모집 차수와 회원으로 생성된다 (`createRoundEvaluator`, L17)
2. 평가자 회원 식별자는 양수여야 한다 (`rejectInvalidEvaluatorMemberId`, L28)
3. 공통 면접 질문은 생성자와 최종 변경자를 기록한다 (`recordRoundInterviewQuestionCreatorAndLastModifier`, L39)
4. 공통 면접 질문을 비활성화한다 (`deactivateRoundInterviewQuestion`, L59)
5. 개별 면접 질문을 수정한다 (`updateApplicationInterviewQuestion`, L77)
6. 개별 면접 질문을 비활성화한다 (`deactivateApplicationInterviewQuestion`, L95)
7. 면접 질문 내용은 공백일 수 없다 (`rejectBlankInterviewQuestionContent`, L110)
8. 공통 면접 질문의 생성자 회원 식별자는 양수여야 한다 (`rejectInvalidRoundInterviewQuestionCreator`, L121)
9. 면접 질문 순서는 0 이상이어야 한다 (`rejectNegativeInterviewQuestionOrder`, L132)

### `RecruitingFormApplicationV2DomainTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingFormApplicationV2DomainTest.java`

1. 지원 폼은 트랙 없이 모집 차수당 하나의 Form을 연결한다 (`createSingleFormForRound`, L25)
2. 공통 섹션은 트랙이 없고 트랙 섹션은 모집 차수 트랙만 허용한다 (`validateSectionPolicyTrack`, L36)
3. 지원 폼 게시 검증은 모집 차수의 모든 트랙 섹션을 요구한다 (`requireEveryRecruitableTrackSectionBeforePublish`, L70)
4. 회원 지원서는 이메일을 정규화하고 개인정보 동의 없이 생성할 수 있다 (`createMemberDraftWithNormalizedEmail`, L84)
5. 실용 이메일 형식이 아니면 지원서를 생성할 수 없다 (`rejectMalformedApplicantEmail`, L102)
6. 지원자 이름에는 공백을 사용할 수 없다 (`rejectWhitespaceInApplicantName`, L116)
7. 2지망은 차수에서 활성화되고 1지망과 다른 모집 트랙이어야 한다 (`validateSecondChoice`, L130)
8. 합격 트랙은 1지망 또는 2지망 중 하나만 허용한다 (`validateAcceptedTrack`, L162)
9. 개인정보 약관과 동의 시각은 함께 기록한다 (`recordPrivacyConsentAsPair`, L181)

### `RecruitingInterviewScheduleDomainTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingInterviewScheduleDomainTest.java`

1. 면접 가능 시간 요청은 메일 대기 상태로 생성된다 (`면접_가능_시간_요청은_메일_대기_상태로_생성된다`, L23)
2. 가능 시간 응답 후 면접 일정을 확정한다 (`가능_시간_응답_후_면접_일정을_확정한다`, L37)
3. 가능 시간 응답 전에는 면접 일정을 확정할 수 없다 (`가능_시간_응답_전에는_면접_일정을_확정할_수_없다`, L54)
4. 면접 종료 시각은 시작 시각보다 늦어야 한다 (`면접_종료_시각은_시작_시각보다_늦어야_한다`, L70)
5. 메일 실패와 성공은 시도 횟수와 결과를 기록한다 (`메일_실패와_성공은_시도_횟수와_결과를_기록한다`, L87)

### `RecruitingRoundApplicationWindowTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingRoundApplicationWindowTest.java`

1. DRAFT 시즌은 접수할 수 없다 (`draftSeasonIsNotApplicationOpen`, L21)
2. DRAFT 차수는 접수할 수 없다 (`draftRoundIsNotApplicationOpen`, L33)
3. 접수 시작 시각은 접수 가능하다 (`documentStartIsInclusive`, L45)
4. 접수 종료 시각은 접수 가능하다 (`documentEndIsInclusive`, L55)
5. 게시되지 않은 Form은 접수할 수 없다 (`unpublishedFormIsNotApplicationOpen`, L65)
6. 접수 시작 전에는 접수할 수 없다 (`beforeDocumentWindowIsNotApplicationOpen`, L75)
7. 접수 종료 후에는 접수할 수 없다 (`afterDocumentWindowIsNotApplicationOpen`, L88)
8. CLOSED 차수는 접수할 수 없다 (`closedRoundIsNotApplicationOpen`, L101)
9. CLOSED 시즌은 접수할 수 없다 (`closedSeasonIsNotApplicationOpen`, L112)

### `RecruitingRoundConfigurationTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingRoundConfigurationTest.java`

1. 면접 차수 설정의 모든 필드를 보존한다 (`configuredRoundWithInterview`, L28)
2. 면접이 없는 차수는 면접 기간과 availability form 없이 생성한다 (`configuredRoundWithoutInterview`, L45)
3. 면접이 없는 차수는 availability form을 설정할 수 없다 (`configuredRoundWithoutInterviewRejectsAvailabilityForm`, L60)
4. 모집 트랙은 비어 있을 수 없다 (`configuredRoundRejectsEmptyTracks`, L69)
5. 모집 트랙은 중복될 수 없다 (`configuredRoundRejectsDuplicateTracks`, L75)
6. 모집 트랙에는 null을 설정할 수 없다 (`configuredRoundRejectsNullTrack`, L81)
7. 모집 트랙에는 INFRA_PLUS를 설정할 수 없다 (`configuredRoundRejectsInfraPlusTrack`, L87)

### `RecruitingRoundScheduleValidationTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingRoundScheduleValidationTest.java`

1. 모집 차수는 필수 서류 일정이 모두 필요하다 (`configuredRoundRejectsMissingRequiredSchedule`, L26)
2. 문서 접수 종료는 시작보다 빠를 수 없다 (`configuredRoundRejectsMalformedDocumentWindow`, L40)
3. 서류 결과 발표는 문서 접수 종료보다 빠를 수 없다 (`configuredRoundRejectsDocumentResultBeforeDocumentEnd`, L54)
4. 면접 시작은 서류 결과 발표보다 빠를 수 없다 (`configuredRoundRejectsInterviewBeforeDocumentResult`, L68)
5. 면접 필수 차수는 면접 시작 시각이 필요하다 (`interviewRequiredRoundRejectsMissingInterviewStart`, L82)
6. 면접 필수 차수는 면접 종료 시각이 필요하다 (`interviewRequiredRoundRejectsMissingInterviewEnd`, L96)
7. 면접 종료는 면접 시작보다 빠를 수 없다 (`configuredRoundRejectsMalformedInterviewWindow`, L110)
8. 최종 결과 발표는 면접 종료보다 빠를 수 없다 (`configuredRoundRejectsFinalResultBeforeInterviewEnd`, L124)
9. 면접 미진행 차수는 면접 시작 시각을 가질 수 없다 (`noInterviewRoundRejectsInterviewStart`, L138)
10. 면접 미진행 차수는 면접 종료 시각을 가질 수 없다 (`noInterviewRoundRejectsInterviewEnd`, L152)
11. 면접이 없으면 최종 결과 발표는 서류 결과 발표보다 빠를 수 없다 (`noInterviewRoundRejectsFinalResultBeforeDocumentResult`, L166)

### `RecruitingSeasonLifecycleTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingSeasonLifecycleTest.java`

1. 새 모집 시즌은 DRAFT 상태이다 (`createdSeasonIsDraft`, L18)
2. 모집 시즌은 DRAFT에서 ACTIVE로 전이한다 (`seasonActivatesFromDraft`, L26)
3. 모집 시즌은 ACTIVE에서 CLOSED로 전이한다 (`seasonClosesFromActive`, L36)
4. 모집 시즌은 상태 전이 순서를 건너뛸 수 없다 (`seasonRejectsInvalidTransition`, L46)
5. 새 모집 차수는 DRAFT 상태이다 (`createdRoundIsDraft`, L57)
6. 모집 차수는 DRAFT에서 OPEN으로 전이한다 (`roundOpensFromDraft`, L65)
7. 모집 차수는 OPEN에서 CLOSED로 전이한다 (`roundClosesFromOpen`, L75)
8. 모집 차수는 상태 전이 순서를 건너뛸 수 없다 (`roundRejectsInvalidTransition`, L85)

### `RecruitingSeasonTrackQuotaTest`

소스: `src/test/java/com/umc/product/recruiting/domain/RecruitingSeasonTrackQuotaTest.java`

1. 시즌 트랙 쿼터는 목표 인원 0명으로 생성할 수 있다 (`quotaAcceptsZeroTargetCountOnCreate`, L17)
2. 시즌 트랙 쿼터 목표 인원을 변경할 수 있다 (`quotaUpdatesTargetCount`, L30)
3. 시즌 트랙 쿼터는 음수 목표 인원을 거부한다 (`quotaRejectsNegativeTargetCount`, L44)
4. 시즌 쿼터에는 INFRA_PLUS 트랙을 설정할 수 없다 (`quotaRejectsInfraPlus`, L57)

## Application Command

### `RecruitingApplicationCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationCommandServiceTest.java`

1. 로그인 회원 지원서 생성은 정규화 이메일과 생성 전용 지원 키를 반환한다 (`createMemberDraft`, L98)
2. 로그인 회원 ID가 없으면 Form 응답을 만들기 전에 거부한다 (`rejectDraftWithoutMember`, L129)
3. 익명 지원서 생성은 개인정보 동의를 검증하고 Form raw key를 내부에만 보관한다 (`createAnonymousDraft`, L144)
4. 익명 지원서 생성은 활성 개인정보 약관과 요청 약관이 다르면 Form 응답 생성 전에 거부한다 (`rejectAnonymousDraftWithInactivePrivacyTerm`, L183)
5. 로그인 회원 지원서 수정은 기본 정보와 Form 응답을 함께 갱신한다 (`updateMemberDraft`, L207)
6. 다른 회원의 지원서 수정은 중복 지원 조회 전에 거부한다 (`rejectUpdateByDifferentMemberBeforeValidation`, L232)
7. 지원서 제출은 선택 섹션의 allowed와 required 문항 ID를 Form 응답에 전달한다 (`submitWithSelectedQuestionScope`, L256)
8. 제출 완료 익명 지원서 수정은 scoped Form 수정 API를 사용하고 제출 상태를 유지한다 (`updateSubmittedAnonymousApplication`, L288)
9. 익명 지원서 수정은 선택한 트랙 scope 밖 질문을 Form 호출 전에 거부한다 (`rejectAnonymousAnswerOutsideSelectedTrackScope`, L322)
10. 익명 지원서 제출은 내부 Form access key와 선택 문항 scope를 전달한다 (`submitAnonymousApplication`, L352)
11. 지원서 철회는 Form 응답을 삭제하지 않고 Recruiting 상태만 변경한다 (`cancelDoesNotDeleteFormResponse`, L377)

### `RecruitingApplicationEvaluationCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationEvaluationCommandServiceTest.java`

1. 평가자 whitelist에 등록된 회원은 평가를 즉시 확정한다 (`평가자_whitelist에_등록된_회원은_평가를_즉시_확정한다`, L72)
2. 평가자 whitelist에 없는 회원은 평가를 만들 수 없다 (`평가자_whitelist에_없는_회원은_평가를_만들_수_없다`, L95)
3. 이미 확정한 평가는 다시 등록할 수 없다 (`이미_확정한_평가는_다시_등록할_수_없다`, L114)
4. INTERVIEW 배정 전에는 면접 평가를 확정할 수 없다 (`면접_배정_전에는_면접_평가를_확정할_수_없다`, L146)

### `RecruitingApplicationFormCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormCommandServiceTest.java`

1. 모집 차수에는 트랙 없이 하나의 지원 Form만 연결한다 (`linkSingleFormToRound`, L62)
2. 같은 모집 차수에 두 번째 지원 Form을 연결할 수 없다 (`rejectSecondFormForRound`, L87)
3. 지원 Form 게시 전 섹션 정책 검증 seam을 호출한다 (`validatePoliciesBeforePublish`, L105)
4. 지원 Form root lock을 획득한 뒤 마감 상태를 검사한다 (`lockFormBeforeCloseStateCheck`, L127)
5. 다른 시즌의 차수에는 지원 Form을 연결하지 않는다 (`rejectLinkForRoundInDifferentSeasonBeforeSave`, L147)
6. 다른 시즌의 지원 Form은 게시하지 않는다 (`rejectPublishForFormInDifferentSeasonBeforeMutation`, L165)
7. 다른 시즌의 지원 Form은 마감하지 않는다 (`rejectCloseForFormInDifferentSeasonBeforeMutation`, L186)

### `RecruitingApplicationFormValidationServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationFormValidationServiceTest.java`

1. 게시할 Form에 정책 없는 section이 있으면 거부한다 (`rejectSectionWithoutPolicy`, L48)
2. TRACK section의 조건부 이동이 다른 TRACK section을 향하면 거부한다 (`rejectConditionalTransitionToDifferentTrack`, L70)
3. 게시 검증이 완료되면 Form의 TRACK section 집합을 반환한다 (`returnValidatedTrackSections`, L91)

### `RecruitingApplicationInterviewQuestionCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationInterviewQuestionCommandServiceTest.java`

1. 최초 면접 평가 제출 전에는 차수 평가자가 개별 질문을 수정할 수 있다 (`updateBeforeFirstSubmission`, L69)
2. 첫 면접 평가 제출 후에는 개별 질문 생성을 거부한다 (`rejectCreationAfterFirstSubmission`, L94)
3. 첫 면접 평가 제출 후에는 개별 질문 수정을 거부한다 (`rejectUpdateAfterFirstSubmission`, L110)
4. 첫 면접 평가 제출 후에는 개별 질문 비활성화를 거부한다 (`rejectDeactivationAfterFirstSubmission`, L132)
5. 차수 평가자가 아니면 개별 질문을 수정할 수 없다 (`rejectUpdateForNonEvaluator`, L154)
6. 요청 지원서와 질문 지원서가 다르면 개별 질문 수정을 거부한다 (`rejectUpdateForDifferentApplicationScope`, L175)

### `RecruitingApplicationInterviewQuestionPreSubmissionCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationInterviewQuestionPreSubmissionCommandServiceTest.java`

1. 최초 면접 평가 제출 전에는 차수 평가자가 개별 질문을 생성할 수 있다 (`createBeforeFirstSubmission`, L60)
2. 최초 면접 평가 제출 전에는 차수 평가자가 개별 질문을 비활성화할 수 있다 (`deactivateBeforeFirstSubmission`, L80)

### `RecruitingApplicationKeyIssuerTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationKeyIssuerTest.java`

1. 이메일과 충돌하지 않는 6자리 지원 키를 발급한다 (`issueNonConflictingKey`, L24)
2. 지원 키가 10회 연속 충돌하면 명시적인 오류를 던진다 (`failAfterTenCollisions`, L39)
3. 9회 충돌 후 10번째 후보가 유일하면 발급한다 (`issueTenthCandidateAfterNineCollisions`, L58)

### `RecruitingApplicationValidationServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingApplicationValidationServiceTest.java`

1. 같은 차수의 회원 또는 이메일 지원서는 상태와 무관하게 중복을 거부한다 (`rejectSameRoundMemberOrEmail`, L47)
2. 같은 기수 다른 학교의 진행 중 회원 또는 이메일 지원서를 거부한다 (`rejectBlockingApplicationAtDifferentSchool`, L59)
3. 같은 기수의 진행 중이거나 합격한 지원서가 있으면 재지원을 거부한다 (`rejectBlockingReapplication`, L77)
4. ACTIVE 시즌과 OPEN 차수의 local 접수 기간에는 지원할 수 있다 (`allowOpenLocalApplicationPeriod`, L94)
5. local 접수 종료 이후에는 지원할 수 없다 (`rejectClosedLocalApplicationPeriod`, L105)
6. Form 응답의 회원과 Form 연결이 일치하면 수정할 수 있다 (`allowOwnedLinkedFormResponse`, L119)
7. Form 응답의 회원 또는 Form 연결이 다르면 수정을 거부한다 (`rejectMismatchedFormResponseOwnership`, L131)

### `RecruitingDecisionCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingDecisionCommandServiceTest.java`

1. 면접 없는 차수의 서류 합격은 즉시 INTERVIEW_SKIPPED로 전환한다 (`passDocumentSkipsInterviewWhenRoundDoesNotRequireInterview`, L59)
2. 면접 차수의 서류 합격은 배정 상태와 가능 일정 요청을 함께 생성한다 (`passDocumentAssignsInterviewAndRequestsAvailability`, L78)
3. 서류 불합격은 면접 일정 요청을 생성하지 않는다 (`failDocumentDoesNotRequestAvailability`, L103)
4. 학교 회장단은 지원한 트랙을 선택해 최종 합격시키고 등록 상태는 NOT_READY로 둔다 (`schoolCoreDecidesFinalPassWithAcceptedTrack`, L121)
5. 중앙 총괄단은 최종 합격을 판정할 수 있다 (`centralCoreDecidesFinalPass`, L136)
6. 평가자 whitelist만으로는 최종 판정을 할 수 없다 (`evaluatorWhitelistCannotDecideFinal`, L148)
7. 최종 합격에는 1지망 또는 2지망 acceptedTrack이 필요하다 (`finalPassRequiresAppliedTrack`, L162)
8. 같은 target gisu에서 이미 최종 합격한 지원자가 있으면 다른 학교 지원도 합격시킬 수 없다 (`finalPassRejectsDuplicateAcrossSchools`, L175)

### `RecruitingFormSectionPolicyCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingFormSectionPolicyCommandServiceTest.java`

1. Form 공개 조회로 section 소속을 확인하고 TRACK 정책을 저장한다 (`addTrackPolicyAfterCheckingFormSection`, L58)
2. section policy type이 없으면 TRACK으로 간주하지 않고 거부한다 (`rejectMissingPolicyType`, L87)
3. 게시된 Form에는 section policy를 추가할 수 없다 (`rejectPolicyForPublishedForm`, L109)
4. 마감된 Form에는 section policy를 추가할 수 없다 (`rejectPolicyForClosedForm`, L124)

### `RecruitingInterviewAvailabilityRequestCoordinatorTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingInterviewAvailabilityRequestCoordinatorTest.java`

1. 최초 요청은 일정 row와 Outbox 이벤트를 함께 생성한다 (`최초_요청은_일정_row와_Outbox_이벤트를_함께_생성한다`, L61)
2. PENDING 또는 SENT 요청은 기존 일정을 반환하고 이벤트를 중복 발행하지 않는다 (`진행_중이거나_발송된_요청은_멱등_성공한다`, L81)
3. FAILED 요청은 PENDING으로 바꾸고 재시도 이벤트를 발행한다 (`실패한_요청은_재시도_이벤트를_발행한다`, L95)

### `RecruitingInterviewCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingInterviewCommandServiceTest.java`

1. 면접 일정 후보 조회는 기존 overlap 경계에 위임한다 (`면접_일정_후보_조회는_기존_overlap_경계에_위임한다`, L54)
2. 면접 생략은 지원서 도메인 전이를 저장한다 (`면접_생략은_지원서_도메인_전이를_저장한다`, L76)

### `RecruitingInterviewMailDeliveryCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingInterviewMailDeliveryCommandServiceTest.java`

1. 메일 발송 성공은 발송 시각과 상태를 저장한다 (`메일_발송_성공은_상태를_저장한다`, L40)
2. 이미 발송된 일정은 실패 이벤트가 뒤늦게 도착해도 상태를 되돌리지 않는다 (`이미_발송된_일정은_실패로_되돌리지_않는다`, L52)

### `RecruitingInterviewScheduleCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingInterviewScheduleCommandServiceTest.java`

1. 수동 면접 가능 시간 요청은 멱등 coordinator에 위임한다 (`수동_면접_가능_시간_요청은_멱등_coordinator에_위임한다`, L74)
2. 가능 시간 응답을 기록하고 일정을 확정한다 (`가능_시간_응답을_기록하고_일정을_확정한다`, L91)
3. 다른 회원은 지원자의 가능 시간 응답 ID를 기록할 수 없다 (`다른_회원은_가능_시간_응답을_기록할_수_없다`, L112)

### `RecruitingManagementAuthorizationServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingManagementAuthorizationServiceTest.java`

1. 공통 질문 관리는 대상 시즌의 EDIT 권한을 요구한다 (`authorizeSeasonManagementWithEditPermission`, L28)

### `RecruitingRegistrationCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRegistrationCommandServiceTest.java`

1. 중앙 총괄단은 quota row를 잠근 뒤 Season 전체 사용량이 남으면 READY를 예약한다 (`centralCorePreparesRegistrationWithQuotaLock`, L73)
2. 학교 회장단은 등록 준비를 할 수 없다 (`schoolCoreCannotPrepareRegistration`, L90)
3. READY와 REGISTERED 합계가 targetCount에 도달하면 추가 READY를 거부한다 (`fullQuotaRejectsPreparation`, L104)
4. READY 취소는 NOT_READY로 되돌려 예약 자리를 반환한다 (`cancelReadyReturnsCapacity`, L121)
5. READY 등록 확정은 Challenger 공개 usecase로 신규 생성 또는 track 멱등 추가 후 REGISTERED가 된다 (`confirmReadyRegistersChallengerTrack`, L135)
6. REGISTERED는 READY 취소로 되돌릴 수 없다 (`registeredCannotBeCancelled`, L154)

### `RecruitingRoundCreateCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundCreateCommandServiceTest.java`

1. 양수 쿼터 트랙으로 추가 모집 차수를 생성한다 (`createAdditionalRound`, L55)
2. 같은 시즌 타입 차수는 중복 생성할 수 없다 (`createRoundRejectsDuplicate`, L72)
3. 시즌 쿼터에 없는 트랙으로 차수를 생성할 수 없다 (`createRoundRejectsTrackOutsideSeasonQuota`, L89)
4. 목표 인원이 0명인 트랙으로 차수를 생성할 수 없다 (`createRoundRejectsZeroTargetQuotaTrack`, L106)

### `RecruitingRoundEvaluatorCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundEvaluatorCommandServiceTest.java`

1. 학교 또는 중앙 모집 관리 권한을 확인한 뒤 평가자를 추가한다 (`addEvaluatorAfterManagementAuthorization`, L62)
2. 같은 차수의 평가자는 중복 추가할 수 없다 (`rejectDuplicateEvaluator`, L86)
3. 모집 관리 권한이 없으면 평가자 whitelist를 변경할 수 없다 (`rejectEvaluatorMutationWithoutManagementPermission`, L107)

### `RecruitingRoundEvaluatorRemovalCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundEvaluatorRemovalCommandServiceTest.java`

1. 실제 시즌 관리 권한을 확인한 뒤 평가자를 제거한다 (`removeEvaluatorAfterActualSeasonAuthorization`, L56)
2. 실제 시즌 관리 권한이 없으면 평가자 조회와 삭제 전에 중단한다 (`rejectRemovalBeforeEvaluatorLookupWithoutActualSeasonPermission`, L74)

### `RecruitingRoundInterviewQuestionCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundInterviewQuestionCommandServiceTest.java`

1. 최초 면접 평가 제출 전에는 공통 질문을 수정할 수 있다 (`updateBeforeFirstSubmission`, L72)
2. 첫 면접 평가 제출 후에는 공통 질문 생성을 거부한다 (`rejectCreationAfterFirstSubmission`, L100)
3. 첫 면접 평가 제출 후에는 공통 질문 수정을 거부한다 (`rejectUpdateAfterFirstSubmission`, L116)
4. 첫 면접 평가 제출 후에는 공통 질문 비활성화를 거부한다 (`rejectDeactivationAfterFirstSubmission`, L143)
5. 모집 관리 권한이 없으면 공통 질문을 수정할 수 없다 (`rejectUpdateWithoutManagementPermission`, L166)
6. 요청 차수와 질문 차수가 다르면 공통 질문 수정을 거부한다 (`rejectUpdateForDifferentRoundScope`, L188)

### `RecruitingRoundInterviewQuestionPreSubmissionCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundInterviewQuestionPreSubmissionCommandServiceTest.java`

1. 최초 면접 평가 제출 전에는 공통 질문을 생성할 수 있다 (`createBeforeFirstSubmission`, L60)
2. 최초 면접 평가 제출 전에는 공통 질문을 비활성화할 수 있다 (`deactivateBeforeFirstSubmission`, L77)

### `RecruitingRoundUpdateCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingRoundUpdateCommandServiceTest.java`

1. 양수 쿼터 트랙으로 차수 설정을 변경한다 (`updateRoundConfiguration`, L55)
2. 지원서가 있으면 DRAFT 차수의 모집 트랙을 변경할 수 없다 (`rejectRecruitableTrackChangeWhenApplicationExists`, L75)
3. 지원서가 있으면 DRAFT 차수의 2지망 정책을 변경할 수 없다 (`rejectSecondChoicePolicyChangeWhenApplicationExists`, L98)
4. OPEN 차수의 모집 트랙은 지원서가 없어도 변경할 수 없다 (`rejectRecruitableTrackChangeWhenRoundIsOpen`, L121)
5. OPEN 차수에서도 모집 정책을 유지하면 일정과 공지를 변경할 수 있다 (`updateScheduleAndAnnouncementWhileOpen`, L144)
6. CLOSED 차수에서도 모집 정책을 유지하면 일정과 연락처를 변경할 수 있다 (`updateScheduleAndContactWhileClosed`, L167)
7. 다른 시즌의 차수 설정을 변경할 수 없다 (`updateRoundRejectsDifferentSeason`, L191)
8. 같은 시즌의 차수 상태를 OPEN으로 변경한다 (`updateRoundStatus`, L207)
9. 다른 시즌의 차수 상태를 변경할 수 없다 (`updateRoundStatusRejectsDifferentSeason`, L223)

### `RecruitingSeasonCommandServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/command/RecruitingSeasonCommandServiceTest.java`

1. 학교와 기수 조합이 없으면 모집 시즌과 쿼터를 생성한다 (`createSeason`, L63)
2. 모집 시즌 생성 command는 기수가 필수이다 (`createSeasonRequiresGisu`, L90)
3. 모집 시즌 생성 command는 학교가 필수이다 (`createSeasonRequiresSchool`, L102)
4. 같은 학교와 기수의 모집 시즌을 중복 생성할 수 없다 (`createSeasonRejectsDuplicateSeason`, L114)
5. 시즌 쿼터는 같은 트랙을 중복 설정할 수 없다 (`createSeasonRejectsDuplicateQuotaTracks`, L132)
6. 다른 학교 회장단은 요청 학교의 시즌을 생성할 수 없다 (`createSeasonRejectsDifferentSchoolPresident`, L153)
7. 다른 기수 중앙 회장단은 요청 기수의 시즌을 생성할 수 없다 (`createSeasonRejectsDifferentGisuCentralPresident`, L173)
8. 시즌 쿼터는 0명을 포함해 전체 교체할 수 있다 (`replaceSeasonQuotas`, L193)
9. TO는 현재 READY와 REGISTERED 합계보다 작게 줄일 수 없다 (`replaceQuotaCannotGoBelowReservedAndRegistered`, L215)
10. 기존 차수의 모집 트랙은 쿼터 교체 후에도 양수여야 한다 (`replaceQuotasRequiresPositiveTargetForExistingRound`, L239)
11. 모집 시즌 상태를 DRAFT에서 ACTIVE로 변경한다 (`updateSeasonStatus`, L260)

## Application Query

### `RecruitingApplicationEvaluationQueryServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingApplicationEvaluationQueryServiceTest.java`

1. 본인 평가 등록 전에는 같은 단계의 다른 평가를 조회할 수 없다 (`본인_평가_등록_전에는_같은_단계의_다른_평가를_조회할_수_없다`, L74)
2. 본인 평가 등록 후에는 같은 단계의 다른 평가를 조회한다 (`본인_평가_등록_후에는_같은_단계의_다른_평가를_조회한다`, L94)
3. 학교와 중앙 권한자는 whitelist와 제출 여부를 우회한다 (`학교와_중앙_권한자는_whitelist와_제출_여부를_우회한다`, L120)
4. whitelist에 없는 일반 회원은 평가를 조회할 수 없다 (`whitelist에_없는_일반_회원은_평가를_조회할_수_없다`, L141)

### `RecruitingApplicationQuestionScopeQueryServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingApplicationQuestionScopeQueryServiceTest.java`

1. COMMON과 1지망 및 2지망 섹션 문항만 허용하고 required 문항만 필수로 반환한다 (`resolveSelectedQuestionScope`, L48)

### `RecruitingCsvExportServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingCsvExportServiceTest.java`

1. CSV는 정확한 헤더와 마스킹 이메일만 포함한다 (`exportSummaryCsvUsesExactHeaderAndMaskedEmail`, L41)
2. CSV는 spreadsheet 수식으로 해석되는 셀 접두사를 중화하고 RFC4180 escaping을 유지한다 (`exportSummaryCsvNeutralizesFormulaPrefixesAndPreservesRfc4180Escaping`, L60)
3. 다른 기수의 중앙 총괄단은 CSV를 export할 수 없다 (`rejectCsvExportForCentralCoreFromDifferentGisu`, L91)

### `RecruitingEvaluatorQuestionQueryServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingEvaluatorQuestionQueryServiceTest.java`

1. 차수 평가자 whitelist가 있으면 서류와 면접 평가 권한이 있다 (`canEvaluateForRoundWhitelist`, L64)
2. 차수 평가자 whitelist가 없으면 평가 권한이 없다 (`cannotEvaluateWithoutRoundWhitelist`, L75)
3. 평가자 목록은 차수에 등록된 평가자를 반환한다 (`listEvaluatorsByRound`, L86)
4. 차수 평가자는 공통 질문을 조회한다 (`evaluatorListsActiveRoundQuestions`, L103)
5. 차수 평가자는 지원서의 개별 질문을 조회한다 (`evaluatorListsActiveApplicationQuestions`, L127)
6. 모집 관리자는 평가자 whitelist 없이 공통 질문을 조회한다 (`managerBypassesEvaluatorWhitelistForRoundQuestions`, L156)
7. 관리 권한과 평가자 whitelist가 없으면 질문 조회를 거부한다 (`rejectQuestionReadWithoutManagementOrEvaluatorWhitelist`, L174)

### `RecruitingInterviewMailDeliveryQueryServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingInterviewMailDeliveryQueryServiceTest.java`

1. 면접 일정 요청 메일 정보는 지원서와 일정 스냅샷에서 조회한다 (`면접_일정_요청_메일_정보를_조회한다`, L36)

### `RecruitingInterviewScheduleQueryServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingInterviewScheduleQueryServiceTest.java`

1. 지원서 면접 일정이 있으면 기본 정보를 조회한다 (`지원서_면접_일정이_있으면_기본_정보를_조회한다`, L36)
2. 지원서 면접 일정이 없으면 빈 결과를 반환한다 (`지원서_면접_일정이_없으면_빈_결과를_반환한다`, L57)

### `RecruitingPublicApplicationQueryServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingPublicApplicationQueryServiceTest.java`

1. 최종 발표 전에는 최종 결과와 합격 트랙을 숨긴다 (`hideFinalResultBeforePublication`, L54)
2. 최종 발표 시각부터 최종 결과와 합격 트랙을 공개한다 (`exposeFinalResultAtPublicationBoundary`, L71)
3. 지원 키 형식이 잘못되면 지원서 조회 전에 거부한다 (`rejectInvalidApplicationKey`, L87)

### `RecruitingQueryServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingQueryServiceTest.java`

1. 상태_요약은_summary_row의_지원서_상태를_집계한다 (`summarizeApplicationStatuses`, L41)
2. 다른 기수의 중앙 총괄단은 상태 요약을 조회할 수 없다 (`rejectStatusSummaryForCentralCoreFromDifferentGisu`, L61)
3. SUPER_ADMIN은 기수와 무관하게 상태 요약을 조회할 수 있다 (`superAdminReadsStatusSummaryAcrossGisu`, L72)

### `RecruitingSeasonQueryServiceTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/query/RecruitingSeasonQueryServiceTest.java`

1. 시즌의 쿼터와 차수 설정을 조회한다 (`getSeasonConfiguration`, L58)
2. 시즌 목록은 현재 학교의 지부 소속을 기준으로 필터링하고 차수를 함께 반환한다 (`searchSeasonsByCurrentChapter`, L100)
3. 차수 목록은 시즌 필터를 적용한다 (`searchRoundsBySeason`, L131)
4. 차수 목록에서 조회 권한이 없는 시즌은 제외한다 (`searchRoundsExcludesUnauthorizedSeason`, L159)

## REST Adapter

### `RecruitingAdminControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingAdminControllerTest.java`

1. 모집 폼 연결 API는 차수와 Form ID command를 전달한다 (`모집_폼_연결_API는_차수와_Form_ID를_전달한다`, L113)
2. 모집 폼 게시 API는 시즌과 요청자 ID를 command로 전달한다 (`publishFormBindsSeasonAndRequester`, L132)
3. 모집 폼 마감 API는 시즌 ID를 command로 전달한다 (`closeFormBindsSeason`, L149)
4. 최종 결정 API는 결정자 memberId를 command로 전달한다 (`최종_결정_API는_결정자_memberId를_command로_전달한다`, L165)
5. 서류 결정 API는 acceptedTrack 없이 결정자와 합불을 전달한다 (`서류_결정_API는_결정자와_합불을_전달한다`, L183)
6. 서류 결정 API는 DB 제한을 넘는 사유를 거부한다 (`서류_결정_API는_긴_사유를_거부한다`, L201)
7. 면접 생략 API는 CurrentMember와 사유를 전달한다 (`면접_생략_API는_CurrentMember와_사유를_전달한다`, L214)
8. 면접 생략 API는 DB 제한을 넘는 사유를 거부한다 (`면접_생략_API는_긴_사유를_거부한다`, L231)
9. READY API는 CurrentMember actor를 use case로 전달한다 (`readyUsesCurrentMemberActor`, L244)
10. CSV export API는 raw email과 지원서 본문 없이 attachment를 반환한다 (`CSV_export_API는_raw_email과_지원서_본문_없이_attachment를_반환한다`, L256)
11. 상태 요약 API는 status별 count를 반환한다 (`상태_요약_API는_status별_count를_반환한다`, L283)

### `RecruitingApplicationControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingApplicationControllerTest.java`

1. 지원서 draft 생성 API는 인증 actor와 지원 기본 정보를 command로 전달한다 (`createDraftUsesAuthenticatedActor`, L26)
2. 비로그인 회원의 지원서 생성 요청은 거부한다 (`rejectUnauthenticatedCreate`, L59)
3. 지원서 생성 API는 잘못된 이메일만 있는 요청을 거부한다 (`rejectMalformedEmailWhenOtherCreateFieldsAreValid`, L70)
4. 지원서 생성 API는 비양수 Form ID만 있는 요청을 거부한다 (`rejectNonPositiveFormIdWhenOtherCreateFieldsAreValid`, L82)

### `RecruitingApplicationMutationControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingApplicationMutationControllerTest.java`

1. 지원서 수정 API는 잘못된 이메일만 있는 요청을 거부한다 (`rejectMalformedEmailWhenOtherUpdateFieldsAreValid`, L29)
2. 지원서 수정 API는 비양수 질문 ID만 있는 요청을 거부한다 (`rejectNonPositiveQuestionIdWhenOtherUpdateFieldsAreValid`, L41)
3. 지원서 제출 API는 비양수 application ID를 거부한다 (`rejectNonPositiveApplicationId`, L53)
4. 지원서 draft 수정 API는 인증 actor와 answers를 command로 전달한다 (`updateDraftUsesAuthenticatedActor`, L64)
5. 지원서 제출 API는 인증 actor와 submittedIp를 command로 전달한다 (`submitUsesAuthenticatedActor`, L86)
6. 지원서 철회 API는 인증 actor를 command로 전달한다 (`cancelUsesAuthenticatedActor`, L107)

### `RecruitingApplicationRandomPortIntegrationTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingApplicationRandomPortIntegrationTest.java`

1. 실제 REST socket에서 JWT actor로 지원서 초안을 생성한다 (`실제_REST_socket_JWT_actor_지원서_생성`, L127)
2. 실제 REST socket에서 비로그인과 malformed email 요청을 거부한다 (`실제_REST_socket_비로그인과_malformed_email_거부`, L150)
3. 실제 GraphQL HTTP에서 query와 인증 mutation 결과를 관측한다 (`실제_GraphQL_HTTP_query와_인증_mutation_결과`, L171)
4. 실제 GraphQL HTTP에서 비로그인 mutation을 거부한다 (`실제_GraphQL_HTTP_비로그인_mutation_거부`, L201)
5. 실제 Security chain에서 공개 Form 목록은 익명 요청을 허용한다 (`실제_Security_chain은_공개_Form_익명_요청을_허용한다`, L218)
6. 실제 REST socket에서 익명 지원서를 생성하고 credential로 조회한다 (`실제_REST_socket_익명_지원서_생성_조회`, L233)
7. 실제 GraphQL HTTP에서 anonymous credential Query를 실행한다 (`실제_GraphQL_HTTP_anonymous_credential_Query`, L278)
8. 실제 REST socket에서 CSV 요청자를 use case 경계에 전달한다 (`실제_REST_socket_CSV_요청자_결속`, L308)
9. 실제 PostgreSQL P6Spy 로그는 INSERT와 SELECT 바인딩 값을 노출하지 않는다 (`실제_PostgreSQL_P6Spy_INSERT_SELECT_redaction`, L329)

### `RecruitingCredentialRestRateLimitInterceptorTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingCredentialRestRateLimitInterceptorTest.java`

1. 같은 IP의 credential 요청은 endpoint와 관계없이 전용 한도를 공유한다 (`blockCredentialRequestsByClientIp`, L34)
2. 지원서 생성 요청은 credential 전용 한도를 소비하지 않고 수정 요청은 소비한다 (`limitOnlyCredentialRequests`, L65)

### `RecruitingEvaluationControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingEvaluationControllerTest.java`

1. 평가 확정은 path stage와 CurrentMember actor를 command로 전달한다 (`submitUsesPathStageAndCurrentMember`, L74)
2. 기존 평가 제출 하위 경로는 제거한다 (`removeLegacySubmitPath`, L90)
3. 기존 평가 초안 PUT 경로는 제거한다 (`removeDraftPutPath`, L101)
4. 평가 조회는 stage 범위의 가시 평가만 반환한다 (`listVisibleEvaluationsByStage`, L112)
5. 평가 요청의 comment가 2000자를 초과하면 거부한다 (`rejectTooLongComment`, L135)

### `RecruitingManagementControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingManagementControllerTest.java`

1. 평가자 추가는 target memberId를 path에서 받고 actor는 CurrentMember를 사용한다 (`addEvaluatorUsesPathTargetAndCurrentMember`, L89)
2. 공통 질문의 blank content는 400으로 거부한다 (`rejectBlankRoundQuestion`, L105)
3. 면접 가능 일정 요청은 CurrentMember actor를 전달한다 (`requestScheduleUsesCurrentMember`, L116)
4. 지원자 가능 일정 제출 body의 memberId는 actor로 사용되지 않는다 (`submitAvailabilityUsesCurrentMember`, L132)
5. COMMON form policy에 track이 포함되면 400으로 거부한다 (`rejectCommonPolicyWithTrack`, L146)

### `RecruitingPublicControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingPublicControllerTest.java`

1. public forms API는 학교별 게시된 모집 폼을 반환한다 (`public_forms_API는_학교별_게시된_모집_폼을_반환한다`, L68)
2. 익명 지원서 생성 API는 지원 키를 최초 응답에서 반환한다 (`익명_지원서_생성_API는_지원_키를_최초_응답에서_반환한다`, L89)
3. 익명 지원서 조회 API는 내부 Form access key와 application key를 노출하지 않는다 (`익명_지원서_조회_API는_내부_key를_노출하지_않는다`, L112)

### `RecruitingRestContractTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingRestContractTest.java`

1. 모든 admin controller의 class path는 admin prefix를 사용한다 (`allAdminControllersUseAdminPrefix`, L37)
2. 모든 REST handler는 설명이 있는 Operation을 제공한다 (`allRestHandlersHaveDescribedOperations`, L45)
3. REST path에는 금지된 legacy credential overlap email endpoint가 없다 (`pathsExcludeDeferredAndLegacyEndpoints`, L57)
4. Task10 request response OpenAPI schema는 설명과 actor 비노출 계약을 가진다 (`task10SchemasAreDescribedAndDoNotExposeActor`, L79)

### `RecruitingRoundAdminControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingRoundAdminControllerTest.java`

1. 면접 차수 생성 요청의 전체 설정을 command로 전달한다 (`createInterviewRound`, L89)
2. 면접 없는 차수에 availability form이 있으면 400을 반환한다 (`updateNoInterviewRoundRejectsAvailabilityForm`, L109)
3. 면접 없는 차수 변경 요청은 null 면접 설정을 전달한다 (`updateNoInterviewRound`, L124)
4. 차수 상태 변경은 path의 seasonId와 roundId를 모두 전달한다 (`updateRoundStatusIncludesSeasonId`, L145)

### `RecruitingSeasonAdminControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/web/RecruitingSeasonAdminControllerTest.java`

1. 시즌 생성 요청의 트랙 쿼터를 command로 전달한다 (`createSeasonWithQuotas`, L99)
2. 시즌 쿼터 교체 요청의 seasonId와 트랙을 command로 전달한다 (`replaceSeasonQuotas`, L127)
3. 음수 시즌 쿼터 요청은 400을 반환한다 (`createSeasonRejectsNegativeQuota`, L145)
4. 시즌 목록 조회는 기수와 지부 필터를 CurrentMember와 함께 전달한다 (`searchSeasons`, L162)
5. 차수 목록 조회는 학교와 시즌 필터를 CurrentMember와 함께 전달한다 (`searchRounds`, L194)
6. 시즌 목록 조회에서 기수 ID가 없으면 400을 반환한다 (`searchSeasonsRequiresGisuId`, L226)

## GraphQL Adapter

### `RecruitingCredentialGraphQlRateLimitInterceptorTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingCredentialGraphQlRateLimitInterceptorTest.java`

1. credential이 아닌 GraphQL 요청은 전용 rate limit bucket을 사용하지 않는다 (`passNonCredentialOperation`, L55)
2. alias와 fragment로 묶은 credential 다중 호출도 실행 전에 제한한다 (`blockBatchedCredentialFieldsInFragment`, L67)

### `RecruitingDecisionGraphQlControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingDecisionGraphQlControllerTest.java`

1. 최종 합격 Mutation은 acceptedTrack과 CurrentMember를 public UseCase에 전달한다 (`최종_합격_Mutation은_acceptedTrack과_CurrentMember를_public_UseCase에_전달한다`, L94)
2. READY Mutation은 CurrentMember를 executor로 전달한다 (`READY_Mutation은_CurrentMember를_executor로_전달한다`, L118)
3. 등록 관리 권한이 없으면 READY UseCase를 호출하지 않는다 (`등록_관리_권한이_없으면_READY_UseCase를_호출하지_않는다`, L136)

### `RecruitingEvaluationGraphQlControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingEvaluationGraphQlControllerTest.java`

1. 평가 확정 Mutation은 stage와 CurrentMember를 public UseCase에 전달한다 (`평가_확정_Mutation은_stage와_CurrentMember를_public_UseCase에_전달한다`, L74)
2. 평가 Query는 제출 시각을 Instant scalar로 반환한다 (`평가_Query는_제출_시각을_Instant_scalar로_반환한다`, L98)

### `RecruitingEvaluatorQuestionGraphQlControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingEvaluatorQuestionGraphQlControllerTest.java`

1. 평가자 추가 Mutation은 대상 evaluator와 CurrentMember를 public UseCase에 전달한다 (`평가자_추가_Mutation은_대상_evaluator와_CurrentMember를_public_UseCase에_전달한다`, L91)
2. 공통 질문 생성 Mutation은 CurrentMember를 public UseCase에 전달한다 (`공통_질문_생성_Mutation은_CurrentMember를_public_UseCase에_전달한다`, L117)
3. INTERVIEW 평가자는 GraphQL에서 담당 차수의 공통 질문을 조회한다 (`interviewEvaluatorReadsRoundQuestionsThroughActorScopedUseCase`, L141)
4. INTERVIEW 평가자는 GraphQL에서 담당 지원서의 개별 질문을 조회한다 (`interviewEvaluatorReadsApplicationQuestionsThroughActorScopedUseCase`, L165)
5. GraphQL 공통 질문은 요청 season과 round scope가 다르면 거부한다 (`rejectRoundQuestionsOutsideRequestedSeason`, L196)
6. GraphQL 개별 질문은 요청 season과 application scope가 다르면 거부한다 (`rejectApplicationQuestionsOutsideRequestedSeason`, L213)

### `RecruitingFormAdminGraphQlControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingFormAdminGraphQlControllerTest.java`

1. 트랙 폼 정책 Mutation은 track list 모델의 단일 섹션 정책을 전달한다 (`트랙_폼_정책_Mutation은_track_list_모델의_단일_섹션_정책을_전달한다`, L89)
2. 지원 Form 연결 Mutation은 검증한 시즌 ID를 command로 전달한다 (`linkFormBindsValidatedSeason`, L115)

### `RecruitingGraphQlArchitectureTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlArchitectureTest.java`

1. Recruiting GraphQL adapter 패키지가 존재한다 (`Recruiting_GraphQL_adapter_패키지가_존재한다`, L22)
2. Recruiting GraphQL adapter는 REST web adapter DTO에 의존하지 않는다 (`Recruiting_GraphQL_adapter는_REST_web_adapter_DTO에_의존하지_않는다`, L29)
3. Recruiting GraphQL adapter는 domain entity를 노출하지 않는다 (`Recruiting_GraphQL_adapter는_domain_entity를_노출하지_않는다`, L39)
4. Recruiting GraphQL adapter는 outbound port DTO에 의존하지 않는다 (`Recruiting_GraphQL_adapter는_outbound_port_DTO에_의존하지_않는다`, L49)
5. Recruiting GraphQL adapter는 persistence adapter에 의존하지 않는다 (`Recruiting_GraphQL_adapter는_persistence_adapter에_의존하지_않는다`, L59)

### `RecruitingGraphQlExceptionAdviceTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlExceptionAdviceTest.java`

1. 잘못된 Instant 일정은 GraphQL 입력 오류로 거부한다 (`잘못된_Instant_일정은_GraphQL_입력_오류로_거부한다`, L74)

### `RecruitingGraphQlRandomPortIntegrationTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlRandomPortIntegrationTest.java`

1. 실제 GraphQL HTTP는 제거된 anonymous credential Query를 거부한다 (`실제_GraphQL_HTTP는_제거된_anonymous_credential_Query를_거부한다`, L80)
2. 실제 GraphQL HTTP는 JWT CurrentMember로 로그인 지원서를 조회한다 (`실제_GraphQL_HTTP는_JWT_CurrentMember로_로그인_지원서를_조회한다`, L98)
3. 실제 GraphQL HTTP는 비로그인 지원서 조회를 FORBIDDEN으로 거부한다 (`실제_GraphQL_HTTP는_비로그인_지원서_조회를_FORBIDDEN으로_거부한다`, L123)

### `RecruitingGraphQlSecurityTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlSecurityTest.java`

1. 비로그인 GraphQL 지원서 생성은 FORBIDDEN으로 거부한다 (`비로그인_GraphQL_지원서_생성은_FORBIDDEN으로_거부한다`, L101)
2. 로그인 지원서 Query는 CurrentMember의 ID만 public UseCase에 전달한다 (`로그인_지원서_Query는_CurrentMember_ID만_public_UseCase에_전달한다`, L125)
3. 익명 지원서 초안 생성 Mutation은 로그인 없이 실행된다 (`익명_지원서_초안_생성_Mutation은_로그인_없이_실행된다`, L159)
4. credential Query는 공개 결과만 반환하고 application key를 응답 계약에 두지 않는다 (`credential_Query는_공개_결과만_반환한다`, L192)

### `RecruitingGraphQlSurfaceTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingGraphQlSurfaceTest.java`

1. GraphQL introspection은 Recruiting Query와 Mutation 계약만 제공한다 (`GraphQL_introspection은_Recruiting_Query와_Mutation_계약만_제공한다`, L41)
2. GraphQL introspection은 Instant와 트랙 및 상태 nullability를 보존한다 (`GraphQL_introspection은_Instant와_트랙_및_상태_nullability를_보존한다`, L93)

### `RecruitingRoundAdminGraphQlControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingRoundAdminGraphQlControllerTest.java`

1. GraphQL 면접 차수 생성은 전체 설정을 Instant command로 전달한다 (`createInterviewRound`, L91)
2. GraphQL 면접 없는 차수 변경은 null 면접 설정을 전달한다 (`updateNoInterviewRound`, L110)
3. GraphQL 차수 생성의 잘못된 Instant는 BAD_REQUEST error를 반환한다 (`createRoundRejectsMalformedInstant`, L144)

### `RecruitingScheduleGraphQlControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingScheduleGraphQlControllerTest.java`

1. 일정 확정 Mutation은 Instant와 CurrentMember를 public UseCase에 전달한다 (`일정_확정_Mutation은_Instant와_CurrentMember를_public_UseCase에_전달한다`, L78)

### `RecruitingSeasonAdminGraphQlControllerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/graphql/RecruitingSeasonAdminGraphQlControllerTest.java`

1. GraphQL 시즌 생성은 0명을 포함한 쿼터를 command로 전달한다 (`createSeasonWithQuotas`, L102)
2. GraphQL 쿼터 교체는 seasonId와 트랙 설정을 command로 전달한다 (`replaceSeasonQuotas`, L132)
3. GraphQL 상태 요약은 CurrentMember와 요청 기수를 public UseCase에 전달한다 (`statusSummaryBindsCurrentMemberAndRequestedGisu`, L156)
4. GraphQL 시즌 목록은 현재 회원과 지부 필터를 query로 전달한다 (`searchSeasons`, L181)
5. GraphQL 차수 목록은 현재 회원과 학교 및 시즌 필터를 query로 전달한다 (`searchRounds`, L222)
6. GraphQL 시즌 목록은 양수가 아닌 기수 ID를 거부한다 (`searchSeasonsRejectsNonPositiveGisuId`, L263)

## Persistence / Migration

### `RecruitingApplicantConcurrencyTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingApplicantConcurrencyTest.java`

1. 같은 기수 지원자가 다른 학교에 동시에 지원해도 하나만 생성된다 (`onlyOneConcurrentApplicationAcrossSchoolsSucceeds`, L110)
2. 같은 기수 지원자를 다른 학교에서 동시에 최종 합격시켜도 하나만 성공한다 (`onlyOneConcurrentFinalPassAcrossSchoolsSucceeds`, L133)

### `RecruitingApplicationDatabaseInvariantTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingApplicationDatabaseInvariantTest.java`

1. 다른 도메인 데이터는 보존하고 지원서 세 unique constraint를 강제한다 (`preserveOtherDomainAndEnforceUniqueConstraints`, L18)
2. 지원서 key email name privacy choice accepted track CHECK를 강제한다 (`enforceApplicationCheckConstraints`, L47)
3. 익명 지원서는 회원 ID 없이 개인정보 증적과 Form access key를 모두 가져야 한다 (`enforceAnonymousIdentityModeConstraint`, L95)

### `RecruitingApplicationFormPolicyConcurrencyTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingApplicationFormPolicyConcurrencyTest.java`

1. PostgreSQL에서 게시가 root lock을 잡으면 동시 section policy 추가는 게시 상태를 보고 거부된다 (`publishSerializesConcurrentPolicyAddition`, L83)

### `RecruitingEvaluationQuestionConcurrencyTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingEvaluationQuestionConcurrencyTest.java`

1. 평가 확정이 round lock을 먼저 잡으면 동시 중복 평가 확정을 거부한다 (`finalizedEvaluationRejectsConcurrentDuplicate`, L111)
2. 평가 제출이 round lock을 먼저 잡으면 동시 공통 질문 수정을 거부한다 (`submittedEvaluationFreezesConcurrentRoundQuestionMutation`, L131)
3. 평가 제출이 round와 application lock을 먼저 잡으면 동시 개별 질문 수정을 거부한다 (`submittedEvaluationFreezesConcurrentApplicationQuestionMutation`, L150)

### `RecruitingEvaluationScheduleMigrationTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingEvaluationScheduleMigrationTest.java`

1. 최종 평가와 면접 일정 테이블만 생성한다 (`최종_평가와_면접_일정_테이블만_생성한다`, L29)
2. SENT 메일 상태에서 sentAt이 없는 면접 일정은 데이터베이스가 거부한다 (`SENT_메일_상태에서_sentAt이_없는_면접_일정은_데이터베이스가_거부한다`, L53)

### `RecruitingEvaluationSchedulePersistenceAdapterTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingEvaluationSchedulePersistenceAdapterTest.java`

1. 단계별 평가와 확정 면접 일정을 저장하고 application은 지연 로딩한다 (`단계별_평가와_확정_면접_일정을_저장하고_application은_지연_로딩한다`, L47)
2. 같은 지원서 평가자 단계 평가는 데이터베이스에서 중복 저장할 수 없다 (`같은_지원서_평가자_단계_평가는_데이터베이스에서_중복_저장할_수_없다`, L104)
3. 서류 평가 확정만으로는 면접 질문을 동결하지 않는다 (`서류_평가_확정만으로는_면접_질문을_동결하지_않는다`, L135)
4. decision이 없는 평가는 데이터베이스가 거부한다 (`decision이_없는_평가는_데이터베이스가_거부한다`, L160)
5. 지원서별 면접 일정은 하나만 저장할 수 있다 (`지원서별_면접_일정은_하나만_저장할_수_있다`, L186)
6. 확정 면접의 잘못된 시간 순서는 데이터베이스가 거부한다 (`확정_면접의_잘못된_시간_순서는_데이터베이스가_거부한다`, L211)

### `RecruitingEvaluatorQuestionPersistenceAdapterTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingEvaluatorQuestionPersistenceAdapterTest.java`

1. 차수 평가자는 서류와 면접 전형의 공통 whitelist로 조회된다 (`evaluatorIsScopedByRound`, L66)
2. 같은 차수의 같은 회원은 데이터베이스에서 중복 평가자로 저장할 수 없다 (`evaluatorIsUniqueByRoundAndMember`, L82)
3. 공통 질문은 순서대로 조회하고 비활성 질문을 활성 목록에서 제외한다 (`listActiveRoundQuestionsInOrder`, L95)
4. 공통 질문의 생성자와 최종 변경자를 데이터베이스에 보존한다 (`persistRoundQuestionCreatorAndLastModifier`, L119)
5. 공통 질문 내용 데이터베이스 제약은 공백을 거부한다 (`roundQuestionContentCheckRejectsBlank`, L137)
6. 개별 질문은 지원서를 지연 로딩으로 참조하고 활성 질문만 순서대로 조회한다 (`applicationQuestionUsesLazyApplicationReference`, L154)
7. 개별 질문 순서 데이터베이스 제약은 음수를 거부한다 (`applicationQuestionOrderCheckRejectsNegativeValue`, L173)
8. 제출된 신규 면접 평가는 질문 동결 port에서 차수와 지원서 단위로 조회된다 (`submittedEvaluationLocksRoundAndApplicationQuestions`, L189)

### `RecruitingFormApplicationMigrationTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingFormApplicationMigrationTest.java`

1. 빈 schema에 최종 지원서 컬럼을 구성한다 (`createFinalRecruitingTables`, L21)
2. Form은 Round당 하나이고 section policy type과 track 조합을 강제한다 (`enforceFormAndSectionPolicyConstraints`, L52)

### `RecruitingFormSectionPolicyPersistenceAdapterTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingFormSectionPolicyPersistenceAdapterTest.java`

1. 지원 Form의 COMMON과 TRACK 섹션 정책을 저장하고 조회한다 (`saveAndListPolicies`, L45)
2. 같은 Form section ID에는 하나의 정책만 저장할 수 있다 (`rejectDuplicateFormSectionPolicy`, L57)

### `RecruitingLockExceptionTranslatorTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingLockExceptionTranslatorTest.java`

1. PostgreSQL lock timeout과 deadlock은 재시도 가능한 Recruiting conflict로 변환한다 (`translatePostgreSqlConcurrencyFailures`, L19)

### `RecruitingPersistenceAdapterTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingPersistenceAdapterTest.java`

1. 모집_시즌_차수_폼_지원서를_저장하고_embedded_email_경로로_조회한다 (`saveAndLoadRecruitingCoreGraphWithDetails`, L40)
2. 익명 지원서는 email과 application key로 조회하고 Form raw key를 내부에 보존한다 (`saveAndLoadAnonymousApplicationByCredential`, L92)
3. 지원서 mutation lock은 application root만 잠근 뒤 상세 연관을 별도 조회한다 (`mutationLockTargetsApplicationRootBeforeLoadingDetails`, L126)
4. 지원서_중복_및_재지원_차단_조회가_상태와_학교를_구분한다 (`duplicateAndReapplicationQueriesRespectStatusAndSchool`, L172)
5. 쿼터 사용량은 Season 전체 차수의 READY와 REGISTERED를 합산한다 (`countQuotaUsageAcrossAllRoundsInSeason`, L221)
6. 지원서 요약 행은 CSV 마스킹용 이메일과 상태 필터로 조회된다 (`searchSummaryRowsReturnsMinimalStatusRows`, L250)

### `RecruitingQuotaReservationConcurrencyTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingQuotaReservationConcurrencyTest.java`

1. PostgreSQL에서 quota 1자리 병렬 READY 요청은 하나만 성공한다 (`onlyOneConcurrentReadyWinsLastSeat`, L78)

### `RecruitingRegistrationDatabaseInvariantTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingRegistrationDatabaseInvariantTest.java`

1. FINAL_PASSED는 acceptedTrack이 필수이고 READY는 최종 합격에만 허용한다 (`enforceAcceptedTrackAndRegistrationLifecycle`, L17)

### `RecruitingRoundScheduleMigrationTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingRoundScheduleMigrationTest.java`

1. 데이터베이스는 순서가 역전된 모집 차수 일정을 거부한다 (`databaseRejectsMalformedRoundSchedule`, L34)
2. 데이터베이스는 null 필수 모집 일정을 거부한다 (`databaseRejectsMissingRequiredRoundSchedule`, L44)
3. 데이터베이스는 접수 종료보다 빠른 서류 결과 발표를 거부한다 (`databaseRejectsDocumentResultBeforeDocumentEnd`, L54)
4. 데이터베이스는 서류 결과 발표보다 빠른 면접 시작을 거부한다 (`databaseRejectsInterviewBeforeDocumentResult`, L64)
5. 데이터베이스는 면접 종료보다 빠른 최종 결과 발표를 거부한다 (`databaseRejectsFinalResultBeforeInterviewEnd`, L74)
6. 데이터베이스는 면접이 없을 때 서류 결과보다 빠른 최종 발표를 거부한다 (`databaseRejectsFinalResultBeforeDocumentResultWithoutInterview`, L84)
7. 데이터베이스는 면접 없는 차수의 availability form을 거부한다 (`databaseRejectsAvailabilityFormWithoutInterview`, L94)
8. 데이터베이스는 필수 면접 기간이 없는 차수를 거부한다 (`databaseRejectsMissingRequiredInterviewWindow`, L104)

### `RecruitingSeasonRoundMigrationTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingSeasonRoundMigrationTest.java`

1. 데이터베이스는 음수 시즌 쿼터를 거부한다 (`databaseRejectsNegativeQuota`, L27)
2. 데이터베이스는 중복 모집 트랙 배열을 거부한다 (`databaseRejectsDuplicateRecruitableTracks`, L42)
3. 데이터베이스는 null 모집 트랙을 거부한다 (`databaseRejectsNullRecruitableTrack`, L51)
4. 데이터베이스는 INFRA_PLUS 모집 트랙을 거부한다 (`databaseRejectsInfraPlusRecruitableTrack`, L60)

### `RecruitingSeasonRoundPersistenceAdapterTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/persistence/RecruitingSeasonRoundPersistenceAdapterTest.java`

1. 시즌 쿼터와 면접 없는 차수 설정을 저장하고 조회한다 (`saveAndLoadConfiguration`, L42)
2. 같은 시즌과 트랙의 쿼터는 중복 저장할 수 없다 (`quotaTrackIsUniqueWithinSeason`, L70)
3. 같은 트랙의 쿼터를 새 목표 인원으로 교체한다 (`replaceQuotaForSameTrack`, L82)
4. 여러 시즌의 차수를 한 번에 조회하면 시즌도 함께 로딩한다 (`listRoundsBySeasonIds`, L107)

## Outbound Adapter

### `RecruitingApplicationKeyGeneratorTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/out/id/RecruitingApplicationKeyGeneratorTest.java`

1. SecureRandom 지원 키는 대문자와 숫자로 된 6자리다 (`generateSixUppercaseAlphanumericCharacters`, L19)
2. 지원 키 생성기는 SecureRandom에서 정확히 6개 문자를 선택한다 (`generateFromSecureRandom`, L28)

## Other

### `InterviewAvailabilityRequestedEventListenerTest`

소스: `src/test/java/com/umc/product/recruiting/adapter/in/event/InterviewAvailabilityRequestedEventListenerTest.java`

1. Outbox 이벤트는 Thymeleaf HTML 메일을 보내고 성공 상태를 기록한다 (`이벤트는_HTML_메일을_보내고_성공_상태를_기록한다`, L55)
2. 메일 실패는 민감정보를 제거한 오류를 기록하고 Outbox 재시도를 위해 예외를 전달한다 (`메일_실패는_오류를_기록하고_예외를_전달한다`, L74)
3. 이미 발송된 요청 이벤트는 멱등하게 무시한다 (`이미_발송된_이벤트는_무시한다`, L89)

### `RecruitingPermissionEvaluatorTest`

소스: `src/test/java/com/umc/product/recruiting/application/service/evaluator/RecruitingPermissionEvaluatorTest.java`

1. supportedResourceType은 RECRUITMENT를 반환한다 (`supportedResourceType은_RECRUITMENT를_반환한다`, L52)
2. 학교 회장단은 자기 학교 모집 WRITE 권한을 통과한다 (`학교_회장단은_자기_학교_모집_WRITE_권한을_통과한다`, L58)
3. 학교 회장단은 다른 학교 모집 WRITE 권한을 거부한다 (`학교_회장단은_다른_학교_모집_WRITE_권한을_거부한다`, L67)
4. 중앙운영사무국 총괄단은 모든 학교 모집 APPROVE 권한을 통과한다 (`중앙운영사무국_총괄단은_모든_학교_모집_APPROVE_권한을_통과한다`, L76)
5. 중앙운영사무국 총괄단은 모든 학교 모집 MANAGE 권한을 통과한다 (`중앙운영사무국_총괄단은_모든_학교_모집_MANAGE_권한을_통과한다`, L85)
6. SUPER_ADMIN은 기수와 무관하게 특정 모집 MANAGE 권한을 통과한다 (`SUPER_ADMIN은_기수와_무관하게_특정_모집_MANAGE_권한을_통과한다`, L94)
7. SUPER_ADMIN은 모집 전체 MANAGE 권한을 통과한다 (`SUPER_ADMIN은_모집_전체_MANAGE_권한을_통과한다`, L103)
8. 학교 회장단은 자기 학교라도 MANAGE 권한을 거부한다 (`학교_회장단은_자기_학교라도_MANAGE_권한을_거부한다`, L114)
9. 교내 파트장은 모집 WRITE 권한을 거부한다 (`교내_파트장은_모집_WRITE_권한을_거부한다`, L123)
10. DELETE 권한은 evaluator에서 구현하지 않아 예외가 발생한다 (`DELETE_권한은_evaluator에서_구현하지_않아_예외가_발생한다`, L132)
