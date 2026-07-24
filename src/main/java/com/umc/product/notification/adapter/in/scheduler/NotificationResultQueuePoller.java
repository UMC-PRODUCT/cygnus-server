package com.umc.product.notification.adapter.in.scheduler;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.notification.application.service.EmailDeliveryResultHandler;
import com.umc.product.notification.application.service.EmailDeliveryResultHandler.EmailDeliveryResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.notification.result-consumer",
    name = "enabled",
    havingValue = "true"
)
public class NotificationResultQueuePoller {

    private static final List<String> SUPPORTED_EVENT_TYPES = List.of(
        "notification.email.accepted.v1",
        "notification.email.failed.v1"
    );

    private final SqsClient notificationResultSqsClient;
    private final NotificationResultQueueProperties properties;
    private final ObjectMapper objectMapper;
    private final EmailDeliveryResultHandler resultHandler;

    @Scheduled(fixedDelayString = "${app.notification.result-consumer.poll-interval-ms:1000}")
    public void poll() {
        List<Message> messages = notificationResultSqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .maxNumberOfMessages(properties.maxMessages())
                .visibilityTimeout(properties.visibilityTimeoutSeconds())
                .waitTimeSeconds(properties.waitTimeSeconds())
                .build())
            .messages();
        for (Message message : messages) {
            process(message);
        }
    }

    private void process(Message message) {
        try {
            JsonNode root = objectMapper.readTree(message.body());
            JsonNode envelope = root.has("detail") ? root.get("detail") : root;
            String eventType = envelope.path("eventType").asText();
            if (!SUPPORTED_EVENT_TYPES.contains(eventType)) {
                throw new IllegalArgumentException("지원하지 않는 notification result eventType입니다.");
            }
            EmailDeliveryResult result =
                objectMapper.treeToValue(envelope.path("detail"), EmailDeliveryResult.class);
            resultHandler.handle(result);
            notificationResultSqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .receiptHandle(message.receiptHandle())
                .build());
        } catch (RuntimeException | java.io.IOException exception) {
            log.warn(
                "Notification result 처리에 실패했습니다: messageId={}, errorType={}",
                message.messageId(),
                exception.getClass().getSimpleName()
            );
        }
    }
}
