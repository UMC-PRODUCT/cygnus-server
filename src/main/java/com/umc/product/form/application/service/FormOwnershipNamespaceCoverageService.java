package com.umc.product.form.application.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetFormOwnershipNamespaceCoverageUseCase;
import com.umc.product.form.application.port.in.query.dto.FormOwnershipNamespaceCoverageInfo;
import com.umc.product.form.application.port.out.FormOwnershipNamespaceDeclaration;
import com.umc.product.form.application.port.out.LoadFormOwnershipNamespacesPort;

@Service
@Transactional(readOnly = true)
public class FormOwnershipNamespaceCoverageService
    implements GetFormOwnershipNamespaceCoverageUseCase {

    private final LoadFormOwnershipNamespacesPort loadNamespacesPort;
    private final List<FormOwnershipNamespaceDeclaration> declarations;
    private final FormOwnerPolicyRegistry policyRegistry;

    public FormOwnershipNamespaceCoverageService(
        LoadFormOwnershipNamespacesPort loadNamespacesPort,
        List<FormOwnershipNamespaceDeclaration> declarations,
        FormOwnerPolicyRegistry policyRegistry
    ) {
        this.loadNamespacesPort = loadNamespacesPort;
        this.declarations = List.copyOf(declarations);
        this.policyRegistry = policyRegistry;
    }

    @Override
    public FormOwnershipNamespaceCoverageInfo getCoverage() {
        List<String> declared = new ArrayList<>(declarations.stream()
            .map(FormOwnershipNamespaceDeclaration::namespace)
            .toList());
        declared.add(StandaloneFormOwnerPolicy.NAMESPACE);
        return new FormOwnershipNamespaceCoverageInfo(
            loadNamespacesPort.loadDistinctNamespaces(),
            declared,
            policyRegistry.namespaces()
        );
    }
}
