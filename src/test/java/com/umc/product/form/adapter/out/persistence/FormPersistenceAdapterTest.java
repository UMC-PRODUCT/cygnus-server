package com.umc.product.form.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.support.fixture.FormFixture;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormPersistenceAdapter")
class FormPersistenceAdapterTest {

    @Mock FormJpaRepository formJpaRepository;

    @Test
    @DisplayName("단건 저장·조회·삭제를 JPA repository에 위임한다")
    void delegates_single_operations() {
        Form form = FormFixture.publishedForm();
        given(formJpaRepository.save(form)).willReturn(form);
        given(formJpaRepository.findById(1L)).willReturn(Optional.of(form));
        FormPersistenceAdapter sut = new FormPersistenceAdapter(formJpaRepository);

        assertThat(sut.save(form)).isSameAs(form);
        assertThat(sut.findById(1L)).containsSame(form);
        sut.deleteById(1L);

        then(formJpaRepository).should().deleteById(1L);
    }

    @Test
    @DisplayName("batch 조회는 빈 입력을 단축하고 중복 ID를 최초 순서대로 제거한다")
    void batch_get_short_circuits_and_deduplicates() {
        Form first = FormFixture.publishedForm();
        Form second = FormFixture.publishedForm();
        given(formJpaRepository.findAllById(List.of(2L, 1L))).willReturn(List.of(second, first));
        FormPersistenceAdapter sut = new FormPersistenceAdapter(formJpaRepository);

        assertThat(sut.batchGetByIds(null)).isEmpty();
        assertThat(sut.batchGetByIds(List.of())).isEmpty();
        assertThat(sut.batchGetByIds(List.of(2L, 1L, 2L))).containsExactly(second, first);

        then(formJpaRepository).should().findAllById(List.of(2L, 1L));
    }

    @Test
    @DisplayName("batch 결과에 누락된 폼이 있으면 전체 조회 실패로 처리한다")
    void batch_get_requires_all_forms() {
        given(formJpaRepository.findAllById(List.of(1L, 2L)))
            .willReturn(List.of(FormFixture.publishedForm()));
        FormPersistenceAdapter sut = new FormPersistenceAdapter(formJpaRepository);

        assertThatThrownBy(() -> sut.batchGetByIds(List.of(1L, 2L)))
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(FormErrorCode.FORM_NOT_FOUND));
    }
}
