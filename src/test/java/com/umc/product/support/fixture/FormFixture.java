package com.umc.product.support.fixture;

import java.time.Instant;
import java.util.Set;

import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;

public final class FormFixture {

    private FormFixture() {
    }

    public static Form publishedForm() {
        return Form.createPublished(1L, "테스트 폼", false);
    }

    public static FormSection section(Form form, long orderNo) {
        return FormSection.create(form, "섹션 " + orderNo, null, orderNo);
    }

    public static Question question(FormSection section, QuestionType type, long orderNo) {
        Question question = Question.create("질문 " + orderNo, type, true, orderNo);
        question.assignTo(section);
        return question;
    }

    public static QuestionOption option(Question question, String content, long orderNo) {
        QuestionOption option = QuestionOption.create(content, orderNo, false);
        option.assignTo(question);
        return option;
    }

    public static FormResponse draft(Form form, long memberId) {
        return FormResponse.createDraft(form, memberId);
    }

    public static FormResponse submitted(Form form, long memberId) {
        FormResponse response = draft(form, memberId);
        response.submit(Instant.parse("2026-01-01T00:00:00Z"), "127.0.0.1");
        return response;
    }

    public static Answer answer(FormResponse response, Question question, String text) {
        return Answer.create(response, question, question.getType(), text, Set.of());
    }

    public static AnswerChoice choice(Answer answer, QuestionOption option) {
        return AnswerChoice.create(answer, option);
    }
}
