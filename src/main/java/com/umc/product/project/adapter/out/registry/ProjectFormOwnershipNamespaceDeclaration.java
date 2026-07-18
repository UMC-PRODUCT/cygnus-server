package com.umc.product.project.adapter.out.registry;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.out.FormOwnershipNamespaceDeclaration;
import com.umc.product.project.application.form.ProjectApplicationFormOwnerReferenceFactory;

@Component
public class ProjectFormOwnershipNamespaceDeclaration
    implements FormOwnershipNamespaceDeclaration {

    @Override
    public String namespace() {
        return ProjectApplicationFormOwnerReferenceFactory.NAMESPACE;
    }
}
