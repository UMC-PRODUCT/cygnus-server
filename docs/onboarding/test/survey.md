# Survey 테스트 케이스

- 테스트 파일: 3개
- 테스트 케이스: 13개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| UseCase / Application Service | 13 |

## UseCase / Application Service

### FormCommandServiceTest
- 위치: `src/test/java/com/umc/product/survey/application/service/command/FormCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [46](../../../src/test/java/com/umc/product/survey/application/service/command/FormCommandServiceTest.java#L46) | createDraft는 요청 description을 신규 폼에 저장한다 | CreateDraftFormCommand {createdMemberId=10L, title="지원서", description="지원 폼 설명", allowDuplicateResponses=true}; 호출 createDraft(CreateDraftFormCommand.builder() | 성공: 검증 assertThat(result).isEqualTo(1L); assertThat(captor.getValue().getDescription()).isEqualTo("지원 폼 설명"); |

### FormResponseCommandServiceTest
- 위치: `src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [71](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L71) | 기본 폼은 같은 form/member의 draft 생성을 차단한다 | CreateDraftFormResponseCommand {formId=FORM_ID, respondentMemberId=MEMBER_ID}; 호출 createDraft(CreateDraftFormResponseCommand.builder() | 실패: 예외 SurveyDomainException; 에러코드 SurveyErrorCode.FORM_RESPONSE_ALREADY_EXISTS; 검증 .isEqualTo(SurveyErrorCode.FORM_RESPONSE_ALREADY_EXISTS); |
| [88](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L88) | 기본 폼은 같은 form/member의 즉시 제출을 차단한다 | SubmitFormResponseCommand {formId=FORM_ID, respondentMemberId=MEMBER_ID, answers=List.of(}; 호출 submitImmediately(SubmitFormResponseCommand.builder() | 실패: 예외 SurveyDomainException; 에러코드 SurveyErrorCode.FORM_RESPONSE_ALREADY_EXISTS; 검증 .isEqualTo(SurveyErrorCode.FORM_RESPONSE_ALREADY_EXISTS); |
| [106](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L106) | 중복 허용 폼은 같은 form/member의 두 번째 draft도 새 응답으로 생성한다 | CreateDraftFormResponseCommand {formId=FORM_ID, respondentMemberId=MEMBER_ID}; 호출 createDraft(CreateDraftFormResponseCommand.builder() | 실패: 검증 assertThat(result).isEqualTo(FORM_RESPONSE_ID); |
| [125](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L125) | 중복 허용 폼은 같은 form/member의 즉시 제출도 새 응답으로 생성한다 | SubmitFormResponseCommand {formId=FORM_ID, respondentMemberId=MEMBER_ID, answers=List.of(}; 호출 submitImmediately(SubmitFormResponseCommand.builder() | 실패: 검증 assertThat(result).isEqualTo(FORM_RESPONSE_ID); |
| [146](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L146) | 중복 허용 폼은 formId/memberId 기반 제출 응답 수정을 막는다 | UpdateFormResponseCommand {formId=FORM_ID, respondentMemberId=MEMBER_ID, answers=List.of(}; 호출 updateResponse(UpdateFormResponseCommand.builder() | 실패: 예외 SurveyDomainException; 에러코드 SurveyErrorCode.FORM_RESPONSE_LOOKUP_AMBIGUOUS; 검증 .isEqualTo(SurveyErrorCode.FORM_RESPONSE_LOOKUP_AMBIGUOUS); |
| [164](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L164) | 중복 허용 폼은 formId/memberId 기반 제출 응답 삭제를 막는다 | DeleteFormResponseCommand {formId=FORM_ID, respondentMemberId=MEMBER_ID}; 호출 deleteResponse(DeleteFormResponseCommand.builder() | 실패: 예외 SurveyDomainException; 에러코드 SurveyErrorCode.FORM_RESPONSE_LOOKUP_AMBIGUOUS; 검증 .isEqualTo(SurveyErrorCode.FORM_RESPONSE_LOOKUP_AMBIGUOUS); |
| [182](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L182) | draft 제출 scope가 있으면 전달된 required question만 필수 응답 검증한다 | SubmitDraftFormResponseCommand {formResponseId=FORM_RESPONSE_ID, requesterMemberId=MEMBER_ID, requiredQuestionIds=Set.of(commonRequiredQuestion.getId(, allowedQuestionIds=Set.of(commonRequiredQuestion.getId(}; 호출 submitDraft(SubmitDraftFormResponseCommand.builder() | 성공: draft 제출 scope가 있으면 전달된 required question만 필수 응답 검증한다 |
| [202](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L202) | draft 제출 scope가 없으면 기존처럼 form 전체 required question을 검증한다 | SubmitDraftFormResponseCommand {formResponseId=FORM_RESPONSE_ID, requesterMemberId=MEMBER_ID}; 호출 submitDraft(SubmitDraftFormResponseCommand.builder() | 실패: 예외 SurveyDomainException; 에러코드 SurveyErrorCode.REQUIRED_QUESTION_NOT_ANSWERED; 검증 .isEqualTo(SurveyErrorCode.REQUIRED_QUESTION_NOT_ANSWERED); |
| [223](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L223) | draft 제출 scope의 allowed question 밖에 저장된 답변이 있으면 실패한다 | SubmitDraftFormResponseCommand {formResponseId=FORM_RESPONSE_ID, requesterMemberId=MEMBER_ID, requiredQuestionIds=Set.of(, allowedQuestionIds=Set.of(10L}; 호출 submitDraft(SubmitDraftFormResponseCommand.builder() | 실패: 예외 SurveyDomainException; 에러코드 SurveyErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM; 검증 .isEqualTo(SurveyErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM); |
| [245](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L245) | SHORT_TEXT 답변이 빈 문자열이면 저장하지 않고 INVALID_ANSWER_FORMAT으로 거부한다 | UpdateDraftFormResponseCommand {formResponseId=FORM_RESPONSE_ID, requesterMemberId=MEMBER_ID, answers=List.of(AnswerCommand.builder(, questionId=question.getId(, textValue=""}; 호출 updateDraft(UpdateDraftFormResponseCommand.builder() | 실패: 예외 SurveyDomainException; 에러코드 SurveyErrorCode.INVALID_ANSWER_FORMAT; 검증 .isEqualTo(SurveyErrorCode.INVALID_ANSWER_FORMAT); |
| [268](../../../src/test/java/com/umc/product/survey/application/service/command/FormResponseCommandServiceTest.java#L268) | LONG_TEXT 답변이 공백 문자열이면 저장하지 않고 INVALID_ANSWER_FORMAT으로 거부한다 | UpdateDraftFormResponseCommand {formResponseId=FORM_RESPONSE_ID, requesterMemberId=MEMBER_ID, answers=List.of(AnswerCommand.builder(, questionId=question.getId(, textValue=" "}; 호출 updateDraft(UpdateDraftFormResponseCommand.builder() | 실패: 예외 SurveyDomainException; 에러코드 SurveyErrorCode.INVALID_ANSWER_FORMAT; 검증 .isEqualTo(SurveyErrorCode.INVALID_ANSWER_FORMAT); |

### QuestionCommandServiceTest
- 위치: `src/test/java/com/umc/product/survey/application/service/command/QuestionCommandServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [46](../../../src/test/java/com/umc/product/survey/application/service/command/QuestionCommandServiceTest.java#L46) | createQuestion은 요청 description을 신규 질문에 저장한다 | CreateQuestionCommand {sectionId=20L, requesterMemberId=99L, type=QuestionType.SHORT_TEXT, title="자기소개", description="자기소개를 입력해주세요", isRequired=true}; 호출 createQuestion(CreateQuestionCommand.builder() | 성공: 검증 assertThat(result).isEqualTo(30L); assertThat(captor.getValue().getDescription()).isEqualTo("자기소개를 입력해주세요"); |
