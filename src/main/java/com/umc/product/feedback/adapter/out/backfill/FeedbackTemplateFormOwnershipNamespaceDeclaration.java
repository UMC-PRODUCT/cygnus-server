package com.umc.product.feedback.adapter.out.backfill;

import org.springframework.stereotype.Component;

import com.umc.product.feedback.application.policy.FeedbackTemplateOwnerReferenceFactory;
import com.umc.product.form.application.port.out.FormOwnershipNamespaceDeclaration;

@Component
public class FeedbackTemplateFormOwnershipNamespaceDeclaration
    implements FormOwnershipNamespaceDeclaration {

    @Override
    public String namespace() {
        return FeedbackTemplateOwnerReferenceFactory.NAMESPACE;
    }
}
