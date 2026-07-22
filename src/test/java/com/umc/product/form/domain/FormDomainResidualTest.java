package com.umc.product.form.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.form.domain.enums.FormOpenStatus;
import com.umc.product.form.domain.enums.FormResponseStatus;
import com.umc.product.form.domain.enums.FormStatus;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

@DisplayName("Form domain 경계와 상태 전이")
class FormDomainResidualTest {

    @Test
    @DisplayName("draft·published factory는 설명·익명·중복 응답 기본값과 명시값을 구분한다")
    void form_factories_preserve_defaults_and_explicit_values() {
        Form draftDefault = Form.createDraft("초안", 1L);
        Form draftDuplicate = Form.createDraft("초안2", 2L, true);
        Form draftDetailed = Form.createDraft("초안3", 3L, "설명", true);
        Form publishedDefault = Form.createPublished(4L, "발행", true);
        Form publishedDuplicate = Form.createPublished(5L, "발행2", false, true);

        assertThat(draftDefault.getStatus()).isEqualTo(FormStatus.DRAFT);
        assertThat(draftDefault.isAllowDuplicateResponses()).isFalse();
        assertThat(draftDuplicate.isAllowDuplicateResponses()).isTrue();
        assertThat(draftDetailed.getDescription()).isEqualTo("설명");
        assertThat(publishedDefault.isPublished()).isTrue();
        assertThat(publishedDefault.isAnonymous()).isTrue();
        assertThat(publishedDefault.isAllowDuplicateResponses()).isFalse();
        assertThat(publishedDuplicate.isAllowDuplicateResponses()).isTrue();
    }

    @Test
    @DisplayName("폼은 DRAFT→PUBLISHED→DRAFT 및 PUBLISHED→CLOSED 전이만 허용한다")
    void form_transition_matrix() {
        Form form = Form.createDraft("폼", 1L);
        form.publish();
        assertThat(form.getStatus()).isEqualTo(FormStatus.PUBLISHED);
        assertError(form::publish, FormErrorCode.FORM_ALREADY_PUBLISHED);
        form.unpublish();
        assertThat(form.getStatus()).isEqualTo(FormStatus.DRAFT);
        assertError(form::unpublish, FormErrorCode.FORM_INVALID_TRANSITION);
        assertError(form::close, FormErrorCode.FORM_INVALID_TRANSITION);

        form.publish();
        form.close();
        assertThat(form.getStatus()).isEqualTo(FormStatus.CLOSED);
        assertError(form::publish, FormErrorCode.FORM_INVALID_TRANSITION);
    }

    @Test
    @DisplayName("폼 PATCH는 null 유지·빈 값 저장·description 명시 삭제를 구분한다")
    void form_patch_semantics() {
        Form form = Form.createDraft("제목", 1L, "설명", false);

        form.update(null, null, null);
        assertThat(form.getTitle()).isEqualTo("제목");
        form.update("변경", "", true, true);
        assertThat(form.getTitle()).isEqualTo("변경");
        assertThat(form.getDescription()).isEmpty();
        assertThat(form.isAnonymous()).isTrue();
        assertThat(form.isAllowDuplicateResponses()).isTrue();
        form.update(null, "무시", null, null, true);
        assertThat(form.getDescription()).isNull();
    }

