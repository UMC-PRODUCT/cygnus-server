package com.umc.product.form.adapter.out.persistence;

import static com.umc.product.form.domain.QAnswer.answer;
import static com.umc.product.form.domain.QAnswerChoice.answerChoice;
import static com.umc.product.form.domain.QFormResponse.formResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.form.domain.AnswerChoice;
import com.umc.product.form.domain.QQuestionOption;
import com.umc.product.form.domain.enums.FormResponseStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AnswerChoiceQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 폼의 SUBMITTED 응답 기준, 선택지별 득표수 집계.
     *
     * @return {optionId -> voteCount}. 득표 0인 선택지는 맵에 키가 없다.
     */
    public Map<Long, Long> countVotesByOptionId(Long formId) {
        NumberExpression<Long> voteCount = answerChoice.id.count();

        List<Tuple> rows = queryFactory
            .select(answerChoice.questionOption.id, voteCount)
            .from(answerChoice)
            .join(answerChoice.answer, answer)
            .join(answer.formResponse, formResponse)
            .where(
                formResponse.form.id.eq(formId),
                formResponse.status.eq(FormResponseStatus.SUBMITTED)
            )
            .groupBy(answerChoice.questionOption.id)
            .fetch();

        return rows.stream()
            .collect(Collectors.toMap(
                t -> t.get(answerChoice.questionOption.id),
                t -> t.get(voteCount)
            ));
    }

    /**
     * 특정 폼의 SUBMITTED 응답 기준, 선택지별 투표자 멤버 ID 조회.
     *
     * @return {optionId -> 투표자 memberId 리스트}. 투표자 없는 선택지는 맵에 키가 없다.
     */
    public Map<Long, List<Long>> findSelectedMemberIdsByOptionId(Long formId) {
        List<Tuple> rows = queryFactory
            .select(answerChoice.questionOption.id, formResponse.respondentMemberId)
            .from(answerChoice)
            .join(answerChoice.answer, answer)
            .join(answer.formResponse, formResponse)
            .where(
                formResponse.form.id.eq(formId),
                formResponse.status.eq(FormResponseStatus.SUBMITTED)
            )
            .fetch();

        return rows.stream()
            .collect(Collectors.groupingBy(
                t -> t.get(answerChoice.questionOption.id),
                Collectors.mapping(
                    t -> t.get(formResponse.respondentMemberId),
                    Collectors.toList()
                )
            ));
    }

    /**
     * 여러 답변의 AnswerChoice 를 벌크 조회.
     * 정렬: 선택지(questionOption) orderNo asc — 삭제된 선택지 (questionOption == null) 는 뒤로.
     */
    public List<AnswerChoice> findAllByAnswerIdIn(Set<Long> answerIds) {
        if (answerIds.isEmpty()) {
            return List.of();
        }
        QQuestionOption qo = QQuestionOption.questionOption;
        return queryFactory
            .selectFrom(answerChoice)
            .leftJoin(answerChoice.questionOption, qo)
            .where(answerChoice.answer.id.in(answerIds))
            .orderBy(qo.orderNo.asc().nullsLast())
            .fetch();
    }
}
