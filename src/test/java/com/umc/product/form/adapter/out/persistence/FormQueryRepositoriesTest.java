package com.umc.product.form.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.support.PersistenceAdapterTest;
import com.umc.product.support.fixture.FormFixture;

import jakarta.persistence.EntityManager;

@PersistenceAdapterTest
@Import({
    AnswerChoiceQueryRepository.class,
    AnswerQueryRepository.class,
    FormResponseQueryRepository.class,
    FormSectionQueryRepository.class,
    QuestionOptionQueryRepository.class,
    QuestionQueryRepository.class
})
@DisplayName("Form QueryDSL repository 통합")
class FormQueryRepositoriesTest {

    @Autowired EntityManager entityManager;
    @Autowired FormJpaRepository formJpaRepository;
    @Autowired FormResponseJpaRepository formResponseJpaRepository;
    @Autowired FormSectionJpaRepository formSectionJpaRepository;
    @Autowired QuestionJpaRepository questionJpaRepository;
    @Autowired QuestionOptionJpaRepository questionOptionJpaRepository;
    @Autowired AnswerJpaRepository answerJpaRepository;
    @Autowired AnswerChoiceJpaRepository answerChoiceJpaRepository;
    @Autowired AnswerChoiceQueryRepository answerChoiceQueryRepository;
    @Autowired AnswerQueryRepository answerQueryRepository;
    @Autowired FormResponseQueryRepository formResponseQueryRepository;
    @Autowired FormSectionQueryRepository formSectionQueryRepository;
    @Autowired QuestionOptionQueryRepository questionOptionQueryRepository;
    @Autowired QuestionQueryRepository questionQueryRepository;

    @Test
    @DisplayName("빈 batch 입력은 SQL 없이 빈 결과를 반환한다")
    void empty_batch_inputs_short_circuit() {
        assertThat(answerQueryRepository.findAllByFormResponseIdIn(null)).isEmpty();
        assertThat(answerQueryRepository.findAllByFormResponseIdIn(Set.of())).isEmpty();
        assertThat(answerChoiceQueryRepository.findAllByAnswerIdIn(Set.of())).isEmpty();
        assertThat(formResponseQueryRepository.findAllByIdInWithForm(null)).isEmpty();
        assertThat(formResponseQueryRepository.findAllByIdInWithForm(Set.of())).isEmpty();
        assertThat(formSectionQueryRepository.findAllByFormIdIn(null)).isEmpty();
        assertThat(formSectionQueryRepository.findAllByFormIdIn(List.of())).isEmpty();
        assertThat(questionOptionQueryRepository.findAllByQuestionIdIn(Set.of())).isEmpty();
        assertThat(questionQueryRepository.findAllBySectionIdIn(Set.of())).isEmpty();
        assertThat(questionQueryRepository.findAllByIdIn(Set.of())).isEmpty();
        assertThat(new FormQueryRepository()).isNotNull();
    }

    @Test
    @DisplayName("섹션·질문·선택지는 orderNo와 active 계약에 따라 조회된다")
    void structure_queries_preserve_order_and_active_filter() {
        Form form = save(FormFixture.publishedForm());
        FormSection laterSection = save(FormFixture.section(form, 2L));
        FormSection earlierSection = save(FormFixture.section(form, 1L));
        Question laterQuestion = save(FormFixture.question(earlierSection, QuestionType.RADIO, 2L));
        Question earlierQuestion = save(FormFixture.question(earlierSection, QuestionType.RADIO, 1L));
        Question inactiveQuestion = FormFixture.question(earlierSection, QuestionType.RADIO, 3L);
        inactiveQuestion.deactivate();
        inactiveQuestion = save(inactiveQuestion);
        Question otherSectionQuestion = save(
            FormFixture.question(laterSection, QuestionType.SHORT_TEXT, 1L));
        QuestionOption laterOption = save(FormFixture.option(earlierQuestion, "B", 2L));
        QuestionOption earlierOption = save(FormFixture.option(earlierQuestion, "A", 1L));
        flushAndClear();

        assertThat(formSectionQueryRepository.findAllByFormId(form.getId()))
            .extracting(FormSection::getId)
            .containsExactly(earlierSection.getId(), laterSection.getId());
        assertThat(formSectionQueryRepository.findAllByFormIdIn(List.of(form.getId())))
            .extracting(FormSection::getId)
            .containsExactly(earlierSection.getId(), laterSection.getId());
        assertThat(questionQueryRepository.findAllBySectionId(earlierSection.getId()))
            .extracting(Question::getId)
            .containsExactly(earlierQuestion.getId(), laterQuestion.getId());
        assertThat(questionQueryRepository.findAllBySectionIdIn(
            Set.of(earlierSection.getId(), laterSection.getId())))
            .extracting(Question::getId)
            .containsExactlyInAnyOrder(
                earlierQuestion.getId(), laterQuestion.getId(), otherSectionQuestion.getId());
        assertThat(questionQueryRepository.findAllByIdIn(
            Set.of(laterQuestion.getId(), inactiveQuestion.getId())))
            .extracting(Question::getId)
            .containsExactly(laterQuestion.getId(), inactiveQuestion.getId());
        assertThat(questionOptionQueryRepository.findAllByQuestionId(earlierQuestion.getId()))
            .extracting(QuestionOption::getId)
            .containsExactly(earlierOption.getId(), laterOption.getId());
        assertThat(questionOptionQueryRepository.findAllByQuestionIdIn(Set.of(earlierQuestion.getId())))
            .extracting(QuestionOption::getId)
            .containsExactly(earlierOption.getId(), laterOption.getId());
    }

