package com.umc.product.form.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.form.domain.Answer;

public interface AnswerJpaRepository extends JpaRepository<Answer, Long> {

    /**
     * 특정 FormResponse에 속한 모든 Answer 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM Answer a
            WHERE a.formResponse.id = :formResponseId
        """)
    int deleteAllByFormResponseId(@Param("formResponseId") Long formResponseId);

    /**
     * 특정 폼에 속한 모든 Answer 삭제 (deleteForm cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM Answer a
            WHERE a.formResponse.id IN (
                SELECT fr.id FROM FormResponse fr WHERE fr.form.id = :formId
            )
        """)
    int deleteByFormId(@Param("formId") Long formId);

    /**
     * 특정 질문에 속한 모든 Answer 삭제 (deleteQuestion cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Answer a WHERE a.question.id = :questionId")
    int deleteByQuestionId(@Param("questionId") Long questionId);
}
