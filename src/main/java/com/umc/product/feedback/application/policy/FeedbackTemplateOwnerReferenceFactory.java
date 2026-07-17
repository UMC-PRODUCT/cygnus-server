package com.umc.product.feedback.application.policy;

import org.springframework.stereotype.Component;

import com.umc.product.form.domain.FormOwnerReference;

/**
 * Feedback template이 Form ownership 좌표를 만드는 trusted factory.
 *
 * <p>template ID와 Form ID는 Feedback persistence에서 읽은 값만 전달받는다. namespace와 slot은
 * HTTP/GraphQL 입력으로 대체할 수 없는 server-owned 상수다.</p>
 */
@Component
public class FeedbackTemplateOwnerReferenceFactory {

    public static final String NAMESPACE = "feedback.template";
    public static final String SLOT = "default";

    public FormOwnerReference create(Long templateId, Long formId) {
        if (templateId == null || templateId <= 0) {
            throw new IllegalArgumentException("feedback template id는 양수여야 합니다.");
        }
        return FormOwnerReference.of(
            formId,
            NAMESPACE,
            String.valueOf(templateId),
            SLOT
        );
    }
}
