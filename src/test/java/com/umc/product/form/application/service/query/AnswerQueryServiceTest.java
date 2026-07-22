package com.umc.product.form.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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

import com.umc.product.authentication.application.service.SecureTokenGenerator;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.application.port.out.LoadAnswerPort;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@ExtendWith(MockitoExtension.class)
class AnswerQueryServiceTest {

    private static final Long FORM_ID = 100L;
    private static final Long FORM_RESPONSE_ID = 200L;
    private static final Long OWNER_MEMBER_ID = 300L;
    private static final Long QUESTION_ID = 400L;
    private static final Long ANSWER_ID = 500L;

    @Mock
    LoadAnswerPort loadAnswerPort;
    @Mock
    SecureTokenGenerator secureTokenGenerator;

    @InjectMocks
    AnswerQueryService sut;

    private static final String RAW_KEY = "raw-key";
    private static final String KEY_HASH = "hash-value";
    private static final String OTHER_HASH = "other-hash";

    @Test
    @DisplayName("findById: 익명 응답의 답변이면 Optional.empty")
    void findById_익명_응답의_답변이면_empty() {
        Answer answer = shortTextAnswer(anonymousDraft());
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        assertThat(sut.findById(ANSWER_ID)).isEmpty();
    }

    @Test
    @DisplayName("findById: 기명 응답의 답변이면 반환")
    void findById_기명_응답의_답변이면_반환() {
        Answer answer = shortTextAnswer(namedDraft(OWNER_MEMBER_ID));
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(Set.of(ANSWER_ID))).willReturn(List.of());

