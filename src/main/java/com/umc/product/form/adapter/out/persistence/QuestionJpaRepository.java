package com.umc.product.form.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;

public interface QuestionJpaRepository extends JpaRepository<Question, Long> {
    /**
     * 특정 폼에 속한 특정 질문 삭제
     */
    @Modifying
    @Transactional
    @Query("""
                DELETE FROM Question q
                WHERE q.id = :questionId
                  AND q.formSection.id IN (
                      SELECT fs.id FROM FormSection fs
                      WHERE fs.form.id = :formId
                  )
            """)
    int deleteByFormIdAndQuestionId(@Param("formId") Long formId,
                                    @Param("questionId") Long questionId);


    List<Question> findAllByFormSectionIdIn(Set<Long> formSectionIds);

    boolean existsByIdAndFormSection_Form_Id(Long questionId, Long formId);


    /**
     * 특정 폼에서 특정 타입의 첫 번째 질문 조회
     */
    @Query("""
                SELECT q
                FROM Question q
                JOIN q.formSection fs
                JOIN fs.form f
                WHERE f.id = :formId
                  AND q.type = :type
                ORDER BY q.id ASC
            """)
    Optional<Question> findFirstByFormIdAndType(@Param("formId") Long formId, @Param("type") QuestionType type);

    /**
     * 특정 폼의 활성화된 질문을 섹션 순서, 질문 순서대로 조회
     */
    @Query("""
                SELECT q
                FROM Question q
                JOIN q.formSection fs
                JOIN fs.form f
                WHERE f.id = :formId
                  AND q.isActive = true
                ORDER BY fs.orderNo ASC, q.orderNo ASC
            """)
    List<Question> findAllByFormIdAndIsActiveTrue(@Param("formId") Long formId);

    /**
     * 특정 폼에 속한 모든 질문 삭제 (deleteForm cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM Question q
            WHERE q.formSection.id IN (
                SELECT fs.id FROM FormSection fs WHERE fs.form.id = :formId
            )
        """)
    int deleteByFormId(@Param("formId") Long formId);

    /**
     * 특정 섹션에 속한 모든 질문 삭제 (deleteSection cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Question q WHERE q.formSection.id = :sectionId")
    int deleteBySectionId(@Param("sectionId") Long sectionId);
}