    @Test
    @DisplayName("기명·익명 응답 draft와 제출은 식별 정보·시간·멱등성을 보존한다")
    void form_response_draft_submit_and_idempotency() {
        Form form = Form.createPublished(1L, "폼", false);
        FormResponse named = FormResponse.createDraft(form, 2L);
        FormResponse anonymous = FormResponse.createAnonymousDraft(form, "hash");
        Instant submittedAt = Instant.parse("2030-01-01T00:00:00Z");

        assertThat(named.getRespondentMemberId()).isEqualTo(2L);
        assertThat(named.getLastSavedAt()).isNotNull();
        assertThat(anonymous.getRespondentMemberId()).isNull();
        assertThat(anonymous.getResponseAccessKeyHash()).isEqualTo("hash");
        named.submit(submittedAt, "127.0.0.1");
        named.submit(submittedAt.plusSeconds(1), "변경 금지");

        assertThat(named.getStatus()).isEqualTo(FormResponseStatus.SUBMITTED);
        assertThat(named.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(named.getSubmittedIp()).isEqualTo("127.0.0.1");
        Instant savedAt = submittedAt.plusSeconds(2);
        named.updateLastSavedAt(savedAt);
        assertThat(named.getLastSavedAt()).isEqualTo(savedAt);
    }

    @Test
    @DisplayName("section PATCH와 순서 변경은 null 유지·빈 값·명시 삭제를 구분한다")
    void form_section_patch_semantics() {
        FormSection section = FormSection.create(Form.createDraft("폼", 1L), "섹션", "설명", 1L);

        section.update(null, null);
        section.update("변경", "");
        assertThat(section.getTitle()).isEqualTo("변경");
        assertThat(section.getDescription()).isEmpty();
        section.update(null, "무시", true);
        section.updateOrderNo(2L);

        assertThat(section.getDescription()).isNull();
        assertThat(section.getOrderNo()).isEqualTo(2L);
    }

    @Test
    @DisplayName("질문 생성·fork·PATCH·비활성화는 원본과 독립된 스냅샷을 만든다")
    void question_lifecycle_and_fork_snapshot() {
        Question simple = Question.create("질문", QuestionType.SHORT_TEXT, true, 1L);
        Question origin = Question.create("원본", "설명", QuestionType.RADIO, false, 2L);
        ReflectionTestUtils.setField(origin, "id", 10L);
        FormSection section = FormSection.create(Form.createDraft("폼", 1L), "섹션", null, 1L);
        origin.assignTo(section);
        Question forked = Question.fork(origin);
        origin.deactivate();

        assertThat(simple.getDescription()).isNull();
        assertThat(origin.isActive()).isFalse();
        assertThat(forked.getParentQuestionId()).isEqualTo(10L);
        assertThat(forked.getFormSection()).isNull();
        assertThat(forked.isActive()).isTrue();
        forked.update(null, null, null);
        forked.update("변경", "", true);
        assertThat(forked.getDescription()).isEmpty();
        forked.update(null, "무시", null, true);
        forked.changeType(QuestionType.CHECKBOX);
        forked.updateOrderNo(3L);
        assertThat(forked.getDescription()).isNull();
        assertThat(forked.getType()).isEqualTo(QuestionType.CHECKBOX);
        assertThat(forked.getOrderNo()).isEqualTo(3L);
    }

    @Test
    @DisplayName("선택지 생성·PATCH·분기 대상 삭제·질문 할당·순서 변경을 지원한다")
    void question_option_lifecycle() {
        QuestionOption basic = QuestionOption.create("보기", 1L, false);
        QuestionOption branching = QuestionOption.create("기타", 2L, true, 9L);
        Question question = Question.create("질문", QuestionType.RADIO, true, 1L);

        assertThat(basic.getNextSectionId()).isNull();
        branching.assignTo(question);
        branching.update(null, null);
        branching.update("변경", false, 10L, false);
        assertThat(branching.getContent()).isEqualTo("변경");
        assertThat(branching.isOther()).isFalse();
        assertThat(branching.getNextSectionId()).isEqualTo(10L);
        branching.update(null, null, 11L, true);
        branching.updateOrderNo(3L);
        assertThat(branching.getNextSectionId()).isNull();
        assertThat(branching.getQuestion()).isSameAs(question);
        assertThat(branching.getOrderNo()).isEqualTo(3L);
    }

    @Test
    @DisplayName("답변 PATCH는 null 유지·blank/empty 삭제·값 교체를 구분하고 선택지 문구를 snapshot한다")
    void answer_and_choice_semantics() {
        FormResponse response = FormResponse.createDraft(Form.createDraft("폼", 1L), 2L);
        Question question = Question.create("질문", QuestionType.FILE, false, 1L);
        Answer answer = Answer.create(response, question, QuestionType.FILE, "기존", Set.of("file-1"));

        answer.update(null, null);
        answer.update(" ", Set.of());
        assertThat(answer.getTextValue()).isNull();
        assertThat(answer.getFileIds()).isNull();
        answer.update("변경", Set.of("file-2"));
        assertThat(answer.getTextValue()).isEqualTo("변경");
        assertThat(answer.getFileIds()).containsExactly("file-2");

        Answer empty = Answer.createEmpty(response, question);
        QuestionOption option = QuestionOption.create("선택 당시", 1L, false);
        AnswerChoice choice = AnswerChoice.create(empty, option);
        AnswerChoice direct = new AnswerChoice(empty, option);
        option.update("나중 변경", null);

        assertThat(empty.getAnsweredAsType()).isEqualTo(QuestionType.FILE);
        assertThat(choice.getAnsweredAsContent()).isEqualTo("선택 당시");
        assertThat(direct.getQuestionOption()).isSameAs(option);
    }

    @Test
    @DisplayName("enum과 모든 오류 코드는 안정적인 외부 오류 계약을 가진다")
    void enums_and_error_contract() {
        assertThat(FormOpenStatus.values()).hasSize(3);
        assertThat(FormResponseStatus.values()).hasSize(2);
        assertThat(FormStatus.values()).hasSize(3);
        assertThat(QuestionType.values()).hasSize(8);
        assertThat(FormErrorCode.values()).allSatisfy(code -> {
            assertThat(code.getHttpStatus()).isNotNull();
            assertThat(code.getCode()).startsWith("FORM-");
            assertThat(code.getMessage()).isNotBlank();
            assertThat(new FormDomainException(code).getBaseCode()).isEqualTo(code);
            assertThat(new FormDomainException(code, "상세").getMessage()).contains("상세");
        });
    }

    private void assertError(Runnable action, FormErrorCode code) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(FormDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(code)
            );
    }
}
