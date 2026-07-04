package com.umc.product.form.adapter.out.persistence;

import com.umc.product.form.domain.FormSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FormSectionJpaRepository extends JpaRepository<FormSection, Long> {

    /**
     * 특정 폼에 속한 모든 섹션 삭제 (deleteForm cascade 용)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FormSection fs WHERE fs.form.id = :formId")
    int deleteByFormId(@Param("formId") Long formId);
}
