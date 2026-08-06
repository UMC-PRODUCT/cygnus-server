package com.umc.product.form.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class ScheduleOverlapQueryServiceTest {

    private static final Long FORM_ID = 100L;
    private static final Long OTHER_FORM_ID = 999L;
    private static final Long QUESTION_ID = 400L;
    private static final Long OTHER_QUESTION_ID = 401L;

    private static final Instant SLOT_A = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant SLOT_B = Instant.parse("2026-08-01T10:15:00Z");
    private static final Instant SLOT_C = Instant.parse("2026-08-01T10:30:00Z");

    @Mock
    LoadFormResponsePort loadFormResponsePort;
    @Mock
    LoadAnswerPort loadAnswerPort;
    @Mock
    LoadQuestionPort loadQuestionPort;

    @InjectMocks
    ScheduleOverlapQueryService sut;

    @Test
    @DisplayName("빈 responseId Set 이면 예외 없이 빈 리스트 반환")
    void 빈_입력_빈_리스트_반환() {
        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, QUESTION_ID, Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null responseId Set 이면 예외 없이 빈 리스트 반환")
    void null_입력_빈_리스트_반환() {
        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, QUESTION_ID, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("모든 응답이 모든 슬롯을 표시하면 각 슬롯에 전원 포함")
    void 완전_교집합() {
        Question q = scheduleQuestion(QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        FormResponse r2 = submittedResponse(2L, FORM_ID);
        FormResponse r3 = submittedResponse(3L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L, 3L)))
            .willReturn(List.of(r1, r2, r3));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L, 2L, 3L)))
            .willReturn(List.of(
                scheduleAnswer(r1, q, Set.of(SLOT_A, SLOT_B)),
                scheduleAnswer(r2, q, Set.of(SLOT_A, SLOT_B)),
                scheduleAnswer(r3, q, Set.of(SLOT_A, SLOT_B))
            ));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L, 2L, 3L));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).startsAt()).isEqualTo(SLOT_A);
        assertThat(result.get(0).availableResponseIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(result.get(1).startsAt()).isEqualTo(SLOT_B);
        assertThat(result.get(1).availableResponseIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("부분 교집합: 슬롯마다 available 응답자 조합이 정확히 뒤집혀 나온다")
    void 부분_교집합() {
        Question q = scheduleQuestion(QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        FormResponse r2 = submittedResponse(2L, FORM_ID);
        FormResponse r3 = submittedResponse(3L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L, 3L)))
            .willReturn(List.of(r1, r2, r3));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L, 2L, 3L)))
            .willReturn(List.of(
                scheduleAnswer(r1, q, Set.of(SLOT_A, SLOT_B)),
                scheduleAnswer(r2, q, Set.of(SLOT_B, SLOT_C)),
                scheduleAnswer(r3, q, Set.of(SLOT_A, SLOT_C))
            ));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L, 2L, 3L));

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
        Question q = scheduleQuestion(QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L))).willReturn(List.of(r1));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L)))
            .willReturn(List.of(scheduleAnswer(r1, q, Set.of(SLOT_A))));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).startsAt()).isEqualTo(SLOT_A);
    }

    @Test
    @DisplayName("존재하지 않는 responseId 포함 시 FORM_RESPONSE_NOT_FOUND")
    void 존재하지_않는_응답_예외() {
        Question q = scheduleQuestion(QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L))).willReturn(List.of(r1));

        assertThatThrownBy(() -> sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L, 2L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 form 의 responseId 섞이면 FORM_RESPONSE_NOT_IN_FORM")
    void 다른_form_응답_섞임_예외() {
        Question q = scheduleQuestion(QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        FormResponse rOther = submittedResponse(2L, OTHER_FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L)))
            .willReturn(List.of(r1, rOther));

        assertThatThrownBy(() -> sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L, 2L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_NOT_IN_FORM);
    }

    @Test
    @DisplayName("DRAFT 상태 응답 섞이면 FORM_RESPONSE_NOT_SUBMITTED")
    void DRAFT_응답_섞임_예외() {
        Question q = scheduleQuestion(QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));
        FormResponse submitted = submittedResponse(1L, FORM_ID);
        FormResponse draft = draftResponse(2L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L)))
            .willReturn(List.of(submitted, draft));

        assertThatThrownBy(() -> sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L, 2L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_NOT_SUBMITTED);
    }

    @Test
    @DisplayName("SCHEDULE 이 아닌 answeredAsType 은 무시")
    void 비_SCHEDULE_답변_무시() {
        Question q = scheduleQuestion(QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L))).willReturn(List.of(r1));
        Question textQ = questionOfType(OTHER_QUESTION_ID, QuestionType.SHORT_TEXT);
        Answer textAnswer = Answer.create(r1, textQ, QuestionType.SHORT_TEXT, "답", null, null);
        Answer scheduleAnswer = scheduleAnswer(r1, q, Set.of(SLOT_A));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L)))
            .willReturn(List.of(textAnswer, scheduleAnswer));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).startsAt()).isEqualTo(SLOT_A);
    }

    @Test
    @DisplayName("결과는 startsAt 오름차순 정렬")
    void 결과_오름차순_정렬() {
        Question q = scheduleQuestion(QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));
        FormResponse r1 = submittedResponse(1L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L))).willReturn(List.of(r1));
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L)))
            .willReturn(List.of(scheduleAnswer(r1, q, Set.of(SLOT_C, SLOT_A, SLOT_B))));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L));

        assertThat(result).extracting(ScheduleOverlapSlotInfo::startsAt)
            .containsExactly(SLOT_A, SLOT_B, SLOT_C);
    }

    // ============================================================
    //          questionId 파라미터 검증 + 다중 SCHEDULE 분리 계산
    // ============================================================

    @Test
    @DisplayName("questionId 가 존재하지 않으면 QUESTION_NOT_FOUND")
    void questionId_없음_예외() {
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.QUESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("questionId 가 다른 form 의 질문이면 QUESTION_IS_NOT_OWNED_BY_FORM")
    void questionId_다른_form_예외() {
        Question q = scheduleQuestion(QUESTION_ID, OTHER_FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));

        assertThatThrownBy(() -> sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.QUESTION_IS_NOT_OWNED_BY_FORM);
    }

    @Test
    @DisplayName("questionId 가 SCHEDULE 타입이 아니면 QUESTION_TYPE_MISMATCH")
    void questionId_비_SCHEDULE_예외() {
        Question q = nonScheduleQuestion(QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(q));

        assertThatThrownBy(() -> sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L)))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.QUESTION_TYPE_MISMATCH);
    }

    @Test
    @DisplayName("다중 SCHEDULE 질문 폼: 요청한 questionId 의 답변만 계산에 포함")
    void 다중_SCHEDULE_분리_계산() {
        // 같은 폼에 SCHEDULE 질문 2개 (Q_TARGET, Q_OTHER)
        Question qTarget = scheduleQuestion(QUESTION_ID, FORM_ID);
        Question qOther = scheduleQuestion(OTHER_QUESTION_ID, FORM_ID);
        given(loadQuestionPort.findById(QUESTION_ID)).willReturn(Optional.of(qTarget));

        FormResponse r1 = submittedResponse(1L, FORM_ID);
        FormResponse r2 = submittedResponse(2L, FORM_ID);
        given(loadFormResponsePort.listByIdsWithForm(Set.of(1L, 2L)))
            .willReturn(List.of(r1, r2));

        // r1 은 두 질문 모두 답변, r2 도 두 질문 모두 답변
        // Q_TARGET 답변: r1=[SLOT_A], r2=[SLOT_A, SLOT_B]  → 요청한 것
        // Q_OTHER  답변: r1=[SLOT_C], r2=[SLOT_C]           → 무시되어야 함
        given(loadAnswerPort.listByFormResponseIds(Set.of(1L, 2L)))
            .willReturn(List.of(
                scheduleAnswer(r1, qTarget, Set.of(SLOT_A)),
                scheduleAnswer(r1, qOther, Set.of(SLOT_C)),
                scheduleAnswer(r2, qTarget, Set.of(SLOT_A, SLOT_B)),
                scheduleAnswer(r2, qOther, Set.of(SLOT_C))
            ));

        List<ScheduleOverlapSlotInfo> result = sut.getOverlap(FORM_ID, QUESTION_ID, Set.of(1L, 2L));

        // SLOT_C 는 Q_OTHER 답변이므로 결과에 없어야 함
        assertThat(result).hasSize(2);
        assertThat(result.get(0).startsAt()).isEqualTo(SLOT_A);
        assertThat(result.get(0).availableResponseIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(result.get(1).startsAt()).isEqualTo(SLOT_B);
        assertThat(result.get(1).availableResponseIds()).containsExactlyInAnyOrder(2L);
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

    private Question scheduleQuestion(Long questionId, Long formId) {
        Form form = publishedForm(formId);
        FormSection section = FormSection.create(form, "섹션", null, 1L);
        Question q = Question.create("일정 선택", QuestionType.SCHEDULE, false, 1L);
        q.assignTo(section);
        ReflectionTestUtils.setField(q, "id", questionId);
        return q;
    }

    private Question nonScheduleQuestion(Long questionId, Long formId) {
        Form form = publishedForm(formId);
        FormSection section = FormSection.create(form, "섹션", null, 1L);
        Question q = Question.create("텍스트 답변", QuestionType.SHORT_TEXT, false, 1L);
        q.assignTo(section);
        ReflectionTestUtils.setField(q, "id", questionId);
        return q;
    }

    private Question questionOfType(Long questionId, QuestionType type) {
        Question q = Question.create("질문", type, false, 1L);
        ReflectionTestUtils.setField(q, "id", questionId);
        return q;
    }

    private Answer scheduleAnswer(FormResponse formResponse, Question question, Set<Instant> times) {
        Answer answer = Answer.create(formResponse, question, QuestionType.SCHEDULE, null, null, times);
        ReflectionTestUtils.setField(answer, "id",
            formResponse.getId() * 100L + question.getId());
        return answer;
    }
}
