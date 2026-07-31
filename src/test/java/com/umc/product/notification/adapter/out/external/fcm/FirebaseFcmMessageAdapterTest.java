package com.umc.product.notification.adapter.out.external.fcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;

@DisplayName("Firebase FCM 메시지 어댑터")
@ExtendWith(MockitoExtension.class)
class FirebaseFcmMessageAdapterTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Test
    @DisplayName("multicast 부분 실패를 무효 토큰과 재시도 가능 토큰으로 분류한다")
    void send_classifies_partial_failures() throws Exception {
        // given
        BatchResponse batchResponse = batchResponse(List.of(
            successResponse(),
            failedResponse(MessagingErrorCode.UNREGISTERED),
            failedResponse(MessagingErrorCode.INTERNAL),
            failedResponse(MessagingErrorCode.UNAVAILABLE),
            failedResponse(MessagingErrorCode.QUOTA_EXCEEDED)
        ), 1, 4);
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(batchResponse);
        FirebaseFcmMessageAdapter adapter = new FirebaseFcmMessageAdapter(firebaseMessaging);
        FcmSendRequest request = FcmSendRequest.of(
            List.of(
                FcmSendTarget.of(1L, "success-token"),
                FcmSendTarget.of(2L, "unregistered-token"),
                FcmSendTarget.of(3L, "internal-token"),
                FcmSendTarget.of(4L, "unavailable-token"),
                FcmSendTarget.of(5L, "quota-token")
            ),
            "제목",
            "본문",
            Map.of(),
            null,
            null
        );

        // when
        FcmSendResult result = adapter.send(request);

        // then
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(4);
        assertThat(result.invalidTokenIds()).containsExactly(2L);
        assertThat(result.retryableTokenIds()).containsExactly(3L, 4L, 5L);
        verify(firebaseMessaging).sendEachForMulticast(any(MulticastMessage.class));
    }

    private SendResponse successResponse() {
        SendResponse response = org.mockito.Mockito.mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(true);
        return response;
    }

    private SendResponse failedResponse(MessagingErrorCode errorCode) {
        FirebaseMessagingException exception = org.mockito.Mockito.mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(errorCode);
        SendResponse response = org.mockito.Mockito.mock(SendResponse.class);
        given(response.isSuccessful()).willReturn(false);
        given(response.getException()).willReturn(exception);
        return response;
    }

    private BatchResponse batchResponse(List<SendResponse> responses, int successCount, int failureCount) {
        BatchResponse response = org.mockito.Mockito.mock(BatchResponse.class);
        given(response.getSuccessCount()).willReturn(successCount);
        given(response.getFailureCount()).willReturn(failureCount);
        given(response.getResponses()).willReturn(responses);
        return response;
    }
}
