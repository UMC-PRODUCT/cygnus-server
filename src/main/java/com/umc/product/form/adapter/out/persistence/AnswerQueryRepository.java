package com.umc.product.form.adapter.out.persistence;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.form.domain.Answer;
import com.umc.product.form.domain.QAnswer;
import com.umc.product.form.domain.QFormResponse;
import com.umc.product.form.domain.QFormSection;
import com.umc.product.form.domain.QQuestion;
import com.umc.product.form.domain.enums.QuestionType;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AnswerQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 FormResponse 의 모든 답변을 조회.
     * 정렬: 섹션 orderNo asc -> 질문 orderNo asc (사용자가 본 화면 순서).
     */
    public List<Answer> findAllByFormResponseId(Long formResponseId) {
        QAnswer a = QAnswer.answer;
        QQuestion q = QQuestion.question;
        QFormSection s = QFormSection.formSection;
        return queryFactory
            .selectFrom(a)
            .join(a.question, q)
            .join(q.formSection, s)
            .where(a.formResponse.id.eq(formResponseId))
            .orderBy(s.orderNo.asc(), q.orderNo.asc())
            .fetch();
    }

    /**
     * 여러 FormResponse 의 모든 답변을 한 번에 조회한다.
     */
    public List<Answer> findAllByFormResponseIdIn(Set<Long> formResponseIds) {
        if (formResponseIds == null || formResponseIds.isEmpty()) {
            return List.of();
        }

        QAnswer a = QAnswer.answer;
        QQuestion q = QQuestion.question;
        QFormSection s = QFormSection.formSection;
        QFormResponse fr = QFormResponse.formResponse;
        return queryFactory
            .selectFrom(a)
            .join(a.formResponse, fr).fetchJoin()
            .join(a.question, q).fetchJoin()
            .join(q.formSection, s)
            .where(a.formResponse.id.in(formResponseIds))
            .orderBy(a.formResponse.id.asc(), s.orderNo.asc(), q.orderNo.asc())
            .fetch();
    }

    public List<Long> findAttachmentIdsByFormResponseId(Long formResponseId) {
        QAnswer answer = QAnswer.answer;
        return queryFactory
            .select(answer.id)
            .from(answer)
            .where(
                answer.formResponse.id.eq(formResponseId),
                answer.answeredAsType.in(QuestionType.FILE, QuestionType.PORTFOLIO)
            )
            .orderBy(answer.id.asc())
            .fetch();
    }

    public List<Long> findAttachmentIdsByFormId(Long formId) {
        QAnswer answer = QAnswer.answer;
        return queryFactory
            .select(answer.id)
            .from(answer)
            .where(
                answer.formResponse.form.id.eq(formId),
                answer.answeredAsType.in(QuestionType.FILE, QuestionType.PORTFOLIO)
            )
            .orderBy(answer.id.asc())
            .fetch();
    }

    public List<Long> findAttachmentIdsByQuestionId(Long questionId) {
        QAnswer answer = QAnswer.answer;
        return queryFactory
            .select(answer.id)
            .from(answer)
            .where(
                answer.question.id.eq(questionId),
                answer.answeredAsType.in(QuestionType.FILE, QuestionType.PORTFOLIO)
            )
            .orderBy(answer.id.asc())
            .fetch();
    }

    /**
     * 특정 FormResponse 에 특정 Question의 답변이 이미 있는지 검사 (createAnswer 중복 검증 용).
     */
    public boolean existsByFormResponseIdAndQuestionId(Long formResponseId, Long questionId) {
        QAnswer a = QAnswer.answer;
        Integer one = queryFactory
            .selectOne()
            .from(a)
            .where(
                a.formResponse.id.eq(formResponseId),
                a.question.id.eq(questionId)
            )
            .fetchFirst();
        return one != null;
    }
}
