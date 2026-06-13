package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import java.util.List;

public interface GetMyChatRoomsUseCase {

    List<ChatRoomSummaryInfo> getMyChatRooms(Long memberId);
}
