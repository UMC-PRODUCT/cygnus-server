package com.umc.product.notification.adapter.out.external.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.umc.product.notification.application.port.out.ValidateFcmTokenPort;
import com.umc.product.notification.application.port.out.dto.FcmSendTarget;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationRequest;
import com.umc.product.notification.application.port.out.dto.FcmTokenValidationResult;
import com.umc.product.notification.domain.exception.FcmDomainException;
import com.umc.product.notification.domain.exception.FcmErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
public class FirebaseFcmTokenValidationAdapter implements ValidateFcmTokenPort {

    private static final boolean DRY_RUN = true;

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public FcmTokenValidationResult validate(FcmTokenValidationRequest request) {
        if (request.targets().isEmpty()) {
            return FcmTokenValidationResult.of(0, 0, List.of(), List.of());
        }

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(toMessage(request), DRY_RUN);
            return toResult(request.targets(), response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 토큰 유효성 검증 실패: tokenCount={}", request.targets().size(), e);
            throw new FcmDomainException(FcmErrorCode.FCM_SEND_FAILED);
        }
    }

    private MulticastMessage toMessage(FcmTokenValidationRequest request) {
        return MulticastMessage.builder()
            .addAllTokens(request.targets().stream().map(FcmSendTarget::token).toList())
            .putData("type", "TOKEN_VALIDATION")
            .build();
    }

    private FcmTokenValidationResult toResult(List<FcmSendTarget> targets, BatchResponse response) {
        List<Long> validTokenIds = new ArrayList<>();
        List<Long> invalidTokenIds = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                validTokenIds.add(targets.get(i).tokenId());
            } else if (isInvalidToken(sendResponse)) {
                invalidTokenIds.add(targets.get(i).tokenId());
            }
        }
        return FcmTokenValidationResult.of(
            response.getSuccessCount(),
            response.getFailureCount(),
            validTokenIds,
            invalidTokenIds
        );
    }

    private boolean isInvalidToken(SendResponse response) {
        if (response.isSuccessful() || response.getException() == null) {
            return false;
        }
        MessagingErrorCode errorCode = response.getException().getMessagingErrorCode();
        return MessagingErrorCode.UNREGISTERED.equals(errorCode)
            || MessagingErrorCode.INVALID_ARGUMENT.equals(errorCode);
    }
}