        assertThat(sut.findById(ANSWER_ID)).isPresent();
    }

    @Test
    @DisplayName("getById: 익명 응답의 답변이면 ANSWER_NOT_FOUND")
    void getById_익명_응답의_답변이면_NOT_FOUND() {
        Answer answer = shortTextAnswer(anonymousDraft());
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> sut.getById(ANSWER_ID))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.ANSWER_NOT_FOUND);
    }

    @Test
    @DisplayName("getById: 기명 답변은 choice와 함께 반환")
    void getById_기명_답변_반환() {
        Answer answer = shortTextAnswer(namedDraft(OWNER_MEMBER_ID));
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(Set.of(ANSWER_ID))).willReturn(List.of());

        assertThat(sut.getById(ANSWER_ID).id()).isEqualTo(ANSWER_ID);
    }

    @Test
    @DisplayName("listByFormResponseId: 익명 응답이면 빈 리스트")
    void listByFormResponseId_익명_응답이면_빈_리스트() {
        Answer answer = shortTextAnswer(anonymousDraft());
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(answer));

        assertThat(sut.listByFormResponseId(FORM_RESPONSE_ID)).isEmpty();
    }

    @Test
    @DisplayName("listByFormResponseId: 응답 자체가 없으면 빈 리스트")
    void listByFormResponseId_응답_없으면_빈_리스트() {
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of());

        assertThat(sut.listByFormResponseId(FORM_RESPONSE_ID)).isEmpty();
    }

    @Test
    @DisplayName("listByFormResponseId: 기명 답변은 choice를 bulk 조회해 조립")
    void listByFormResponseId_기명_답변_choice_조립() {
        Answer answer = shortTextAnswer(namedDraft(OWNER_MEMBER_ID));
        QuestionOption option = QuestionOption.create("선택", 1L, false);
        ReflectionTestUtils.setField(option, "id", 600L);
        AnswerChoice choice = AnswerChoice.create(answer, option);
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(answer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(Set.of(ANSWER_ID))).willReturn(List.of(choice));

        assertThat(sut.listByFormResponseId(FORM_RESPONSE_ID).get(0).selectedOptions())
            .extracting(AnswerInfo.SelectedOption::questionOptionId)
            .containsExactly(600L);
    }

    @Test
    @DisplayName("listByFormResponseIds: null·empty와 익명 전용 결과는 빈 map")
    void listByFormResponseIds_빈_입력과_익명만_있으면_빈_map() {
        assertThat(sut.listByFormResponseIds(null)).isEmpty();
        assertThat(sut.listByFormResponseIds(Set.of())).isEmpty();
        given(loadAnswerPort.listByFormResponseIds(Set.of(FORM_RESPONSE_ID)))
            .willReturn(List.of(shortTextAnswer(anonymousDraft())));

        assertThat(sut.listByFormResponseIds(Set.of(FORM_RESPONSE_ID))).isEmpty();
    }

    @Test
    @DisplayName("삭제된 option의 choice는 snapshot content와 null ID로 반환")
    void selected_option_preserves_snapshot_after_option_deletion() {
        Answer answer = shortTextAnswer(namedDraft(OWNER_MEMBER_ID));
        QuestionOption option = QuestionOption.create("삭제 전", 1L, false);
        AnswerChoice choice = AnswerChoice.create(answer, option);
        ReflectionTestUtils.setField(choice, "questionOption", null);

        AnswerInfo.SelectedOption result = AnswerInfo.SelectedOption.from(choice);

        assertThat(result.questionOptionId()).isNull();
        assertThat(result.answeredAsContent()).isEqualTo("삭제 전");
    }

    @Test
    @DisplayName("listByFormResponseIds: 익명 응답의 답변은 결과 map 에서 제외")
    void listByFormResponseIds_익명_응답은_제외() {
        Answer namedAnswer = shortTextAnswer(namedDraft(OWNER_MEMBER_ID));
        Answer anonymousAnswer = shortTextAnswerWithId(anonymousDraft(), 501L);
        given(loadAnswerPort.listByFormResponseIds(Set.of(FORM_RESPONSE_ID, 201L)))
            .willReturn(List.of(namedAnswer, anonymousAnswer));
        given(loadAnswerPort.listChoicesByAnswerIdIn(any())).willReturn(List.of());

        var result = sut.listByFormResponseIds(Set.of(FORM_RESPONSE_ID, 201L));

        assertThat(result).containsOnlyKeys(FORM_RESPONSE_ID);
    }

    // ============================================================
    //          findByIdAsAnonymous / getByIdAsAnonymous — 익명 access key 검증
    // ============================================================

    @Test
    @DisplayName("findByIdAsAnonymous: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void findByIdAsAnonymous_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.findByIdAsAnonymous(ANSWER_ID, null))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
    }

    @Test
    @DisplayName("findByIdAsAnonymous: 답변 없으면 Optional.empty")
    void findByIdAsAnonymous_답변_없으면_empty() {
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.empty());

        assertThat(sut.findByIdAsAnonymous(ANSWER_ID, RAW_KEY)).isEmpty();
    }

    @Test
    @DisplayName("findByIdAsAnonymous: 기명 응답의 답변이면 Optional.empty (silently)")
    void findByIdAsAnonymous_기명_응답이면_empty() {
        Answer answer = shortTextAnswer(namedDraft(OWNER_MEMBER_ID));
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        assertThat(sut.findByIdAsAnonymous(ANSWER_ID, RAW_KEY)).isEmpty();
    }

    @Test
    @DisplayName("findByIdAsAnonymous: hash 불일치면 Optional.empty (silently)")
    void findByIdAsAnonymous_hash_불일치_empty() {
        Answer answer = shortTextAnswer(anonymousDraft());
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(OTHER_HASH);

        assertThat(sut.findByIdAsAnonymous(ANSWER_ID, RAW_KEY)).isEmpty();
    }

    @Test
    @DisplayName("findByIdAsAnonymous: hash 일치하는 익명 응답이면 반환")
    void findByIdAsAnonymous_hash_일치_반환() {
        Answer answer = shortTextAnswer(anonymousDraft());
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(KEY_HASH);
        given(loadAnswerPort.listChoicesByAnswerIdIn(Set.of(ANSWER_ID))).willReturn(List.of());

        assertThat(sut.findByIdAsAnonymous(ANSWER_ID, RAW_KEY)).isPresent();
    }

    @Test
    @DisplayName("getByIdAsAnonymous: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void getByIdAsAnonymous_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.getByIdAsAnonymous(ANSWER_ID, null))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
    }

    @Test
    @DisplayName("getByIdAsAnonymous: 답변 없으면 FORM_RESPONSE_FORBIDDEN")
    void getByIdAsAnonymous_답변_없으면_FORBIDDEN() {
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getByIdAsAnonymous(ANSWER_ID, RAW_KEY))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
    }

    @Test
    @DisplayName("getByIdAsAnonymous: 기명 응답의 답변이면 FORM_RESPONSE_FORBIDDEN")
    void getByIdAsAnonymous_기명_응답_FORBIDDEN() {
        Answer answer = shortTextAnswer(namedDraft(OWNER_MEMBER_ID));
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));

        assertThatThrownBy(() -> sut.getByIdAsAnonymous(ANSWER_ID, RAW_KEY))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
    }

    @Test
    @DisplayName("getByIdAsAnonymous: hash 불일치면 FORM_RESPONSE_FORBIDDEN")
    void getByIdAsAnonymous_hash_불일치_FORBIDDEN() {
        Answer answer = shortTextAnswer(anonymousDraft());
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(OTHER_HASH);

        assertThatThrownBy(() -> sut.getByIdAsAnonymous(ANSWER_ID, RAW_KEY))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
    }

    @Test
    @DisplayName("getByIdAsAnonymous: hash 일치하는 익명 응답이면 반환")
    void getByIdAsAnonymous_hash_일치_반환() {
        Answer answer = shortTextAnswer(anonymousDraft());
        given(loadAnswerPort.findById(ANSWER_ID)).willReturn(Optional.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(KEY_HASH);
        given(loadAnswerPort.listChoicesByAnswerIdIn(Set.of(ANSWER_ID))).willReturn(List.of());

        assertThat(sut.getByIdAsAnonymous(ANSWER_ID, RAW_KEY)).isNotNull();
    }

    // ============================================================
    //          listByFormResponseIdAsAnonymous — 익명 access key 검증
    // ============================================================

    @Test
    @DisplayName("listByFormResponseIdAsAnonymous: rawKey null 이면 RESPONSE_ACCESS_KEY_REQUIRED")
    void listByFormResponseIdAsAnonymous_rawKey_null_예외() {
        assertThatThrownBy(() -> sut.listByFormResponseIdAsAnonymous(FORM_RESPONSE_ID, null))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.RESPONSE_ACCESS_KEY_REQUIRED);
    }

    @Test
    @DisplayName("listByFormResponseIdAsAnonymous: 응답에 답변 없으면 빈 리스트 (정보 유출 없음)")
    void listByFormResponseIdAsAnonymous_답변_없으면_빈_리스트() {
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of());

        assertThat(sut.listByFormResponseIdAsAnonymous(FORM_RESPONSE_ID, RAW_KEY)).isEmpty();
    }

    @Test
    @DisplayName("listByFormResponseIdAsAnonymous: 기명 응답이면 FORM_RESPONSE_FORBIDDEN")
    void listByFormResponseIdAsAnonymous_기명_응답_FORBIDDEN() {
        Answer answer = shortTextAnswer(namedDraft(OWNER_MEMBER_ID));
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(answer));

        assertThatThrownBy(() -> sut.listByFormResponseIdAsAnonymous(FORM_RESPONSE_ID, RAW_KEY))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
    }

    @Test
    @DisplayName("listByFormResponseIdAsAnonymous: hash 불일치면 FORM_RESPONSE_FORBIDDEN")
    void listByFormResponseIdAsAnonymous_hash_불일치_FORBIDDEN() {
        Answer answer = shortTextAnswer(anonymousDraft());
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(OTHER_HASH);

        assertThatThrownBy(() -> sut.listByFormResponseIdAsAnonymous(FORM_RESPONSE_ID, RAW_KEY))
            .isInstanceOf(FormDomainException.class)
            .extracting("baseCode")
            .isEqualTo(FormErrorCode.FORM_RESPONSE_FORBIDDEN);
    }

    @Test
    @DisplayName("listByFormResponseIdAsAnonymous: hash 일치하는 익명 응답이면 답변 반환")
    void listByFormResponseIdAsAnonymous_hash_일치_반환() {
        Answer answer = shortTextAnswer(anonymousDraft());
        given(loadAnswerPort.listByFormResponseId(FORM_RESPONSE_ID)).willReturn(List.of(answer));
        given(secureTokenGenerator.sha256Hex(RAW_KEY)).willReturn(KEY_HASH);
        given(loadAnswerPort.listChoicesByAnswerIdIn(Set.of(ANSWER_ID))).willReturn(List.of());

        assertThat(sut.listByFormResponseIdAsAnonymous(FORM_RESPONSE_ID, RAW_KEY)).hasSize(1);
    }

    private FormResponse namedDraft(Long memberId) {
        Form form = publishedForm();
        FormResponse draft = FormResponse.createDraft(form, memberId);
        ReflectionTestUtils.setField(draft, "id", FORM_RESPONSE_ID);
        return draft;
    }

    private FormResponse anonymousDraft() {
        Form form = publishedForm();
        FormResponse draft = FormResponse.createAnonymousDraft(form, "hash-value");
        ReflectionTestUtils.setField(draft, "id", FORM_RESPONSE_ID);
        return draft;
    }

    private Form publishedForm() {
        Form form = Form.createDraft("폼", 1L, false);
        ReflectionTestUtils.setField(form, "id", FORM_ID);
        form.publish();
        return form;
    }

    private Answer shortTextAnswer(FormResponse formResponse) {
        return shortTextAnswerWithId(formResponse, ANSWER_ID);
    }

    private Answer shortTextAnswerWithId(FormResponse formResponse, Long id) {
        Question question = Question.create("질문", QuestionType.SHORT_TEXT, false, 1L);
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);
        Answer answer = Answer.create(formResponse, question, QuestionType.SHORT_TEXT, "답", null);
        ReflectionTestUtils.setField(answer, "id", id);
        return answer;
    }
}
