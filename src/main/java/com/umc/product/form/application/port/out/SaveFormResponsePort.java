package com.umc.product.form.application.port.out;

import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.enums.FormResponseStatus;
import java.util.List;

public interface SaveFormResponsePort {
    FormResponse save(FormResponse formResponse);

    void deleteById(Long formResponseId);

    void deleteAllByIds(List<Long> ids);

    /**
     * @return 삭제된 row 수
     */
    int deleteByFormIdAndStatus(Long formId, FormResponseStatus status);

    /**
     * 특정 폼에 속한 모든 응답 삭제 (deleteForm cascade 용)
     */
    void deleteByFormId(Long formId);
}
