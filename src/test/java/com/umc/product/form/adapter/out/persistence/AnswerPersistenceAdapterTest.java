package com.umc.product.form.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.support.fixture.FormFixture;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerPersistenceAdapter")
class AnswerPersistenceAdapterTest {

    @Mock FormResponseJpaRepository formResponseJpaRepository;
    @Mock AnswerChoiceJpaRepository answerChoiceJpaRepository;
    @Mock AnswerChoiceQueryRepository answerChoiceQueryRepository;
    @Mock AnswerJpaRepository answerJpaRepository;
    @Mock AnswerQueryRepository answerQueryRepository;

    @Test
    @DisplayName("답변·choice 조회와 통계를 올바른 repository에 위임한다")
    void delegates_load_and_statistics() {
        Fixture fixture = fixture();
        Set<Long> ids = Set.of(1L);
        given(answerJpaRepository.findById(1L)).willReturn(Optional.of(fixture.answer()));
        given(answerQueryRepository.existsByFormResponseIdAndQuestionId(2L, 3L)).willReturn(true);
        given(answerQueryRepository.findAllByFormResponseId(2L)).willReturn(List.of(fixture.answer()));
        given(answerQueryRepository.findAllByFormResponseIdIn(ids)).willReturn(List.of(fixture.answer()));
        given(answerChoiceQueryRepository.findAllByAnswerIdIn(ids)).willReturn(List.of(fixture.choice()));
        given(formResponseJpaRepository.countByFormIdAndStatus(4L, FormResponseStatus.SUBMITTED)).willReturn(5L);
        given(answerChoiceQueryRepository.countVotesByOptionId(4L)).willReturn(Map.of(6L, 2L));
        given(answerChoiceJpaRepository.findSelectedOptionIdsByMember(4L, 7L)).willReturn(List.of(6L));
        given(answerChoiceQueryRepository.findSelectedMemberIdsByOptionId(4L))
            .willReturn(Map.of(6L, List.of(7L)));
        AnswerPersistenceAdapter sut = sut();

        assertThat(sut.findById(1L)).containsSame(fixture.answer());
        assertThat(sut.existsByFormResponseIdAndQuestionId(2L, 3L)).isTrue();
        assertThat(sut.listByFormResponseId(2L)).containsExactly(fixture.answer());
        assertThat(sut.listByFormResponseIds(ids)).containsExactly(fixture.answer());
        assertThat(sut.listChoicesByAnswerIdIn(ids)).containsExactly(fixture.choice());
        assertThat(sut.countTotalParticipants(4L)).isEqualTo(5L);
        assertThat(sut.countVotesByOptionId(4L)).containsEntry(6L, 2L);
        assertThat(sut.findSelectedOptionIdsByMember(4L, 7L)).containsExactly(6L);
        assertThat(sut.findSelectedMemberIdsByOptionId(4L)).containsEntry(6L, List.of(7L));
    }

    @Test
    @DisplayName("저장은 대상별 repository에 위임하고 cascade 삭제는 choice를 먼저 제거한다")
    void delegates_save_and_preserves_delete_order() {
        Fixture fixture = fixture();
        given(answerJpaRepository.save(fixture.answer())).willReturn(fixture.answer());
        given(answerJpaRepository.saveAll(List.of(fixture.answer()))).willReturn(List.of(fixture.answer()));
        given(answerChoiceJpaRepository.saveAll(List.of(fixture.choice()))).willReturn(List.of(fixture.choice()));
        AnswerPersistenceAdapter sut = sut();

        assertThat(sut.save(fixture.answer())).isSameAs(fixture.answer());
        assertThat(sut.saveAll(List.of(fixture.answer()))).containsExactly(fixture.answer());
        assertThat(sut.saveAllChoices(List.of(fixture.choice()))).containsExactly(fixture.choice());
        sut.deleteAllByFormResponseId(1L);
        sut.deleteByFormId(2L);
        sut.deleteByQuestionId(3L);
        sut.deleteByAnswerId(4L);
        sut.deleteChoicesByAnswerId(5L);

        InOrder order = Mockito.inOrder(answerChoiceJpaRepository, answerJpaRepository);
        order.verify(answerChoiceJpaRepository).deleteAllByFormResponseId(1L);
        order.verify(answerJpaRepository).deleteAllByFormResponseId(1L);
        order.verify(answerChoiceJpaRepository).deleteByFormId(2L);
        order.verify(answerJpaRepository).deleteByFormId(2L);
        order.verify(answerChoiceJpaRepository).deleteByQuestionId(3L);
        order.verify(answerJpaRepository).deleteByQuestionId(3L);
        order.verify(answerChoiceJpaRepository).deleteByAnswerId(4L);
        order.verify(answerJpaRepository).deleteById(4L);
        then(answerChoiceJpaRepository).should().deleteByAnswerId(5L);
    }

    private AnswerPersistenceAdapter sut() {
        return new AnswerPersistenceAdapter(
            formResponseJpaRepository, answerChoiceJpaRepository, answerChoiceQueryRepository,
            answerJpaRepository, answerQueryRepository
        );
    }

    private Fixture fixture() {
        Form form = FormFixture.publishedForm();
        FormSection section = FormFixture.section(form, 1L);
        Question question = FormFixture.question(section, QuestionType.RADIO, 1L);
        QuestionOption option = FormFixture.option(question, "선택", 1L);
        FormResponse response = FormFixture.submitted(form, 7L);
        Answer answer = FormFixture.answer(response, question, null);
        return new Fixture(answer, FormFixture.choice(answer, option));
    }

    private record Fixture(Answer answer, AnswerChoice choice) {
    }
}
