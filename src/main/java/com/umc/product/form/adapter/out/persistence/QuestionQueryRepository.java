package com.umc.product.form.adapter.out.persistence;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.form.domain.QQuestion;
import com.umc.product.form.domain.Question;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QuestionQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 섹션에 속한 Active 질문을 orderNo 오름차순으로 조회.
     */
    public List<Question> findAllBySectionId(Long sectionId) {
        QQuestion q = QQuestion.question;
        return queryFactory
            .selectFrom(q)
            .where(q.formSection.id.eq(sectionId)
                .and(q.isActive.isTrue()))
            .orderBy(q.orderNo.asc())
            .fetch();
    }

    /**
     * 여러 섹션의 Active 질문을 벌크 조회. orderNo 오름차순 정렬.
     */
    public List<Question> findAllBySectionIdIn(Set<Long> sectionIds) {
        if (sectionIds.isEmpty()) {
            return List.of();
        }
        QQuestion q = QQuestion.question;
        return queryFactory
            .selectFrom(q)
            .where(q.formSection.id.in(sectionIds)
                .and(q.isActive.isTrue()))
            .orderBy(q.orderNo.asc())
            .fetch();
    }

    /**
     * questionId 목록으로 질문을 조회한다 (isActive 무관). orderNo 오름차순 정렬.
     */
    public List<Question> findAllByIdIn(Set<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }
        QQuestion q = QQuestion.question;
        return queryFactory
            .selectFrom(q)
            .where(q.id.in(questionIds))
            .orderBy(q.orderNo.asc())
            .fetch();
    }
}
