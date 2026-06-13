package com.umc.product.global.websocket.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MimeTypeUtils;

@DisplayName("ApiResponseStompErrorHandler")
class ApiResponseStompErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiResponseStompErrorHandler sut = new ApiResponseStompErrorHandler(objectMapper);

    @Test
    @DisplayName("BusinessException은 해당 에러 코드의 ApiResponse ERROR 프레임으로 변환된다")
    void business_exception_is_converted_to_api_response_error_frame() throws Exception {
        Message<byte[]> clientMessage = stompMessageWithReceipt("receipt-1");

        Message<byte[]> result = sut.handleClientMessageProcessingError(
            clientMessage,
            new AuthorizationDomainException(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED)
        );

        JsonNode body = objectMapper.readTree(result.getPayload());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo("AUTHORIZATION-0002");
        assertThat(body.path("message").asText()).isEqualTo("해당 리소스에 접근할 권한이 없습니다.");
        assertThat(body.has("result")).isFalse();

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor.getCommand()).isEqualTo(StompCommand.ERROR);
        assertThat(accessor.getContentType()).isEqualTo(MimeTypeUtils.APPLICATION_JSON);
        assertThat(accessor.getMessage()).isEqualTo("해당 리소스에 접근할 권한이 없습니다.");
        assertThat(accessor.getReceiptId()).isEqualTo("receipt-1");
    }

    @Test
    @DisplayName("감싸진 BusinessException도 원래 에러 코드로 변환된다")
    void wrapped_business_exception_is_converted_to_original_error_code() throws Exception {
        RuntimeException exception = new RuntimeException(new CommonException(CommonErrorCode.SECURITY_NOT_GIVEN));

        Message<byte[]> result = sut.handleClientMessageProcessingError(null, exception);

        JsonNode body = objectMapper.readTree(result.getPayload());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo("SECURITY-0001");
        assertThat(body.path("message").asText()).isEqualTo("인증 정보가 전달되지 않았습니다.");
        assertThat(body.has("result")).isFalse();
    }

    @Test
    @DisplayName("알 수 없는 예외는 공통 서버 에러 코드로 변환된다")
    void unknown_exception_is_converted_to_internal_server_error() throws Exception {
        Message<byte[]> result = sut.handleClientMessageProcessingError(null, new IllegalStateException("boom"));

        JsonNode body = objectMapper.readTree(result.getPayload());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asText()).isEqualTo("COMMON-0001");
        assertThat(body.path("message").asText()).isEqualTo("알 수 없는 오류입니다. 관리자에게 문의해주세요.");
        assertThat(body.has("result")).isFalse();
    }

    private Message<byte[]> stompMessageWithReceipt(String receipt) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/chat/rooms/10/messages");
        accessor.setReceipt(receipt);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
