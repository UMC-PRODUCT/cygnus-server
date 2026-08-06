package com.umc.product.form.adapter.out.persistence;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.form.domain.QQuestionOption;
import com.umc.product.form.domain.QuestionOption;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QuestionOptionQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 질문에 속한 모든 선택지를 orderNo 오름차순으로 조회.
     */
    public List<QuestionOption> findAllByQuestionId(Long questionId) {
        QQuestionOption o = QQuestionOption.questionOption;
        return queryFactory
            .selectFrom(o)
            .where(o.question.id.eq(questionId))
            .orderBy(o.orderNo.asc())
            .fetch();
    }

    /**
     * 여러 질문의 모든 선택지를 벌크 조회. orderNo 오름차순 정렬.
     */
    public List<QuestionOption> findAllByQuestionIdIn(Set<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }
        QQuestionOption o = QQuestionOption.questionOption;
        return queryFactory
            .selectFrom(o)
            .where(o.question.id.in(questionIds))
            .orderBy(o.orderNo.asc())
            .fetch();
    }
}
