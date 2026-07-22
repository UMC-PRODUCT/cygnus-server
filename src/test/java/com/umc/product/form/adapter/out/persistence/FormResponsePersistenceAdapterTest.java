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
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.support.fixture.FormFixture;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormResponsePersistenceAdapter")
class FormResponsePersistenceAdapterTest {

    @Mock FormResponseJpaRepository jpaRepository;
    @Mock FormResponseQueryRepository queryRepository;

    @Test
    @DisplayName("응답 조회 port는 상태와 access key 계약을 보존해 위임한다")
    void delegates_load_operations() {
        Form form = FormFixture.publishedForm();
        FormResponse response = FormFixture.draft(form, 10L);
        List<FormResponse> responses = List.of(response);
        Set<Long> ids = Set.of(1L);
        given(jpaRepository.findById(1L)).willReturn(Optional.of(response));
        given(queryRepository.findAllByIdInWithForm(ids)).willReturn(responses);
        given(queryRepository.findAllByFormId(2L)).willReturn(responses);
        given(queryRepository.findAllSubmittedByFormId(2L)).willReturn(responses);
        given(jpaRepository.findFirstByForm_IdAndRespondentMemberIdAndStatusOrderByIdDesc(
            2L, 10L, FormResponseStatus.DRAFT)).willReturn(Optional.of(response));
        given(jpaRepository.findByRespondentMemberIdAndStatus(10L, FormResponseStatus.DRAFT))
            .willReturn(responses);
        given(jpaRepository.findIdsByFormIdAndStatus(2L, FormResponseStatus.DRAFT)).willReturn(List.of(1L));
        given(jpaRepository.existsByForm_IdAndRespondentMemberId(2L, 10L)).willReturn(true);
        given(jpaRepository.existsByForm_Id(2L)).willReturn(true);
        given(jpaRepository.countByFormIdAndStatus(2L, FormResponseStatus.SUBMITTED)).willReturn(3L);
        given(jpaRepository.findFirstByForm_IdAndRespondentMemberIdAndStatusOrderByIdDesc(
            2L, 10L, FormResponseStatus.SUBMITTED)).willReturn(Optional.of(response));
        given(jpaRepository.findByResponseAccessKeyHashAndStatus("hash", FormResponseStatus.DRAFT))
            .willReturn(Optional.of(response));
        given(jpaRepository.findByResponseAccessKeyHashAndStatus("hash", FormResponseStatus.SUBMITTED))
            .willReturn(Optional.of(response));
        given(jpaRepository.findByResponseAccessKeyHash("hash")).willReturn(Optional.of(response));
        FormResponsePersistenceAdapter sut = new FormResponsePersistenceAdapter(jpaRepository, queryRepository);

        assertThat(sut.findById(1L)).containsSame(response);
        assertThat(sut.listByIdsWithForm(ids)).isSameAs(responses);
        assertThat(sut.listByFormId(2L)).isSameAs(responses);
        assertThat(sut.listSubmittedByFormId(2L)).isSameAs(responses);
        assertThat(sut.findDraftByFormIdAndRespondentMemberId(2L, 10L)).containsSame(response);
        assertThat(sut.findAllDraftByRespondentMemberId(10L)).isSameAs(responses);
        assertThat(sut.findDraftIdsByFormId(2L)).containsExactly(1L);
        assertThat(sut.existsByFormIdAndMemberId(2L, 10L)).isTrue();
        assertThat(sut.existsByFormId(2L)).isTrue();
        assertThat(sut.findIdsByFormIdAndStatus(2L, FormResponseStatus.DRAFT)).containsExactly(1L);
        assertThat(sut.countSubmittedByFormId(2L)).isEqualTo(3L);
        assertThat(sut.findSubmittedByFormIdAndRespondentMemberId(2L, 10L)).containsSame(response);
        assertThat(sut.findDraftByAccessKeyHash("hash")).containsSame(response);
        assertThat(sut.findSubmittedByAccessKeyHash("hash")).containsSame(response);
        assertThat(sut.findByAccessKeyHash("hash")).containsSame(response);
    }

    @Test
    @DisplayName("응답 저장·일괄 삭제 port를 위임하고 삭제 건수를 반환한다")
    void delegates_save_and_delete_operations() {
        FormResponse response = FormFixture.draft(FormFixture.publishedForm(), 10L);
        given(jpaRepository.save(response)).willReturn(response);
        given(jpaRepository.deleteByFormIdAndStatus(2L, FormResponseStatus.DRAFT)).willReturn(4);
        FormResponsePersistenceAdapter sut = new FormResponsePersistenceAdapter(jpaRepository, queryRepository);

        assertThat(sut.save(response)).isSameAs(response);
        sut.deleteById(1L);
        sut.deleteAllByIds(List.of(1L, 2L));
        assertThat(sut.deleteByFormIdAndStatus(2L, FormResponseStatus.DRAFT)).isEqualTo(4);
        sut.deleteByFormId(2L);

        then(jpaRepository).should().deleteById(1L);
        then(jpaRepository).should().deleteAllByIdInBatch(List.of(1L, 2L));
        then(jpaRepository).should().deleteByFormId(2L);
    }
}
