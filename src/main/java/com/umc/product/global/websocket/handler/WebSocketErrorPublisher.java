package com.umc.product.global.websocket.handler;

import com.umc.product.global.response.ApiResponse;
import com.umc.product.global.response.code.BaseCode;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * WebSocket 세션 사용자에게 ApiResponse 형식의 에러 메시지를 전송한다.
 */
@Component
public class WebSocketErrorPublisher {

    private static final String USER_ERROR_DESTINATION = "/queue/errors";

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketErrorPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * WebSocket 에러 이벤트를 받아 사용자의 에러 큐로 응답을 보낸다.
     */
    @EventListener
    public void sendErrorToUser(WebSocketErrorEvent event) {
        sendErrorToUser(event.userName(), event.errorCode());
    }

    private void sendErrorToUser(String userName, BaseCode errorCode) {
        ApiResponse<Object> response = ApiResponse.onFailure(
            errorCode.getCode(),
            errorCode.getMessage(),
            null
        );

        messagingTemplate.convertAndSendToUser(userName, USER_ERROR_DESTINATION, response);
    }
}
