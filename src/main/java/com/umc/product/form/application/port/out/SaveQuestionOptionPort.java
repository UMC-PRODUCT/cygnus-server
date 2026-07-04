package com.umc.product.form.application.port.out;

import com.umc.product.form.domain.QuestionOption;

import java.util.List;

public interface SaveQuestionOptionPort {

    QuestionOption save(QuestionOption option);

    List<QuestionOption> saveAll(List<QuestionOption> questionOptions);

    void deleteById(Long optionId);

    void deleteAllByQuestionId(Long questionId);

    /**
     * 특정 폼에 속한 모든 선택지 삭제 (deleteForm cascade 용)
     */
    void deleteByFormId(Long formId);

    /**
     * 특정 섹션에 속한 모든 선택지 삭제 (deleteSection cascade 용)
     */
    void deleteBySectionId(Long sectionId);
}
