package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.ChatOwnershipNamespaceCoverageInfo;

public interface GetChatOwnershipNamespaceCoverageUseCase {

    ChatOwnershipNamespaceCoverageInfo getCoverage();
}
