package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.AnonymousFormResponseResult;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitAnonymousDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousFormResponseCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CancelAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateAnonymousRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateAnonymousRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationQuestionScopeInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationPort;
import com.umc.product.recruiting.domain.RecruitingApplicantEmail;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.term.application.port.in.query.GetTermUseCase;
import com.umc.product.term.application.port.in.query.dto.TermInfo;
import com.umc.product.term.domain.enums.TermType;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationCommandServiceTest {

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;

    @Mock
    SaveRecruitingApplicationPort saveApplicationPort;

    @Mock
    ManageFormResponseUseCase manageFormResponseUseCase;

    @Mock
    GetRecruitingApplicationQuestionScopeUseCase getQuestionScopeUseCase;

    @Mock
    RecruitingApplicationValidationService validationService;

    @Mock
    RecruitingApplicationKeyIssuer applicationKeyIssuer;

    @Mock
    RecruitingConcurrencyLockService concurrencyLockService;

    @Mock
    GetTermUseCase getTermUseCase;

    @Spy
    Clock clock = Clock.fixed(Instant.parse("2026-08-05T00:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    RecruitingApplicationCommandService sut;

    @Test
    @DisplayName("로그인 회원 지원서 생성은 정규화 이메일과 생성 전용 지원 키를 반환한다")
    void createMemberDraft() {
        RecruitingApplicationForm form = publishedForm();
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(applicationKeyIssuer.issue("applicant@example.com")).willReturn("A1B2C3");
        given(manageFormResponseUseCase.createDraft(any())).willReturn(700L);
        given(saveApplicationPort.save(any())).willAnswer(invocation -> {
            RecruitingApplication application = invocation.getArgument(0);
            ReflectionTestUtils.setField(application, "id", 900L);
            return application;
        });

        RecruitingApplicationCreatedInfo result = sut.createDraft(CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(100L)
            .applicantMemberId(200L)
            .applicantName("홍길동")
            .applicantEmail(" Applicant@Example.COM ")
            .firstChoice(ChallengerTrack.PLAN)
            .secondChoice(ChallengerTrack.DESIGN)
            .build());

        assertThat(result.applicationId()).isEqualTo(900L);
        assertThat(result.applicationKey()).isEqualTo("A1B2C3");
        ArgumentCaptor<CreateDraftFormResponseCommand> captor =
            ArgumentCaptor.forClass(CreateDraftFormResponseCommand.class);
        then(manageFormResponseUseCase).should().createDraft(captor.capture());
        assertThat(captor.getValue().respondentMemberId()).isEqualTo(200L);
        then(validationService).should().validateNew(form.getRound(), 200L, "applicant@example.com");
    }

    @Test
    @DisplayName("로그인 회원 ID가 없으면 Form 응답을 만들기 전에 거부한다")
    void rejectDraftWithoutMember() {
        assertThatThrownBy(() -> sut.createDraft(CreateRecruitingApplicationDraftCommand.builder()
            .applicationFormId(100L)
            .applicantName("홍길동")
            .applicantEmail("applicant@example.com")
            .firstChoice(ChallengerTrack.PLAN)
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_MEMBER_REQUIRED);
        then(manageFormResponseUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("익명 지원서 생성은 개인정보 동의를 검증하고 Form raw key를 내부에만 보관한다")
    void createAnonymousDraft() {
        RecruitingApplicationForm form = publishedForm();
        given(loadApplicationFormPort.getById(100L)).willReturn(form);
        given(getTermUseCase.getTermsByType(TermType.PRIVACY))
            .willReturn(new TermInfo(77L, "https://example.com/privacy", true, TermType.PRIVACY));
        given(applicationKeyIssuer.issue("applicant@example.com")).willReturn("A1B2C3");
        given(manageFormResponseUseCase.createAnonymousDraft(any()))
            .willReturn(AnonymousFormResponseResult.builder()
                .formResponseId(700L)
                .responseAccessKey("raw-form-key")
                .build());
        given(saveApplicationPort.save(any())).willAnswer(invocation -> {
            RecruitingApplication application = invocation.getArgument(0);
            ReflectionTestUtils.setField(application, "id", 900L);
            return application;
        });

        RecruitingApplicationCreatedInfo result = sut.createAnonymousDraft(
            CreateAnonymousRecruitingApplicationDraftCommand.builder()
                .applicationFormId(100L)
                .applicantName("홍길동")
                .applicantEmail(" Applicant@Example.COM ")
                .firstChoice(ChallengerTrack.PLAN)
                .secondChoice(ChallengerTrack.DESIGN)
                .privacyTermId(77L)
                .privacyAgreed(true)
                .build()
        );

        assertThat(result.applicationKey()).isEqualTo("A1B2C3");
        ArgumentCaptor<RecruitingApplication> applicationCaptor = ArgumentCaptor.forClass(RecruitingApplication.class);
        then(saveApplicationPort).should().save(applicationCaptor.capture());
        assertThat(applicationCaptor.getValue().getFormResponseAccessKey()).isEqualTo("raw-form-key");
        assertThat(applicationCaptor.getValue().getApplicantMemberId()).isNull();
        assertThat(applicationCaptor.getValue().getPrivacyTermId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("익명 지원서 생성은 활성 개인정보 약관과 요청 약관이 다르면 Form 응답 생성 전에 거부한다")
    void rejectAnonymousDraftWithInactivePrivacyTerm() {
        given(loadApplicationFormPort.getById(100L)).willReturn(publishedForm());
        given(getTermUseCase.getTermsByType(TermType.PRIVACY))
            .willReturn(new TermInfo(88L, "https://example.com/privacy", true, TermType.PRIVACY));

        assertThatThrownBy(() -> sut.createAnonymousDraft(
            CreateAnonymousRecruitingApplicationDraftCommand.builder()
                .applicationFormId(100L)
                .applicantName("홍길동")
                .applicantEmail("applicant@example.com")
                .firstChoice(ChallengerTrack.PLAN)
                .privacyTermId(77L)
                .privacyAgreed(true)
                .build()
        ))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_PRIVACY_CONSENT);

        then(manageFormResponseUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("로그인 회원 지원서 수정은 기본 정보와 Form 응답을 함께 갱신한다")
    void updateMemberDraft() {
        RecruitingApplication application = draftApplication();
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of("new@example.com")))
            .willReturn(application);

        RecruitingApplicationInfo result = sut.updateDraft(UpdateRecruitingApplicationDraftCommand.builder()
            .applicationId(900L)
            .requesterMemberId(200L)
            .applicantName("김지원")
            .applicantEmail("new@example.com")
            .firstChoice(ChallengerTrack.DESIGN)
            .secondChoice(ChallengerTrack.PLAN)
            .answers(List.of())
            .build());

        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.DRAFT);
        assertThat(application.getApplicantName()).isEqualTo("김지원");
        assertThat(application.getApplicantEmail()).isEqualTo("new@example.com");
        then(validationService).should().validateFormResponseOwnership(application, 200L);
        then(manageFormResponseUseCase).should().updateDraft(any());
        then(saveApplicationPort).should().save(application);
    }

    @Test
    @DisplayName("다른 회원의 지원서 수정은 중복 지원 조회 전에 거부한다")
    void rejectUpdateByDifferentMemberBeforeValidation() {
        RecruitingApplication application = draftApplication();
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of("new@example.com")))
            .willReturn(application);

        assertThatThrownBy(() -> sut.updateDraft(UpdateRecruitingApplicationDraftCommand.builder()
            .applicationId(900L)
            .requesterMemberId(201L)
            .applicantName("김지원")
            .applicantEmail("new@example.com")
            .firstChoice(ChallengerTrack.DESIGN)
            .secondChoice(ChallengerTrack.PLAN)
            .answers(List.of())
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_APPLICANT_MISMATCH);
        then(validationService).shouldHaveNoInteractions();
        then(manageFormResponseUseCase).shouldHaveNoInteractions();
        then(saveApplicationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서 제출은 선택 섹션의 allowed와 required 문항 ID를 Form 응답에 전달한다")
    void submitWithSelectedQuestionScope() {
        RecruitingApplication application = draftApplication();
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);
        given(getQuestionScopeUseCase.getQuestionScope(
            100L,
            ChallengerTrack.PLAN,
            ChallengerTrack.DESIGN
        )).willReturn(new RecruitingApplicationQuestionScopeInfo(Set.of(1L, 2L), Set.of(1L)));

        RecruitingApplicationInfo result = sut.submit(SubmitRecruitingApplicationCommand.builder()
            .applicationId(900L)
            .requesterMemberId(200L)
            .submittedIp("127.0.0.1")
            .build());

        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);
        ArgumentCaptor<SubmitDraftFormResponseCommand> captor =
            ArgumentCaptor.forClass(SubmitDraftFormResponseCommand.class);
        then(manageFormResponseUseCase).should().submitDraft(captor.capture());
        assertThat(captor.getValue().allowedQuestionIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(captor.getValue().requiredQuestionIds()).containsExactly(1L);
        then(validationService).should().validateFormResponseOwnership(application, 200L);
        then(validationService).should().validateUpdate(
            application.getRound(),
            200L,
            "applicant@example.com",
            900L
        );
    }

    @Test
    @DisplayName("제출 완료 익명 지원서 수정은 scoped Form 수정 API를 사용하고 제출 상태를 유지한다")
    void updateSubmittedAnonymousApplication() {
        RecruitingApplication application = anonymousDraftApplication();
        application.submitAnonymous("applicant@example.com");
        given(loadApplicationPort.findByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(java.util.Optional.of(application));
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of("new@example.com")))
            .willReturn(application);
        given(getQuestionScopeUseCase.getQuestionScope(100L, ChallengerTrack.PLAN, ChallengerTrack.DESIGN))
            .willReturn(new RecruitingApplicationQuestionScopeInfo(Set.of(1L, 2L), Set.of(1L)));

        RecruitingApplicationInfo result = sut.updateAnonymous(UpdateAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail("applicant@example.com")
            .applicationKey("A1B2C3")
            .applicantName("김지원")
            .applicantEmail("new@example.com")
            .firstChoice(ChallengerTrack.PLAN)
            .secondChoice(ChallengerTrack.DESIGN)
            .answers(List.of(UpdateRecruitingApplicationDraftCommand.AnswerEntry.builder()
                .questionId(1L)
                .textValue("수정 답변")
                .build()))
            .build());

        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);
        ArgumentCaptor<UpdateAnonymousFormResponseCommand> captor =
            ArgumentCaptor.forClass(UpdateAnonymousFormResponseCommand.class);
        then(manageFormResponseUseCase).should().updateAnonymousResponse(captor.capture());
        assertThat(captor.getValue().responseAccessKey()).isEqualTo("raw-form-key");
        assertThat(captor.getValue().allowedQuestionIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(captor.getValue().requiredQuestionIds()).containsExactly(1L);
    }

    @Test
    @DisplayName("작성 중 익명 지원서 수정은 access key 기반 draft 수정 API를 사용한다")
    void updateAnonymousDraftApplication() {
        RecruitingApplication application = anonymousDraftApplication();
        given(loadApplicationPort.findByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(java.util.Optional.of(application));
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of("new@example.com")))
            .willReturn(application);
        given(getQuestionScopeUseCase.getQuestionScope(100L, ChallengerTrack.PLAN, ChallengerTrack.DESIGN))
            .willReturn(new RecruitingApplicationQuestionScopeInfo(Set.of(1L), Set.of(1L)));

        RecruitingApplicationInfo result = sut.updateAnonymous(UpdateAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail("applicant@example.com")
            .applicationKey("A1B2C3")
            .applicantName("김지원")
            .applicantEmail("new@example.com")
            .firstChoice(ChallengerTrack.PLAN)
            .secondChoice(ChallengerTrack.DESIGN)
            .answers(List.of())
            .build());

        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.DRAFT);
        then(manageFormResponseUseCase).should().updateAnonymousDraft(any());
        then(manageFormResponseUseCase).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("제출 완료 로그인 지원서 수정은 필수·허용 문항 범위를 포함한 제출 응답 수정 API를 사용한다")
    void updateSubmittedMemberApplication() {
        RecruitingApplication application = draftApplication();
        application.submit(200L);
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of("new@example.com")))
            .willReturn(application);
        given(getQuestionScopeUseCase.getQuestionScope(100L, ChallengerTrack.DESIGN, ChallengerTrack.PLAN))
            .willReturn(new RecruitingApplicationQuestionScopeInfo(Set.of(1L, 2L), Set.of(1L)));

        RecruitingApplicationInfo result = sut.updateDraft(UpdateRecruitingApplicationDraftCommand.builder()
            .applicationId(900L)
            .requesterMemberId(200L)
            .applicantName("김지원")
            .applicantEmail("new@example.com")
            .firstChoice(ChallengerTrack.DESIGN)
            .secondChoice(ChallengerTrack.PLAN)
            .answers(List.of())
            .build());

        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);
        then(manageFormResponseUseCase).should().updateResponse(any());
        then(manageFormResponseUseCase).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("철회된 지원서는 수정 가능한 상태가 아니므로 Form 검증 전에 거부한다")
    void rejectUpdateOfCancelledApplication() {
        RecruitingApplication application = draftApplication();
        application.cancel(200L, "철회");
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of("new@example.com")))
            .willReturn(application);

        assertThatThrownBy(() -> sut.updateDraft(UpdateRecruitingApplicationDraftCommand.builder()
            .applicationId(900L)
            .requesterMemberId(200L)
            .applicantName("김지원")
            .applicantEmail("new@example.com")
            .firstChoice(ChallengerTrack.PLAN)
            .answers(List.of())
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);

        then(validationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("익명 지원 키 형식이 잘못되면 저장소 조회 없이 거부한다")
    void rejectMalformedAnonymousApplicationKey() {
        assertThatThrownBy(() -> sut.cancelAnonymous(CancelAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail("applicant@example.com")
            .applicationKey("bad-key")
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_KEY);

        then(loadApplicationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("익명 지원서 수정은 선택한 트랙 scope 밖 질문을 Form 호출 전에 거부한다")
    void rejectAnonymousAnswerOutsideSelectedTrackScope() {
        RecruitingApplication application = anonymousDraftApplication();
        given(loadApplicationPort.findByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(java.util.Optional.of(application));
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of("applicant@example.com")))
            .willReturn(application);
        given(getQuestionScopeUseCase.getQuestionScope(100L, ChallengerTrack.PLAN, ChallengerTrack.DESIGN))
            .willReturn(new RecruitingApplicationQuestionScopeInfo(Set.of(1L, 2L), Set.of(1L)));

        assertThatThrownBy(() -> sut.updateAnonymous(UpdateAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail("applicant@example.com")
            .applicationKey("A1B2C3")
            .applicantName("홍길동")
            .applicantEmail("applicant@example.com")
            .firstChoice(ChallengerTrack.PLAN)
            .secondChoice(ChallengerTrack.DESIGN)
            .answers(List.of(UpdateRecruitingApplicationDraftCommand.AnswerEntry.builder()
                .questionId(3L)
                .textValue("다른 트랙 답변")
                .build()))
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_ANSWER_OUT_OF_SCOPE);

        then(manageFormResponseUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("익명 지원서 제출은 내부 Form access key와 선택 문항 scope를 전달한다")
    void submitAnonymousApplication() {
        RecruitingApplication application = anonymousDraftApplication();
        given(loadApplicationPort.findByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(java.util.Optional.of(application));
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);
        given(getQuestionScopeUseCase.getQuestionScope(100L, ChallengerTrack.PLAN, ChallengerTrack.DESIGN))
            .willReturn(new RecruitingApplicationQuestionScopeInfo(Set.of(1L, 2L), Set.of(1L)));

        RecruitingApplicationInfo result = sut.submitAnonymous(SubmitAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail("applicant@example.com")
            .applicationKey("A1B2C3")
            .submittedIp("127.0.0.1")
            .build());

        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.SUBMITTED);
        ArgumentCaptor<SubmitAnonymousDraftFormResponseCommand> captor =
            ArgumentCaptor.forClass(SubmitAnonymousDraftFormResponseCommand.class);
        then(manageFormResponseUseCase).should().submitAnonymousDraft(captor.capture());
        assertThat(captor.getValue().responseAccessKey()).isEqualTo("raw-form-key");
        assertThat(captor.getValue().allowedQuestionIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(captor.getValue().submittedIp()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("이미 제출한 익명 지원서는 중복 제출할 수 없다")
    void rejectAlreadySubmittedAnonymousApplication() {
        RecruitingApplication application = anonymousDraftApplication();
        application.submitAnonymous("applicant@example.com");
        given(loadApplicationPort.findByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(java.util.Optional.of(application));
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);

        assertThatThrownBy(() -> sut.submitAnonymous(SubmitAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail("applicant@example.com")
            .applicationKey("A1B2C3")
            .submittedIp("127.0.0.1")
            .build()))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_INVALID_TRANSITION);

        then(manageFormResponseUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("지원서 철회는 Form 응답을 삭제하지 않고 Recruiting 상태만 변경한다")
    void cancelDoesNotDeleteFormResponse() {
        RecruitingApplication application = draftApplication();
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);

        RecruitingApplicationInfo result = sut.cancel(CancelRecruitingApplicationCommand.builder()
            .applicationId(900L)
            .requesterMemberId(200L)
            .reason("지원 취소")
            .build());

        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.CANCELLED);
        then(manageFormResponseUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("익명 지원서 철회는 정규화된 이메일과 지원 키를 검증하고 Form 응답을 보존한다")
    void cancelAnonymousApplication() {
        RecruitingApplication application = anonymousDraftApplication();
        given(loadApplicationPort.findByApplicantEmailAndApplicationKey("applicant@example.com", "A1B2C3"))
            .willReturn(java.util.Optional.of(application));
        given(concurrencyLockService.lockApplicantThenApplication(900L, List.of())).willReturn(application);

        RecruitingApplicationInfo result = sut.cancelAnonymous(CancelAnonymousRecruitingApplicationCommand.builder()
            .credentialEmail(" Applicant@Example.COM ")
            .applicationKey("A1B2C3")
            .build());

        assertThat(result.status()).isEqualTo(RecruitingApplicationStatus.CANCELLED);
        then(saveApplicationPort).should().save(application);
        then(manageFormResponseUseCase).shouldHaveNoInteractions();
    }

    private RecruitingApplication draftApplication() {
        RecruitingApplicationForm form = publishedForm();
        RecruitingApplication application = RecruitingApplication.createMemberDraft(
            form,
            700L,
            200L,
            RecruitingApplicantProfile.create(
                form.getRound(),
                "홍길동",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.PLAN,
                ChallengerTrack.DESIGN
            ),
            "A1B2C3"
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        return application;
    }

    private RecruitingApplication anonymousDraftApplication() {
        RecruitingApplicationForm form = publishedForm();
        RecruitingApplication application = RecruitingApplication.createAnonymousDraft(
            form,
            700L,
            "raw-form-key",
            RecruitingApplicantProfile.create(
                form.getRound(),
                "홍길동",
                RecruitingApplicantEmail.from("applicant@example.com"),
                ChallengerTrack.PLAN,
                ChallengerTrack.DESIGN
            ),
            "A1B2C3",
            77L,
            Instant.parse("2026-07-15T00:00:00Z")
        );
        ReflectionTestUtils.setField(application, "id", 900L);
        return application;
    }

    private RecruitingApplicationForm publishedForm() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(configuredRound(), 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        form.publish(form.getRound().getRecruitableTracks());
        return form;
    }

    private RecruitingRound configuredRound() {
        RecruitingSeason season = RecruitingSeason.create(1L, 10L);
        ReflectionTestUtils.setField(season, "id", 1L);
        RecruitingRound round = RecruitingRound.createRegular(season, RecruitingRoundConfiguration.of(
            List.of(ChallengerTrack.PLAN, ChallengerTrack.DESIGN),
            true,
            Instant.parse("2026-08-01T00:00:00Z"),
            Instant.parse("2026-08-08T00:00:00Z"),
            Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            Instant.parse("2026-08-16T00:00:00Z"),
            null,
            null,
            null
        ));
        ReflectionTestUtils.setField(round, "id", 10L);
        return round;
    }
}
