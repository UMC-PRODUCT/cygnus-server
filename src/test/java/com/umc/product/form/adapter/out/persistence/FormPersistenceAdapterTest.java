package com.umc.product.form.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class FormPersistenceAdapterTest {

    @Mock
    FormJpaRepository formJpaRepository;

    @InjectMocks
    FormPersistenceAdapter sut;

    @Test
    @DisplayName("batchGetByIds는 중복 ID를 첫 등장 순서로 제거해 한 번 조회한다")
    void batchGetByIds_중복_ID_제거_단일_조회() {
        // given
        Form form10 = form(10L);
        Form form20 = form(20L);
        Form form30 = form(30L);
        given(formJpaRepository.findAllById(List.of(20L, 10L, 30L)))
            .willReturn(List.of(form10, form30, form20));

        // when
        List<Form> result = sut.batchGetByIds(List.of(20L, 10L, 20L, 30L));

        // then
        assertThat(result).extracting(Form::getId).containsExactly(10L, 30L, 20L);
        verify(formJpaRepository, times(1)).findAllById(List.of(20L, 10L, 30L));
    }

    @Test
    @DisplayName("batchGetByIds는 요청한 폼이 하나라도 없으면 FORM_NOT_FOUND를 던진다")
    void batchGetByIds_폼_누락_FORM_NOT_FOUND() {
        // given
        given(formJpaRepository.findAllById(List.of(20L, 10L)))
            .willReturn(List.of(form(20L)));

        // when & then
        assertThatThrownBy(() -> sut.batchGetByIds(List.of(20L, 10L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_NOT_FOUND);
        verify(formJpaRepository, times(1)).findAllById(List.of(20L, 10L));
    }

    @Test
    @DisplayName("batchGetByIds는 null 또는 빈 입력이면 저장소를 호출하지 않는다")
    void batchGetByIds_null_또는_빈_입력_저장소_미호출() {
        assertThat(sut.batchGetByIds(null)).isEmpty();
        assertThat(sut.batchGetByIds(List.of())).isEmpty();
        verifyNoInteractions(formJpaRepository);
    }

    private Form form(Long id) {
        Form form = Form.createDraft("폼 " + id, 1L);
        ReflectionTestUtils.setField(form, "id", id);
        return form;
    }
}
