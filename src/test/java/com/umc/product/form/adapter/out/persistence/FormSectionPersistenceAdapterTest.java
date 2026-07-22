package com.umc.product.form.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormSection;
import com.umc.product.support.fixture.FormFixture;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormSectionPersistenceAdapter")
class FormSectionPersistenceAdapterTest {

    @Mock FormSectionJpaRepository jpaRepository;
    @Mock FormSectionQueryRepository queryRepository;

    @Test
    @DisplayName("저장·조회·목록·삭제 port를 각각 올바른 repository에 위임한다")
    void delegates_all_operations() {
        Form form = FormFixture.publishedForm();
        FormSection section = FormFixture.section(form, 1L);
        List<FormSection> sections = List.of(section);
        given(jpaRepository.save(section)).willReturn(section);
        given(jpaRepository.saveAll(sections)).willReturn(sections);
        given(jpaRepository.findById(1L)).willReturn(Optional.of(section));
        given(queryRepository.findAllByFormId(2L)).willReturn(sections);
        given(queryRepository.findAllByFormIdIn(List.of(2L))).willReturn(sections);
        FormSectionPersistenceAdapter sut = new FormSectionPersistenceAdapter(jpaRepository, queryRepository);

        assertThat(sut.save(section)).isSameAs(section);
        assertThat(sut.saveAll(sections)).isSameAs(sections);
        assertThat(sut.findById(1L)).containsSame(section);
        assertThat(sut.listByFormId(2L)).isSameAs(sections);
        assertThat(sut.listByFormIds(List.of(2L))).isSameAs(sections);
        sut.deleteById(1L);
        sut.deleteByFormId(2L);

        then(jpaRepository).should().deleteById(1L);
        then(jpaRepository).should().deleteByFormId(2L);
    }
}
