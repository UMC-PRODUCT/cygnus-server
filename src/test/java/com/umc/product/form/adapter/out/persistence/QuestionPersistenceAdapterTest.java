package com.umc.product.form.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.support.fixture.FormFixture;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionPersistenceAdapter")
class QuestionPersistenceAdapterTest {

    @Mock QuestionJpaRepository jpaRepository;
    @Mock QuestionQueryRepository queryRepository;

    @Test
    @DisplayName("질문 삭제는 실제 삭제 건수가 없을 때 NOT_FOUND를 반환한다")
    void scoped_delete_requires_existing_question() {
        given(jpaRepository.deleteByFormIdAndQuestionId(1L, 2L)).willReturn(1);
        given(jpaRepository.deleteByFormIdAndQuestionId(1L, 404L)).willReturn(0);
        QuestionPersistenceAdapter sut = new QuestionPersistenceAdapter(jpaRepository, queryRepository);

        sut.deleteByFormIdAndQuestionId(1L, 2L);
        assertThatThrownBy(() -> sut.deleteByFormIdAndQuestionId(1L, 404L))
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(FormErrorCode.QUESTION_NOT_FOUND));
    }

    @Test
    @DisplayName("저장·조회·목록·삭제 port를 각각 올바른 repository에 위임한다")
    void delegates_all_operations() {
        Form form = FormFixture.publishedForm();
        FormSection section = FormFixture.section(form, 1L);
        Question question = FormFixture.question(section, QuestionType.SHORT_TEXT, 1L);
        List<Question> questions = List.of(question);
        Set<Long> ids = Set.of(1L);
        given(jpaRepository.findAllByFormSectionIdIn(ids)).willReturn(questions);
        given(jpaRepository.existsByIdAndFormSection_Form_Id(1L, 2L)).willReturn(true);
        given(jpaRepository.findById(1L)).willReturn(Optional.of(question));
        given(jpaRepository.findFirstByFormIdAndType(2L, QuestionType.SHORT_TEXT))
            .willReturn(Optional.of(question));
        given(jpaRepository.findAllByFormIdAndIsActiveTrue(2L)).willReturn(questions);
        given(queryRepository.findAllBySectionId(3L)).willReturn(questions);
        given(queryRepository.findAllBySectionIdIn(ids)).willReturn(questions);
        given(queryRepository.findAllByIdIn(ids)).willReturn(questions);
        given(jpaRepository.save(question)).willReturn(question);
        given(jpaRepository.saveAll(questions)).willReturn(questions);
        QuestionPersistenceAdapter sut = new QuestionPersistenceAdapter(jpaRepository, queryRepository);

        assertThat(sut.findAllByFormSectionIdIn(ids)).isSameAs(questions);
        assertThat(sut.existsByIdAndFormId(1L, 2L)).isTrue();
        assertThat(sut.findById(1L)).containsSame(question);
        assertThat(sut.findFirstByFormIdAndType(2L, QuestionType.SHORT_TEXT)).containsSame(question);
        assertThat(sut.listByFormId(2L)).isSameAs(questions);
        assertThat(sut.listBySectionId(3L)).isSameAs(questions);
        assertThat(sut.listBySectionIdIn(ids)).isSameAs(questions);
        assertThat(sut.listByIdIn(ids)).isSameAs(questions);
        assertThat(sut.save(question)).isSameAs(question);
        assertThat(sut.saveAll(questions)).isSameAs(questions);
        sut.deleteById(1L);
        sut.deleteByFormId(2L);
        sut.deleteBySectionId(3L);

        then(jpaRepository).should().deleteById(1L);
        then(jpaRepository).should().deleteByFormId(2L);
        then(jpaRepository).should().deleteBySectionId(3L);
    }
}
