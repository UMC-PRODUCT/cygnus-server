package com.umc.product.registry.adapter.out.coverage;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.in.query.GetChatOwnershipNamespaceCoverageUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatOwnershipNamespaceCoverageInfo;
import com.umc.product.registry.application.port.out.RegistryNamespaceCoveragePort;
import com.umc.product.registry.domain.RegistryName;
import com.umc.product.registry.domain.RegistryNamespaceCoverage;

@Component
public class ChatRegistryNamespaceCoverageAdapter implements RegistryNamespaceCoveragePort {

    private final GetChatOwnershipNamespaceCoverageUseCase coverageUseCase;

    public ChatRegistryNamespaceCoverageAdapter(
        GetChatOwnershipNamespaceCoverageUseCase coverageUseCase
    ) {
        this.coverageUseCase = coverageUseCase;
    }

    @Override
    public RegistryName registryName() {
        return RegistryName.CHAT_OWNERSHIP;
    }

    @Override
    public RegistryNamespaceCoverage loadCoverage() {
        ChatOwnershipNamespaceCoverageInfo coverage = coverageUseCase.getCoverage();
        return new RegistryNamespaceCoverage(
            coverage.persistedNamespaces(),
            coverage.declaredNamespaces(),
            coverage.evaluatorNamespaces()
        );
    }

}
