package com.umc.product.notification.adapter.out.external.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.umc.product.notification.application.port.out.SendFcmMessagePort;
import com.umc.product.notification.application.port.out.dto.FcmSendRequest;
import com.umc.product.notification.application.port.out.dto.FcmSendResult;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;
import com.umc.product.notification.domain.exception.FcmDomainException;
import com.umc.product.notification.domain.exception.FcmErrorCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
public class FirebaseFcmMessageAdapter implements SendFcmMessagePort {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public FcmSendResult send(FcmSendRequest request) {
        if (request.targets().isEmpty()) {
            return FcmSendResult.of(0, 0, List.of());
        }

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(toMessage(request));
            return toResult(request.targets(), response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 배치 발송 실패: tokenCount={}", request.targets().size(), e);
            throw new FcmDomainException(FcmErrorCode.FCM_SEND_FAILED);
        }
    }

    private MulticastMessage toMessage(FcmSendRequest request) {
        Notification.Builder notificationBuilder = Notification.builder()
            .setTitle(request.title())
            .setBody(request.body());
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            notificationBuilder.setImage(request.imageUrl());
        }

        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
            .addAllTokens(request.targets().stream().map(FcmSendTarget::token).toList())
            .setNotification(notificationBuilder.build());

        Map<String, String> data = new HashMap<>(request.data());
        if (request.deepLink() != null && !request.deepLink().isBlank()) {
            data.putIfAbsent("deepLink", request.deepLink());
        }
        if (!data.isEmpty()) {
            messageBuilder.putAllData(data);
        }

        return messageBuilder.build();
    }

    private FcmSendResult toResult(List<FcmSendTarget> targets, BatchResponse response) {
        List<Long> invalidTokenIds = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (isUnregistered(sendResponse)) {
                invalidTokenIds.add(targets.get(i).tokenId());
            }
        }
        return FcmSendResult.of(response.getSuccessCount(), response.getFailureCount(), invalidTokenIds);
    }

    private boolean isUnregistered(SendResponse response) {
        return !response.isSuccessful()
            && response.getException() != null
            && MessagingErrorCode.UNREGISTERED.equals(response.getException().getMessagingErrorCode());
    }
}
