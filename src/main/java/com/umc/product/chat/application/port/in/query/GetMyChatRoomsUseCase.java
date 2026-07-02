package com.umc.product.chat.application.port.in.query;

import java.util.List;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;

public interface GetMyChatRoomsUseCase {

    List<ChatRoomSummaryInfo> getMyChatRooms(Long memberId);
}
