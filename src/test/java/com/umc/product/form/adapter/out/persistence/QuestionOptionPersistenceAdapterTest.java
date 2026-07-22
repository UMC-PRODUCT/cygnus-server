package com.umc.product.form.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.support.fixture.FormFixture;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionOptionPersistenceAdapter")
class QuestionOptionPersistenceAdapterTest {

    @Mock QuestionOptionJpaRepository jpaRepository;
    @Mock QuestionOptionQueryRepository queryRepository;

    @Test
    @DisplayName("저장·조회·목록·삭제 port를 각각 올바른 repository에 위임한다")
    void delegates_all_operations() {
        Form form = FormFixture.publishedForm();
        FormSection section = FormFixture.section(form, 1L);
        Question question = FormFixture.question(section, QuestionType.RADIO, 1L);
        QuestionOption option = FormFixture.option(question, "선택", 1L);
        List<QuestionOption> options = List.of(option);
        given(jpaRepository.save(option)).willReturn(option);
        given(jpaRepository.saveAll(options)).willReturn(options);
        given(jpaRepository.findById(1L)).willReturn(Optional.of(option));
        given(jpaRepository.existsByIdAndQuestion_Id(1L, 2L)).willReturn(true);
        given(queryRepository.findAllByQuestionId(2L)).willReturn(options);
        given(queryRepository.findAllByQuestionIdIn(Set.of(2L))).willReturn(options);
        QuestionOptionPersistenceAdapter sut = new QuestionOptionPersistenceAdapter(jpaRepository, queryRepository);

        assertThat(sut.save(option)).isSameAs(option);
        assertThat(sut.saveAll(options)).isSameAs(options);
        assertThat(sut.findById(1L)).containsSame(option);
        assertThat(sut.existsByIdAndQuestionId(null, 2L)).isFalse();
        assertThat(sut.existsByIdAndQuestionId(1L, null)).isFalse();
        assertThat(sut.existsByIdAndQuestionId(1L, 2L)).isTrue();
        assertThat(sut.listByQuestionId(2L)).isSameAs(options);
        assertThat(sut.listByQuestionIdIn(Set.of(2L))).isSameAs(options);
        sut.deleteById(1L);
        sut.deleteAllByQuestionId(2L);
        sut.deleteByFormId(3L);
        sut.deleteBySectionId(4L);

        then(jpaRepository).should().deleteById(1L);
        then(jpaRepository).should().deleteAllByQuestionId(2L);
        then(jpaRepository).should().deleteByFormId(3L);
        then(jpaRepository).should().deleteBySectionId(4L);
    }
}
