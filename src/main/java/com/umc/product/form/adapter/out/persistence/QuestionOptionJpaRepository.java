package com.umc.product.form.adapter.out.persistence;

import com.umc.product.form.domain.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface QuestionOptionJpaRepository extends JpaRepository<QuestionOption, Long> {

    /**
     * 특정 질문에 속한 모든 선택지 삭제
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM QuestionOption o WHERE o.question.id = :questionId")
    int deleteAllByQuestionId(@Param("questionId") Long questionId);

    /**
     * 특정 폼에 속한 모든 선택지 삭제 (deleteForm cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM QuestionOption qo
            WHERE qo.question.id IN (
                SELECT q.id FROM Question q
                JOIN q.formSection fs
                WHERE fs.form.id = :formId
            )
        """)
    int deleteByFormId(@Param("formId") Long formId);

    /**
     * 특정 섹션에 속한 모든 선택지 삭제 (deleteSection cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM QuestionOption qo
            WHERE qo.question.id IN (
                SELECT q.id FROM Question q WHERE q.formSection.id = :sectionId
            )
        """)
    int deleteBySectionId(@Param("sectionId") Long sectionId);

    boolean existsByIdAndQuestion_Id(Long optionId, Long questionId);
}
