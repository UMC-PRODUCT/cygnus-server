package com.umc.product.notification.adapter.out.external.fcm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;
import com.umc.product.notification.domain.exception.FcmDomainException;
import com.umc.product.notification.domain.exception.FcmErrorCode;

@DisplayName("Firebase FCM 메시지 어댑터")
@ExtendWith(MockitoExtension.class)
class FirebaseFcmMessageAdapterTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Test
    @DisplayName("대상이 비어 있으면 Firebase를 호출하지 않고 빈 결과를 반환한다")
    void empty_targets_short_circuit() {
        FirebaseFcmMessageAdapter adapter = new FirebaseFcmMessageAdapter(firebaseMessaging);

        FcmSendResult result = adapter.send(FcmSendRequest.of(
            List.of(), "제목", "본문", Map.of(), null, null
        ));

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isZero();
        then(firebaseMessaging).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미지·data·deep link를 Firebase multicast message에 보존한다")
    void optional_payload_is_preserved() throws Exception {
        BatchResponse response = batchResponse(List.of(successResponse()), 1, 0);
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(response);
        FirebaseFcmMessageAdapter adapter = new FirebaseFcmMessageAdapter(firebaseMessaging);

        adapter.send(FcmSendRequest.of(
            List.of(FcmSendTarget.of(1L, "token")),
            "제목",
            "본문",
            Map.of("type", "NOTICE", "deepLink", "custom://link"),
            "https://example.com/image.png",
            "umc://notice/1"
        ));

        ArgumentCaptor<MulticastMessage> captor = ArgumentCaptor.forClass(MulticastMessage.class);
        then(firebaseMessaging).should().sendEachForMulticast(captor.capture());
        MulticastMessage message = captor.getValue();
        Object notification = ReflectionTestUtils.getField(message, "notification");
        assertThat(ReflectionTestUtils.getField(notification, "image"))
            .isEqualTo("https://example.com/image.png");
        assertThat(ReflectionTestUtils.getField(message, "data"))
            .isEqualTo(Map.of("type", "NOTICE", "deepLink", "custom://link"));
    }

    @Test
    @DisplayName("Firebase provider 오류를 FCM 도메인 예외로 변환한다")
    void provider_failure_is_translated() throws Exception {
        FirebaseMessagingException providerFailure = mock(FirebaseMessagingException.class);
        given(providerFailure.getMessage()).willReturn("provider failure");
        given(providerFailure.getSuppressed()).willReturn(new Throwable[0]);
        given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willThrow(providerFailure);
        FirebaseFcmMessageAdapter adapter = new FirebaseFcmMessageAdapter(firebaseMessaging);
        FcmSendRequest request = FcmSendRequest.of(
            List.of(FcmSendTarget.of(1L, "token")), "제목", "본문", Map.of(), null, null
        );

        assertThatThrownBy(() -> adapter.send(request))
            .isInstanceOfSatisfying(FcmDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(FcmErrorCode.FCM_SEND_FAILED)
            );
    }

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
