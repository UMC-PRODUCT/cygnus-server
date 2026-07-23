package com.umc.product.community.application.port.in.query.thread.message;

import java.util.Map;

import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageRecipientsQuery;

public interface GetCommunityThreadMessageForRecipientsUseCase {

    Map<Long, CommunityThreadMessageInfo> getMessageForRecipients(
        CommunityThreadMessageRecipientsQuery query
    );
}
