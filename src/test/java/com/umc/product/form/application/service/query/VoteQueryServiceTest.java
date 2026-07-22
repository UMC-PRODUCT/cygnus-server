package com.umc.product.form.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@DisplayName("VoteQueryService")
class VoteQueryServiceTest {

    LoadFormPort forms;
    LoadAnswerPort answers;
    LoadFormSectionPort sections;
    LoadQuestionPort questions;
    LoadQuestionOptionPort options;
    VoteQueryService sut;

    @BeforeEach
    void setUp() {
        forms = mock(LoadFormPort.class);
        answers = mock(LoadAnswerPort.class);
        sections = mock(LoadFormSectionPort.class);
        questions = mock(LoadQuestionPort.class);
        options = mock(LoadQuestionOptionPort.class);
        sut = new VoteQueryService(forms, answers, sections, questions, options);
    }

    @Test
    @DisplayName("기명 RADIO 투표는 득표율·내 선택·옵션별 선택 member를 조립한다")
    void assembles_named_vote_statistics() {
        Fixture fixture = fixture(false, QuestionType.RADIO);
        stubStructure(fixture);
        given(answers.countVotesByOptionId(1L)).willReturn(Map.of(30L, 2L));
        given(answers.countTotalParticipants(1L)).willReturn(3L);
        given(answers.findSelectedOptionIdsByMember(1L, 9L)).willReturn(List.of(30L));
        given(answers.findSelectedMemberIdsByOptionId(1L)).willReturn(Map.of(30L, List.of(9L, 10L)));

        var result = sut.getVoteInfo(1L, 9L);

        assertThat(result.allowMultipleChoice()).isFalse();
        assertThat(result.totalParticipants()).isEqualTo(3L);
        assertThat(result.mySelectedOptionIds()).containsExactly(30L);
        assertThat(result.options()).hasSize(2);
        assertThat(result.options().get(0).voteRate()).isEqualByComparingTo(new BigDecimal("66.7"));
        assertThat(result.options().get(0).selectedMemberIds()).containsExactly(9L, 10L);
        assertThat(result.options().get(1).voteCount()).isZero();
        assertThat(result.options().get(1).selectedMemberIds()).isEmpty();
    }

    @Test
    @DisplayName("익명 CHECKBOX 투표는 member 목록을 조회하지 않고 참여자 0명 득표율을 0.0으로 반환한다")
    void anonymous_vote_hides_members_and_handles_zero_participants() {
        Fixture fixture = fixture(true, QuestionType.CHECKBOX);
        stubStructure(fixture);
        given(answers.countVotesByOptionId(1L)).willReturn(Map.of());
        given(answers.countTotalParticipants(1L)).willReturn(0L);
        given(answers.findSelectedOptionIdsByMember(1L, 9L)).willReturn(List.of());

        var result = sut.getVoteInfo(1L, 9L);

        assertThat(result.isAnonymous()).isTrue();
        assertThat(result.allowMultipleChoice()).isTrue();
        assertThat(result.options()).allSatisfy(option -> {
            assertThat(option.voteRate()).isEqualByComparingTo(new BigDecimal("0.0"));
            assertThat(option.selectedMemberIds()).isEmpty();
        });
        then(answers).should(org.mockito.Mockito.never()).findSelectedMemberIdsByOptionId(1L);
    }

    @Test
    @DisplayName("폼·섹션·질문 누락은 명시적인 구조 오류로 fail-closed한다")
    void missing_structure_paths() {
        given(forms.findById(404L)).willReturn(Optional.empty());
        assertError(() -> sut.getVoteInfo(404L, 9L), FormErrorCode.FORM_NOT_FOUND);

        Form form = form(false);
        given(forms.findById(1L)).willReturn(Optional.of(form));
        given(answers.countVotesByOptionId(1L)).willReturn(Map.of());
        given(answers.findSelectedOptionIdsByMember(1L, 9L)).willReturn(List.of());
        given(sections.listByFormId(1L)).willReturn(List.of());
        assertError(() -> sut.getVoteInfo(1L, 9L), FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);

        FormSection section = section(form);
        given(sections.listByFormId(1L)).willReturn(List.of(section));
        given(questions.findAllByFormSectionIdIn(Set.of(10L))).willReturn(List.of());
        assertError(() -> sut.getVoteInfo(1L, 9L), FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);
    }

    @Test
    @DisplayName("primary question은 첫 질문 ID를 반환하고 빈 구조는 거부한다")
    void primary_question_contract() {
        Question question = question(section(form(false)), QuestionType.RADIO);
        given(questions.listByFormId(1L)).willReturn(List.of(question));
        given(questions.listByFormId(2L)).willReturn(List.of());

        assertThat(sut.getPrimaryQuestionId(1L)).isEqualTo(20L);
        assertError(() -> sut.getPrimaryQuestionId(2L), FormErrorCode.INVALID_VOTE_FORM_STRUCTURE);
    }

    private void stubStructure(Fixture fixture) {
        given(forms.findById(1L)).willReturn(Optional.of(fixture.form()));
        given(sections.listByFormId(1L)).willReturn(List.of(fixture.section()));
        given(questions.findAllByFormSectionIdIn(Set.of(10L))).willReturn(List.of(fixture.question()));
        given(options.listByQuestionId(20L)).willReturn(fixture.options());
    }

    private Fixture fixture(boolean anonymous, QuestionType type) {
        Form form = form(anonymous);
        FormSection section = section(form);
        Question question = question(section, type);
        QuestionOption first = option(30L, question, "A");
        QuestionOption second = option(31L, question, "B");
        return new Fixture(form, section, question, List.of(first, second));
    }

    private Form form(boolean anonymous) {
        Form form = Form.createPublished(9L, "투표", anonymous);
        ReflectionTestUtils.setField(form, "id", 1L);
        return form;
    }

    private FormSection section(Form form) {
        FormSection section = FormSection.create(form, "투표", null, 1L);
        ReflectionTestUtils.setField(section, "id", 10L);
        return section;
    }

    private Question question(FormSection section, QuestionType type) {
        Question question = Question.create("투표", type, true, 1L);
        question.assignTo(section);
        ReflectionTestUtils.setField(question, "id", 20L);
        return question;
    }

    private QuestionOption option(Long id, Question question, String content) {
        QuestionOption option = QuestionOption.create(content, id - 29L, false);
        option.assignTo(question);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    private void assertError(Runnable action, FormErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code));
    }

    private record Fixture(
        Form form,
        FormSection section,
        Question question,
        List<QuestionOption> options
    ) {
    }
}
