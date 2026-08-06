package com.umc.product.chat.application.port.out;

import java.util.List;
import java.util.Map;

import com.umc.product.chat.application.port.out.dto.ChatReactionSummary;

public interface LoadChatMessageReactionPort {

    List<ChatReactionSummary> summarizeByMessageIds(List<Long> messageIds, Long viewerMemberId);

    Map<Long, List<ChatReactionSummary>> summarizeByMessageIdsForViewers(
        List<Long> messageIds,
        List<Long> viewerMemberIds
    );
}
