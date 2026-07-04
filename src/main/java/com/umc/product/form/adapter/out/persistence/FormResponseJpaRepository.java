package com.umc.product.form.adapter.out.persistence;

import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.enums.FormResponseStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FormResponseJpaRepository extends JpaRepository<FormResponse, Long> {
    Optional<FormResponse> findFirstByForm_IdAndRespondentMemberIdAndStatusOrderByIdDesc(
        Long formId,
        Long respondentMemberId,
        FormResponseStatus status
    );

    List<FormResponse> findByRespondentMemberIdAndStatus(
        Long respondentMemberId,
        FormResponseStatus status
    );

    /**
     * 특정 폼의 특정 상태 응답을 일괄 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM FormResponse fr
            WHERE fr.form.id = :formId
              AND fr.status = :status
        """)
    int deleteByFormIdAndStatus(
        @Param("formId") Long formId,
        @Param("status") FormResponseStatus status
    );

    /**
     * 특정 폼의 특정 상태 응답 ID 목록 조회
     */
    @Query("""
            SELECT fr.id
            FROM FormResponse fr
            WHERE fr.form.id = :formId
              AND fr.status = :status
        """)
    List<Long> findIdsByFormIdAndStatus(
        @Param("formId") Long formId,
        @Param("status") FormResponseStatus status
    );

    boolean existsByForm_IdAndRespondentMemberId(Long formId, Long respondentMemberId);

    /**
     * 특정 폼의 특정 상태 응답 수 조회
     */
    @Query("""
            SELECT count(fr)
            FROM FormResponse fr
            WHERE fr.form.id = :formId
              AND fr.status = :status
        """)
    long countByFormIdAndStatus(
        @Param("formId") Long formId,
        @Param("status") FormResponseStatus status
    );

    /**
     * 특정 폼의 모든 응답 삭제 (deleteForm cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FormResponse fr WHERE fr.form.id = :formId")
    int deleteByFormId(@Param("formId") Long formId);
}
