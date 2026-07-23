package com.umc.product.form.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.application.port.in.query.dto.ScheduleOverlapSlotInfo;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.application.port.out.LoadFormResponsePort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class ScheduleOverlapQueryServiceTest {

    private static final Long FORM_ID = 100L;
    private static final Long OTHER_FORM_ID = 999L;
    private static final Long QUESTION_ID = 400L;

    private static final Instant SLOT_A = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant SLOT_B = Instant.parse("2026-08-01T10:15:00Z");
    private static final Instant SLOT_C = Instant.parse("2026-08-01T10:30:00Z");

    @Mock
    LoadFormResponsePort loadFormResponsePort;
    @Mock
    LoadAnswerPort loadAnswerPort;

    @InjectMocks
    ScheduleOverlapQueryService sut;

    @Test
    @DisplayName("빈 responseId Set 이면 예외 없이 빈 리스트 반환")
    void 빈_입력_빈_리스트_반환() {
        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null responseId Set 이면 예외 없이 빈 리스트 반환")
    void null_입력_빈_리스트_반환() {
        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("모든 응답이 모든 슬롯을 표시하면 각 슬롯에 전원 포함")
    void 완전_교집합() {
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        FormResponse r2 = submittedResponse(2L, FORM_ID);
        FormResponse r3 = submittedResponse(3L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L, 3L)))
            .willReturn(List.of(r1, r2, r3));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L, 2L, 3L)))
            .willReturn(List.of(
                scheduleAnswer(r1, Set.of(SLOT_A, SLOT_B)),
                scheduleAnswer(r2, Set.of(SLOT_A, SLOT_B)),
                scheduleAnswer(r3, Set.of(SLOT_A, SLOT_B))
            ));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, Set.of(1L, 2L, 3L));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).startsAt()).isEqualTo(SLOT_A);
        assertThat(result.get(0).availableResponseIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(result.get(1).startsAt()).isEqualTo(SLOT_B);
        assertThat(result.get(1).availableResponseIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("부분 교집합: 슬롯마다 available 응답자 조합이 정확히 뒤집혀 나온다")
    void 부분_교집합() {
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        FormResponse r2 = submittedResponse(2L, FORM_ID);
        FormResponse r3 = submittedResponse(3L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L, 3L)))
            .willReturn(List.of(r1, r2, r3));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L, 2L, 3L)))
            .willReturn(List.of(
                scheduleAnswer(r1, Set.of(SLOT_A, SLOT_B)),
                scheduleAnswer(r2, Set.of(SLOT_B, SLOT_C)),
                scheduleAnswer(r3, Set.of(SLOT_A, SLOT_C))
            ));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, Set.of(1L, 2L, 3L));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).startsAt()).isEqualTo(SLOT_A);
        assertThat(result.get(0).availableResponseIds()).containsExactlyInAnyOrder(1L, 3L);
        assertThat(result.get(1).startsAt()).isEqualTo(SLOT_B);
        assertThat(result.get(1).availableResponseIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.get(2).startsAt()).isEqualTo(SLOT_C);
        assertThat(result.get(2).availableResponseIds()).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("아무도 표시하지 않은 슬롯은 결과에 포함되지 않음")
    void 빈_슬롯_제외() {
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L))).willReturn(List.of(r1));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L)))
            .willReturn(List.of(scheduleAnswer(r1, Set.of(SLOT_A))));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, Set.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).startsAt()).isEqualTo(SLOT_A);
    }

    @Test
    @DisplayName("존재하지 않는 responseId 포함 시 FORM_RESPONSE_NOT_FOUND")
    void 존재하지_않는_응답_예외() {
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L))).willReturn(List.of(r1));

        assertThatThrownBy(() -> sut.getOverlap(FORM_ID, Set.of(1L, 2L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 form 의 responseId 섞이면 FORM_RESPONSE_NOT_IN_FORM")
    void 다른_form_응답_섞임_예외() {
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        FormResponse rOther = submittedResponse(2L, OTHER_FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L)))
            .willReturn(List.of(r1, rOther));

        assertThatThrownBy(() -> sut.getOverlap(FORM_ID, Set.of(1L, 2L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_NOT_IN_FORM);
    }

    @Test
    @DisplayName("DRAFT 상태 응답 섞이면 FORM_RESPONSE_NOT_SUBMITTED")
    void DRAFT_응답_섞임_예외() {
        FormResponse submitted = submittedResponse(1L, FORM_ID);
        FormResponse draft = draftResponse(2L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L)))
            .willReturn(List.of(submitted, draft));

        assertThatThrownBy(() -> sut.getOverlap(FORM_ID, Set.of(1L, 2L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_NOT_SUBMITTED);
    }

    @Test
    @DisplayName("SCHEDULE 이 아닌 answeredAsType 은 무시")
    void 비_SCHEDULE_답변_무시() {
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L))).willReturn(List.of(r1));
        Answer textAnswer = Answer.create(r1, questionOfType(QuestionType.SHORT_TEXT),
            QuestionType.SHORT_TEXT, "답", null, null);
        Answer scheduleAnswer = scheduleAnswer(r1, Set.of(SLOT_A));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L)))
            .willReturn(List.of(textAnswer, scheduleAnswer));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, Set.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).startsAt()).isEqualTo(SLOT_A);
    }

    @Test
    @DisplayName("결과는 startsAt 오름차순 정렬")
    void 결과_오름차순_정렬() {
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L))).willReturn(List.of(r1));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L)))
            .willReturn(List.of(scheduleAnswer(r1, Set.of(SLOT_C, SLOT_A, SLOT_B))));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, Set.of(1L));

        assertThat(result).extracting(ScheduleOverlapSlotInfo::startsAt)
            .containsExactly(SLOT_A, SLOT_B, SLOT_C);
    }

    private FormResponse submittedResponse(Long id, Long formId) {
        FormResponse fr = FormResponse.createDraft(publishedForm(formId), 999L);
        ReflectionTestUtils.setField(fr, "id", id);
        fr.submit(Instant.now(), "127.0.0.1");
        return fr;
    }

    private FormResponse draftResponse(Long id, Long formId) {
        FormResponse fr = FormResponse.createDraft(publishedForm(formId), 999L);
        ReflectionTestUtils.setField(fr, "id", id);
        return fr;
    }

    private Form publishedForm(Long formId) {
        Form form = Form.createDraft("폼", 1L, false);
        ReflectionTestUtils.setField(form, "id", formId);
        form.publish();
        return form;
    }

    private Question questionOfType(QuestionType type) {
        Question q = Question.create("질문", type, false, 1L);
        ReflectionTestUtils.setField(q, "id", QUESTION_ID);
        return q;
    }

    private Answer scheduleAnswer(FormResponse formResponse, Set<Instant> times) {
        Answer answer = Answer.create(formResponse, questionOfType(QuestionType.SCHEDULE),
            QuestionType.SCHEDULE, null, null, times);
        ReflectionTestUtils.setField(answer, "id", formResponse.getId() + 1000L);
        return answer;
    }
}
