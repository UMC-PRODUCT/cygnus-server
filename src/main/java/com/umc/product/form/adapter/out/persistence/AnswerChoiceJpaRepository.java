package com.umc.product.form.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.form.domain.AnswerChoice;

public interface AnswerChoiceJpaRepository extends JpaRepository<AnswerChoice, Long> {

    /**
     * 특정 폼에서 특정 멤버가 선택한 선택지 ID 목록 조회 (SUBMITTED 응답 기준)
     */
    @Query("""
            SELECT ac.questionOption.id
            FROM AnswerChoice ac
            JOIN ac.answer a
            JOIN a.formResponse fr
            WHERE fr.form.id = :formId
              AND fr.respondentMemberId = :memberId
              AND fr.status = 'SUBMITTED'
        """)
    List<Long> findSelectedOptionIdsByMember(@Param("formId") Long formId, @Param("memberId") Long memberId);

    /**
     * 특정 FormResponse 에 속한 모든 Answer 의 AnswerChoice 를 일괄 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM AnswerChoice ac
            WHERE ac.answer.id IN (
                SELECT a.id FROM Answer a WHERE a.formResponse.id = :formResponseId
            )
        """)
    int deleteAllByFormResponseId(@Param("formResponseId") Long formResponseId);

    /**
     * 특정 폼에 속한 모든 AnswerChoice 삭제 (deleteForm cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM AnswerChoice ac
            WHERE ac.answer.id IN (
                SELECT a.id FROM Answer a
                JOIN a.formResponse fr
                WHERE fr.form.id = :formId
            )
        """)
    int deleteByFormId(@Param("formId") Long formId);

    /**
     * 특정 질문에 속한 모든 AnswerChoice 삭제 (deleteQuestion cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM AnswerChoice ac
            WHERE ac.answer.id IN (
                SELECT a.id FROM Answer a WHERE a.question.id = :questionId
            )
        """)
    int deleteByQuestionId(@Param("questionId") Long questionId);

    /**
     * 단일 Answer 의 모든 AnswerChoice 삭제 (deleteAnswer / updateAnswer 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AnswerChoice ac WHERE ac.answer.id = :answerId")
    int deleteByAnswerId(@Param("answerId") Long answerId);
}
