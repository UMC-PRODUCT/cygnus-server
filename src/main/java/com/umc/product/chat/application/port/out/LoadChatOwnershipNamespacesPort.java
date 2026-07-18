package com.umc.product.chat.application.port.out;

import java.util.List;

public interface LoadChatOwnershipNamespacesPort {

    List<String> loadDistinctNamespaces();
}