    @Test
    @DisplayName("응답과 답변은 상태 필터·화면 순서·batch fetch 계약을 보존한다")
    void response_and_answer_queries_preserve_contracts() {
        Form form = save(FormFixture.publishedForm());
        FormSection laterSection = save(FormFixture.section(form, 2L));
        FormSection earlierSection = save(FormFixture.section(form, 1L));
        Question laterQuestion = save(
            FormFixture.question(laterSection, QuestionType.SHORT_TEXT, 1L));
        Question earlierQuestion = save(
            FormFixture.question(earlierSection, QuestionType.SHORT_TEXT, 2L));
        FormResponse draft = save(FormFixture.draft(form, 10L));
        FormResponse submitted = save(FormFixture.submitted(form, 20L));
        Answer laterAnswer = save(FormFixture.answer(draft, laterQuestion, "나중"));
        Answer earlierAnswer = save(FormFixture.answer(draft, earlierQuestion, "먼저"));
        Answer submittedAnswer = save(FormFixture.answer(submitted, earlierQuestion, "제출"));
        flushAndClear();

        assertThat(formResponseQueryRepository.findAllByFormId(form.getId()))
            .extracting(FormResponse::getId)
            .containsExactly(submitted.getId(), draft.getId());
        assertThat(formResponseQueryRepository.findAllSubmittedByFormId(form.getId()))
            .extracting(FormResponse::getId)
            .containsExactly(submitted.getId());
        assertThat(formResponseQueryRepository.findAllByIdInWithForm(
            Set.of(submitted.getId(), draft.getId())))
            .extracting(FormResponse::getId)
            .containsExactly(draft.getId(), submitted.getId());
        assertThat(answerQueryRepository.findAllByFormResponseId(draft.getId()))
            .extracting(Answer::getId)
            .containsExactly(earlierAnswer.getId(), laterAnswer.getId());
        assertThat(answerQueryRepository.findAllByFormResponseIdIn(
            Set.of(draft.getId(), submitted.getId())))
            .extracting(Answer::getId)
            .containsExactly(earlierAnswer.getId(), laterAnswer.getId(), submittedAnswer.getId());
        assertThat(answerQueryRepository.existsByFormResponseIdAndQuestionId(
            draft.getId(), earlierQuestion.getId())).isTrue();
        assertThat(answerQueryRepository.existsByFormResponseIdAndQuestionId(
            draft.getId(), 999_999L)).isFalse();
    }

    @Test
    @DisplayName("투표 집계는 제출 응답만 포함하고 option 순서와 member grouping을 보존한다")
    void choice_queries_only_aggregate_submitted_responses() {
        Form form = save(FormFixture.publishedForm());
        FormSection section = save(FormFixture.section(form, 1L));
        Question question = save(FormFixture.question(section, QuestionType.CHECKBOX, 1L));
        QuestionOption laterOption = save(FormFixture.option(question, "B", 2L));
        QuestionOption earlierOption = save(FormFixture.option(question, "A", 1L));
        FormResponse first = save(FormFixture.submitted(form, 10L));
        FormResponse second = save(FormFixture.submitted(form, 20L));
        FormResponse draft = save(FormFixture.draft(form, 30L));
        Answer firstAnswer = save(FormFixture.answer(first, question, null));
        Answer secondAnswer = save(FormFixture.answer(second, question, null));
        Answer draftAnswer = save(FormFixture.answer(draft, question, null));
        save(FormFixture.choice(firstAnswer, earlierOption));
        save(FormFixture.choice(secondAnswer, earlierOption));
        save(FormFixture.choice(secondAnswer, laterOption));
        save(FormFixture.choice(draftAnswer, laterOption));
        flushAndClear();

        assertThat(answerChoiceQueryRepository.countVotesByOptionId(form.getId()))
            .containsExactlyInAnyOrderEntriesOf(Map.of(earlierOption.getId(), 2L, laterOption.getId(), 1L));
        assertThat(answerChoiceJpaRepository.findSelectedOptionIdsByMember(form.getId(), 20L))
            .containsExactlyInAnyOrder(earlierOption.getId(), laterOption.getId());
        Map<Long, List<Long>> memberIds =
            answerChoiceQueryRepository.findSelectedMemberIdsByOptionId(form.getId());
        assertThat(memberIds.get(earlierOption.getId())).containsExactlyInAnyOrder(10L, 20L);
        assertThat(memberIds.get(laterOption.getId())).containsExactly(20L);
        assertThat(answerChoiceQueryRepository.findAllByAnswerIdIn(
            Set.of(firstAnswer.getId(), secondAnswer.getId())))
            .extracting(choice -> choice.getQuestionOption().getOrderNo())
            .containsExactly(1L, 1L, 2L);
    }

    private Form save(Form value) {
        return formJpaRepository.save(value);
    }

    private FormSection save(FormSection value) {
        return formSectionJpaRepository.save(value);
    }

    private Question save(Question value) {
        return questionJpaRepository.save(value);
    }

    private QuestionOption save(QuestionOption value) {
        return questionOptionJpaRepository.save(value);
    }

    private FormResponse save(FormResponse value) {
        return formResponseJpaRepository.save(value);
    }

    private Answer save(Answer value) {
        return answerJpaRepository.save(value);
    }

    private AnswerChoice save(AnswerChoice value) {
        return answerChoiceJpaRepository.save(value);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
