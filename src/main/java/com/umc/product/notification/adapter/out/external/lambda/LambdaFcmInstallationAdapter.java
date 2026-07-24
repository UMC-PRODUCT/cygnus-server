package com.umc.product.notification.adapter.out.external.lambda;

import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.notification.application.port.in.dto.RegisterFcmTokenCommand;
import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;
import com.umc.product.notification.application.port.out.ManageExternalFcmInstallationPort;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.notification.lambda",
    name = "enabled",
    havingValue = "true"
)
public class LambdaFcmInstallationAdapter implements ManageExternalFcmInstallationPort {

    private final LambdaClient notificationLambdaClient;
    private final NotificationLambdaProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public void register(RegisterFcmTokenCommand command) {
        invoke(new InstallationCommand(
            UUID.randomUUID(),
            "UPSERT",
            Instant.now(),
            command.memberId(),
            command.installationId(),
            command.fcmToken(),
            command.platform(),
            command.appVersion()
        ));
    }

    @Override
    public void unregister(UnregisterFcmTokenCommand command) {
        invoke(new InstallationCommand(
            UUID.randomUUID(),
            "DEACTIVATE",
            Instant.now(),
            command.memberId(),
            command.installationId(),
            null,
            null,
            null
        ));
    }

    private void invoke(InstallationCommand command) {
        InvokeResponse response = notificationLambdaClient.invoke(InvokeRequest.builder()
            .functionName(properties.installationFunctionName())
            .invocationType(InvocationType.REQUEST_RESPONSE)
            .payload(SdkBytes.fromUtf8String(serialize(command)))
            .build());
        if (response.statusCode() == null
            || response.statusCode() < 200
            || response.statusCode() >= 300
            || response.functionError() != null) {
            throw new IllegalStateException("FCM installation Lambda 호출이 실패했습니다.");
        }
    }

    private String serialize(InstallationCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("FCM installation command 직렬화에 실패했습니다.", exception);
        }
    }

    private record InstallationCommand(
        UUID commandId,
        String action,
        Instant occurredAt,
        Long memberId,
        String installationId,
        String fcmToken,
        String platform,
        String appVersion
    ) {
    }
}
