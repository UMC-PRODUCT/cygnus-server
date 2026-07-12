package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormResponseCommand;
import com.umc.product.form.application.port.in.command.dto.SubmitDraftFormResponseCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationQuestionScopeInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
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

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationCommandServiceTest {

    @Mock
    LoadRecruitingApplicationFormPort loadApplicationFormPort;

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

    private RecruitingApplicationForm publishedForm() {
        RecruitingApplicationForm form = RecruitingApplicationForm.create(configuredRound(), 500L);
        ReflectionTestUtils.setField(form, "id", 100L);
        form.publish();
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
