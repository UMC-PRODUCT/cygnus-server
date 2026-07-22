package com.umc.product.form.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.query.GetAnswerUseCase;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@DisplayName("FormResponseQueryService 잔여 query 계약")
class FormResponseQueryServiceResidualTest {

    LoadFormResponsePort port;
    GetAnswerUseCase answers;
    FormResponseQueryService sut;

    @BeforeEach
    void setUp() {
        port = mock(LoadFormResponsePort.class);
        answers = mock(GetAnswerUseCase.class);
        sut = new FormResponseQueryService(port, answers, mock(SecureTokenGenerator.class));
    }

    @Test
    @DisplayName("exists/find/get은 port 결과를 보존하고 get 부재만 예외로 변환한다")
    void exists_find_get_contract() {
        FormResponse response = response(10L, 2L);
        given(port.existsByFormId(1L)).willReturn(true);
        given(port.findById(10L)).willReturn(Optional.of(response));
        given(port.findById(404L)).willReturn(Optional.empty());

        assertThat(sut.existsByFormId(1L)).isTrue();
        assertThat(sut.findById(10L)).get().extracting(info -> info.id()).isEqualTo(10L);
        assertThat(sut.findById(404L)).isEmpty();
        assertThat(sut.getById(10L).respondentMemberId()).isEqualTo(2L);
        assertError(() -> sut.getById(404L), FormErrorCode.FORM_RESPONSE_NOT_FOUND);
    }

    @Test
    @DisplayName("form·상태·응답자별 목록과 단건 조회를 info로 변환한다")
    void list_and_member_lookup_contract() {
        FormResponse response = response(10L, 2L);
        given(port.listByFormId(1L)).willReturn(List.of(response));
        given(port.listSubmittedByFormId(1L)).willReturn(List.of(response));
        given(port.findAllDraftByRespondentMemberId(2L)).willReturn(List.of(response));
        given(port.findDraftByFormIdAndRespondentMemberId(1L, 2L)).willReturn(Optional.of(response));
        given(port.findSubmittedByFormIdAndRespondentMemberId(1L, 2L)).willReturn(Optional.empty());

        assertThat(sut.listByFormId(1L)).singleElement().extracting(info -> info.id()).isEqualTo(10L);
        assertThat(sut.listSubmittedByFormId(1L)).hasSize(1);
        assertThat(sut.listDraftByRespondentMemberId(2L)).hasSize(1);
        assertThat(sut.findDraftByFormIdAndRespondentMemberId(1L, 2L)).isPresent();
        assertThat(sut.findSubmittedByFormIdAndRespondentMemberId(1L, 2L)).isEmpty();
    }

    @Test
    @DisplayName("기명 응답 상세은 answer 목록을 결합하고 없는 ID는 거부한다")
    void response_with_answers_contract() {
        FormResponse response = response(10L, 2L);
        AnswerInfo answer = mock(AnswerInfo.class);
        given(port.findById(10L)).willReturn(Optional.of(response));
        given(port.findById(404L)).willReturn(Optional.empty());
        given(answers.listByFormResponseId(10L)).willReturn(List.of(answer));

        assertThat(sut.getResponseWithAnswers(10L).answers()).containsExactly(answer);
        assertError(() -> sut.getResponseWithAnswers(404L), FormErrorCode.FORM_RESPONSE_NOT_FOUND);
    }

    @Test
    @DisplayName("batch 상세 조회는 null·empty를 단축하고 answer 누락은 빈 목록으로 조립한다")
    void batch_empty_and_partial_answer_map() {
        assertThat(sut.findResponsesWithAnswers(null)).isEmpty();
        assertThat(sut.findResponsesWithAnswers(Set.of())).isEmpty();
        then(port).shouldHaveNoInteractions();

        FormResponse withAnswers = response(10L, 2L);
        FormResponse withoutAnswers = response(11L, 3L);
        AnswerInfo answer = mock(AnswerInfo.class);
        Set<Long> ids = Set.of(10L, 11L);
        given(port.listByIdsWithForm(ids)).willReturn(List.of(withAnswers, withoutAnswers));
        given(answers.listByFormResponseIds(ids)).willReturn(Map.of(10L, List.of(answer)));

        var result = sut.findResponsesWithAnswers(ids);

        assertThat(result).containsOnlyKeys(10L, 11L);
        assertThat(result.get(10L).answers()).containsExactly(answer);
        assertThat(result.get(11L).answers()).isEmpty();
    }

    private FormResponse response(Long id, Long memberId) {
        Form form = Form.createPublished(1L, "폼", false);
        ReflectionTestUtils.setField(form, "id", 1L);
        FormResponse response = FormResponse.createDraft(form, memberId);
        ReflectionTestUtils.setField(response, "id", id);
        return response;
    }

    private void assertError(Runnable action, FormErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code));
    }
}
