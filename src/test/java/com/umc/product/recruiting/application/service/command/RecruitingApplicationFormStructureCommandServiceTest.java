package com.umc.product.recruiting.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionOptionUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionOptionCommand;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.Option;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.QuestionWithOptions;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.SectionWithQuestions;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.OptionEntry;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.QuestionEntry;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.SectionEntry;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.RecruitingRoundConfiguration;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingApplicationFormStructureCommandServiceTest {

    @Mock LoadRecruitingRoundPort loadRoundPort;
    @Mock LoadRecruitingApplicationFormPort loadApplicationFormPort;
    @Mock SaveRecruitingApplicationFormPort saveApplicationFormPort;
    @Mock LoadRecruitingFormSectionPolicyPort loadPolicyPort;
    @Mock SaveRecruitingFormSectionPolicyPort savePolicyPort;
    @Mock ManageFormUseCase manageFormUseCase;
    @Mock ManageFormSectionUseCase manageFormSectionUseCase;
    @Mock ManageQuestionUseCase manageQuestionUseCase;
    @Mock ManageQuestionOptionUseCase manageQuestionOptionUseCase;
    @Mock GetFormUseCase getFormUseCase;

    RecruitingApplicationFormStructureCommandService sut;
    RecruitingRound round;

    @BeforeEach
    void setUp() {
        sut = new RecruitingApplicationFormStructureCommandService(
            loadRoundPort,
            loadApplicationFormPort,
            saveApplicationFormPort,
            loadPolicyPort,
            savePolicyPort,
            manageFormUseCase,
            manageFormSectionUseCase,
            manageQuestionUseCase,
            manageQuestionOptionUseCase,
            getFormUseCase
        );
        round = round();
    }

    @Test
    @DisplayName("새 Form 구조는 client section key를 실제 조건부 이동 ID로 변환한다")
    void createFormAndResolveConditionalSectionKey() {
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.empty());
        given(manageFormUseCase.createDraft(any())).willReturn(100L);
        given(saveApplicationFormPort.save(any())).willAnswer(invocation -> {
            RecruitingApplicationForm form = invocation.getArgument(0);
            ReflectionTestUtils.setField(form, "id", 200L);
            return form;
        });
        given(manageFormSectionUseCase.createSection(any())).willReturn(300L, 301L);
        given(manageQuestionUseCase.createQuestion(any())).willReturn(400L, 401L);
        given(manageQuestionOptionUseCase.createOption(any())).willReturn(500L);

        Long result = sut.upsert(command(List.of(
            section("common", RecruitingFormSectionType.COMMON, null, List.of(
                question(QuestionType.RADIO, List.of(option("track")))
            )),
            section("track", RecruitingFormSectionType.TRACK, ChallengerTrack.PLAN, List.of(
                question(QuestionType.LONG_TEXT, List.of())
            ))
        )));

        assertThat(result).isEqualTo(200L);
        ArgumentCaptor<CreateQuestionOptionCommand> captor =
            ArgumentCaptor.forClass(CreateQuestionOptionCommand.class);
        then(manageQuestionOptionUseCase).should().createOption(captor.capture());
        assertThat(captor.getValue().nextSectionId()).isEqualTo(301L);
        then(savePolicyPort).should(org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    @DisplayName("TRACK section은 다른 TRACK section으로 조건부 이동할 수 없다")
    void rejectCrossTrackConditionalTransition() {
        given(loadRoundPort.getById(20L)).willReturn(round);

        assertThatThrownBy(() -> sut.upsert(command(List.of(
            section("plan", RecruitingFormSectionType.TRACK, ChallengerTrack.PLAN, List.of(
                question(QuestionType.RADIO, List.of(option("design")))
            )),
            section("design", RecruitingFormSectionType.TRACK, ChallengerTrack.DESIGN, List.of(
                question(QuestionType.LONG_TEXT, List.of())
            ))
        ))))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID);

        then(manageFormUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 Form의 section ID를 기존 구조 수정 요청에 사용할 수 없다")
    void rejectSectionIdOwnedByAnotherForm() {
        RecruitingApplicationForm applicationForm = RecruitingApplicationForm.create(round, 100L);
        ReflectionTestUtils.setField(applicationForm, "id", 200L);
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(100L)).willReturn(FormWithStructureInfo.builder()
            .formId(100L)
            .sections(List.of(FormWithStructureInfo.SectionWithQuestions.builder()
                .sectionId(300L)
                .title("기존")
                .questions(List.of())
                .build()))
            .build());
        SectionEntry foreignSection = SectionEntry.builder()
            .clientKey("foreign")
            .sectionId(999L)
            .title("다른 Form section")
            .type(RecruitingFormSectionType.COMMON)
            .questions(List.of())
            .build();

        assertThatThrownBy(() -> sut.upsert(command(List.of(foreignSection))))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID);

        then(manageFormSectionUseCase).shouldHaveNoInteractions();
        then(manageQuestionUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("기존 Form 구조는 생성·수정·삭제·재정렬 diff를 ID 소유권 안에서 적용한다")
    void updateExistingStructureWithFullDiff() {
        RecruitingApplicationForm applicationForm = existingApplicationForm();
        FormWithStructureInfo existing = existingStructure();
        RecruitingFormSectionPolicy policy = RecruitingFormSectionPolicy.createCommon(applicationForm, 300L);
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(100L)).willReturn(existing);
        given(loadPolicyPort.listByApplicationFormId(200L)).willReturn(List.of(policy));
        given(manageFormSectionUseCase.createSection(any())).willReturn(302L);
        given(manageQuestionUseCase.createQuestion(any())).willReturn(402L);
        given(manageQuestionOptionUseCase.createOption(any())).willReturn(502L);

        SectionEntry retained = SectionEntry.builder()
            .sectionId(300L)
            .clientKey("retained")
            .title("수정 섹션")
            .type(RecruitingFormSectionType.TRACK)
            .track(ChallengerTrack.PLAN)
            .questions(List.of(
                QuestionEntry.builder()
                    .questionId(400L)
                    .type(QuestionType.RADIO)
                    .title("수정 질문")
                    .required(true)
                    .options(List.of(
                        OptionEntry.builder()
                            .optionId(500L)
                            .content("수정 옵션")
                            .nextSectionKey("new-common")
                            .build(),
                        OptionEntry.builder().content("새 옵션").build()
                    ))
                    .build(),
                question(QuestionType.LONG_TEXT, List.of())
            ))
            .build();
        SectionEntry created = section(
            "new-common", RecruitingFormSectionType.COMMON, null, List.of()
        );

        Long result = sut.upsert(command(List.of(retained, created)));

        assertThat(result).isEqualTo(200L);
        assertThat(policy.getType()).isEqualTo(RecruitingFormSectionType.TRACK);
        assertThat(policy.getTrack()).isEqualTo(ChallengerTrack.PLAN);
        then(manageFormUseCase).should().updateForm(any());
        then(manageFormSectionUseCase).should().updateSection(any());
        then(manageFormSectionUseCase).should().createSection(any());
        then(manageFormSectionUseCase).should().deleteSection(any());
        then(manageFormSectionUseCase).should().reorderSections(any());
        then(manageQuestionUseCase).should().updateQuestion(any());
        then(manageQuestionUseCase).should().createQuestion(any());
        then(manageQuestionUseCase).should().deleteQuestion(any());
        then(manageQuestionUseCase).should().reorderQuestions(any());
        then(manageQuestionOptionUseCase).should().updateOption(any());
        then(manageQuestionOptionUseCase).should().createOption(any());
        then(manageQuestionOptionUseCase).should().deleteOption(any());
        then(manageQuestionOptionUseCase).should().reorderOptions(any());
        then(savePolicyPort).should().deleteByFormSectionId(301L);
    }

    @Test
    @DisplayName("빈·중복·공백 section key와 질문 타입에 맞지 않는 option 구조를 거절한다")
    void rejectMalformedStructure() {
        given(loadRoundPort.getById(20L)).willReturn(round);

        assertInvalid(List.of());
        assertInvalid(List.of(
            section("same", RecruitingFormSectionType.COMMON, null, List.of()),
            section("same", RecruitingFormSectionType.COMMON, null, List.of())
        ));
        assertInvalid(List.of(section(" ", RecruitingFormSectionType.COMMON, null, List.of())));
        assertInvalid(List.of(section(
            "choice", RecruitingFormSectionType.COMMON, null,
            List.of(question(QuestionType.RADIO, List.of()))
        )));
        assertInvalid(List.of(section(
            "text", RecruitingFormSectionType.COMMON, null,
            List.of(question(QuestionType.LONG_TEXT, List.of(option(null))))
        )));
    }

    @Test
    @DisplayName("존재하지 않는 조건부 section과 유효하지 않은 section 정책을 거절한다")
    void rejectInvalidTransitionAndPolicy() {
        given(loadRoundPort.getById(20L)).willReturn(round);

        assertInvalid(List.of(section(
            "common", RecruitingFormSectionType.COMMON, null,
            List.of(question(QuestionType.RADIO, List.of(option("missing"))))
        )));
        assertThatThrownBy(() -> sut.upsert(command(List.of(section(
            "invalid", RecruitingFormSectionType.COMMON, ChallengerTrack.PLAN, List.of()
        )))))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID_TRACK);
    }

    @Test
    @DisplayName("기존 Form의 section·question·option ID 소유권 위조를 모두 거절한다")
    void rejectForeignNestedIds() {
        givenExistingForm();

        assertInvalidOwned(sectionWithIds(300L, 999L, null));
        assertInvalidOwned(sectionWithIds(300L, 400L, 999L));
        assertInvalidOwned(sectionWithIds(null, 400L, null));
        assertInvalidOwned(sectionWithIds(300L, null, 500L));
    }

    @Test
    @DisplayName("DRAFT가 아닌 모집 차수의 Form 구조 변경을 거절한다")
    void rejectNonDraftRound() {
        round.open();
        given(loadRoundPort.getById(20L)).willReturn(round);

        assertThatThrownBy(() -> sut.upsert(command(List.of(
            section("common", RecruitingFormSectionType.COMMON, null, List.of())
        ))))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("기존 section 정책이 누락됐으면 요청 정책으로 복구한다")
    void restoreMissingSectionPolicy() {
        RecruitingApplicationForm applicationForm = existingApplicationForm();
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(100L)).willReturn(existingStructure());
        given(loadPolicyPort.listByApplicationFormId(200L)).willReturn(List.of());

        sut.upsert(command(List.of(SectionEntry.builder()
            .sectionId(300L)
            .clientKey("retained")
            .title("복구")
            .type(RecruitingFormSectionType.COMMON)
            .questions(List.of())
            .build())));

        then(savePolicyPort).should().save(any(RecruitingFormSectionPolicy.class));
    }

    @Test
    @DisplayName("다른 season의 round로 Form을 수정할 수 없다")
    void rejectRoundFromAnotherSeason() {
        given(loadRoundPort.getById(20L)).willReturn(round);
        UpsertRecruitingApplicationFormCommand foreignSeason =
            UpsertRecruitingApplicationFormCommand.builder()
                .seasonId(999L)
                .roundId(20L)
                .requesterMemberId(99L)
                .sections(List.of(section(
                    "common", RecruitingFormSectionType.COMMON, null, List.of()
                )))
                .build();

        assertThatThrownBy(() -> sut.upsert(foreignSeason))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND);
    }

    private UpsertRecruitingApplicationFormCommand command(List<SectionEntry> sections) {
        return UpsertRecruitingApplicationFormCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .requesterMemberId(99L)
            .description("지원서")
            .sections(sections)
            .build();
    }

    private void assertInvalid(List<SectionEntry> sections) {
        assertThatThrownBy(() -> sut.upsert(command(sections)))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID);
    }

    private void assertInvalidOwned(SectionEntry section) {
        assertThatThrownBy(() -> sut.upsert(command(List.of(section))))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID);
    }

    private void givenExistingForm() {
        RecruitingApplicationForm applicationForm = existingApplicationForm();
        given(loadRoundPort.getById(20L)).willReturn(round);
        given(loadApplicationFormPort.findByRoundId(20L)).willReturn(Optional.of(applicationForm));
        given(getFormUseCase.getFormWithStructure(100L)).willReturn(existingStructure());
    }

    private RecruitingApplicationForm existingApplicationForm() {
        RecruitingApplicationForm applicationForm = RecruitingApplicationForm.create(round, 100L);
        ReflectionTestUtils.setField(applicationForm, "id", 200L);
        return applicationForm;
    }

    private FormWithStructureInfo existingStructure() {
        return FormWithStructureInfo.builder()
            .formId(100L)
            .sections(List.of(
                SectionWithQuestions.builder()
                    .sectionId(300L)
                    .title("유지 섹션")
                    .questions(List.of(
                        QuestionWithOptions.builder()
                            .questionId(400L)
                            .type(QuestionType.RADIO)
                            .options(List.of(
                                Option.builder().optionId(500L).content("유지 옵션").build(),
                                Option.builder().optionId(501L).content("삭제 옵션").build()
                            ))
                            .build(),
                        QuestionWithOptions.builder()
                            .questionId(401L)
                            .type(QuestionType.LONG_TEXT)
                            .options(List.of())
                            .build()
                    ))
                    .build(),
                SectionWithQuestions.builder()
                    .sectionId(301L)
                    .title("삭제 섹션")
                    .questions(List.of())
                    .build()
            ))
            .build();
    }

    private SectionEntry sectionWithIds(Long sectionId, Long questionId, Long optionId) {
        OptionEntry nestedOption = OptionEntry.builder()
            .optionId(optionId)
            .content("옵션")
            .build();
        QuestionEntry nestedQuestion = QuestionEntry.builder()
            .questionId(questionId)
            .type(QuestionType.RADIO)
            .title("질문")
            .options(List.of(nestedOption))
            .build();
        return SectionEntry.builder()
            .sectionId(sectionId)
            .clientKey("owned")
            .title("섹션")
            .type(RecruitingFormSectionType.COMMON)
            .questions(List.of(nestedQuestion))
            .build();
    }

    private SectionEntry section(
        String key,
        RecruitingFormSectionType type,
        ChallengerTrack track,
        List<QuestionEntry> questions
    ) {
        return SectionEntry.builder()
            .clientKey(key)
            .title(key)
            .type(type)
            .track(track)
            .questions(questions)
            .build();
    }

    private QuestionEntry question(QuestionType type, List<OptionEntry> options) {
        return QuestionEntry.builder()
            .type(type)
            .title("질문")
            .required(true)
            .options(options)
            .build();
    }

    private OptionEntry option(String nextSectionKey) {
        return OptionEntry.builder()
            .content("선택")
            .nextSectionKey(nextSectionKey)
            .build();
    }

    private RecruitingRound round() {
        RecruitingSeason season = RecruitingSeason.create(1L, 2L);
        ReflectionTestUtils.setField(season, "id", 10L);
        RecruitingRound result = RecruitingRound.createRegular(
            season,
            "15기 본모집",
            RecruitingRoundConfiguration.of(
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
            )
        );
        ReflectionTestUtils.setField(result, "id", 20L);
        return result;
    }
}
