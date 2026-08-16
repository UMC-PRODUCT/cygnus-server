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

    private UpsertRecruitingApplicationFormCommand command(List<SectionEntry> sections) {
        return UpsertRecruitingApplicationFormCommand.builder()
            .seasonId(10L)
            .roundId(20L)
            .requesterMemberId(99L)
            .description("지원서")
            .sections(sections)
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
