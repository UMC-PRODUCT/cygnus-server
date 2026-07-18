package com.umc.product.registry.adapter.out.coverage;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.in.query.GetFormOwnershipNamespaceCoverageUseCase;
import com.umc.product.form.application.port.in.query.dto.FormOwnershipNamespaceCoverageInfo;
import com.umc.product.registry.application.port.out.RegistryNamespaceCoveragePort;
import com.umc.product.registry.domain.RegistryName;
import com.umc.product.registry.domain.RegistryNamespaceCoverage;

@Component
public class FormRegistryNamespaceCoverageAdapter implements RegistryNamespaceCoveragePort {

    private final GetFormOwnershipNamespaceCoverageUseCase coverageUseCase;

    public FormRegistryNamespaceCoverageAdapter(
        GetFormOwnershipNamespaceCoverageUseCase coverageUseCase
    ) {
        this.coverageUseCase = coverageUseCase;
    }

    @Override
    public RegistryName registryName() {
        return RegistryName.FORM_OWNERSHIP;
    }

    @Override
    public RegistryNamespaceCoverage loadCoverage() {
        FormOwnershipNamespaceCoverageInfo coverage = coverageUseCase.getCoverage();
        return new RegistryNamespaceCoverage(
            coverage.persistedNamespaces(),
            coverage.declaredNamespaces(),
            coverage.evaluatorNamespaces()
        );
    }

}
