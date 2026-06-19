package com.umc.product.notification.adapter.out.external.fcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationRequest;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("Firebase FCM 토큰 유효성 검증 어댑터")
@ExtendWith(MockitoExtension.class)
class FirebaseFcmTokenValidationAdapterTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Test
    @DisplayName("dry-run multicast로 토큰을 검증하고 UNREGISTERED 토큰 id를 반환한다")
    void validate_dry_run() throws Exception {
        // given
        SendResponse success = successResponse();
        SendResponse unregistered = failedResponse(MessagingErrorCode.UNREGISTERED);
        BatchResponse batchResponse = batchResponse(List.of(success, unregistered), 1, 1);
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class), eq(true)))
            .willReturn(batchResponse);
        FirebaseFcmTokenValidationAdapter adapter = new FirebaseFcmTokenValidationAdapter(firebaseMessaging);

        // when
        FcmTokenValidationResult result = adapter.validate(FcmTokenValidationRequest.of(List.of(
            FcmSendTarget.of(1L, "valid-token"),
            FcmSendTarget.of(2L, "invalid-token")
        )));

        // then
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.invalidTokenIds()).containsExactly(2L);
        verify(firebaseMessaging).sendEachForMulticast(any(MulticastMessage.class), eq(true));
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
