package com.umc.product.chat.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.policy.ChatRoomOwnerPolicyRegistry;
import com.umc.product.chat.application.port.in.query.GetChatOwnershipNamespaceCoverageUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatOwnershipNamespaceCoverageInfo;
import com.umc.product.chat.application.port.out.LoadChatOwnershipNamespacesPort;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatOwnershipNamespaceCoverageService
    implements GetChatOwnershipNamespaceCoverageUseCase {

    private final LoadChatOwnershipNamespacesPort loadNamespacesPort;
    private final ChatRoomOwnerPolicyRegistry policyRegistry;

    @Override
    public ChatOwnershipNamespaceCoverageInfo getCoverage() {
        return new ChatOwnershipNamespaceCoverageInfo(
            loadNamespacesPort.loadDistinctNamespaces(),
            List.of(ChatRoomOwnerReference.STANDALONE_NAMESPACE),
            policyRegistry.namespaces()
        );
    }
}
