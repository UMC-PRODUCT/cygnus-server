package com.umc.product.global.event.application.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.umc.product.global.event.application.port.in.command.RedactEventOutboxPayloadUseCase;
import com.umc.product.global.event.application.port.out.RedactEventOutboxPayloadPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventOutboxRetentionService implements RedactEventOutboxPayloadUseCase {

    private final RedactEventOutboxPayloadPort redactEventOutboxPayloadPort;

    @Override
    public int redactExpiredPayloads(
        Instant now,
        Duration publishedRetention,
        Duration failedRetention,
        int batchSize,
        int maxBatches
    ) {
        validateInputs(now, publishedRetention, failedRetention, batchSize, maxBatches);
        Instant publishedThreshold = now.minus(publishedRetention);
        Instant failedThreshold = now.minus(failedRetention);
        boolean publishedDrained = false;
        boolean failedDrained = false;
        boolean publishedTurn = true;
        int total = 0;

        for (int batch = 0; batch < maxBatches && !(publishedDrained && failedDrained); batch++) {
            if ((publishedTurn && !publishedDrained) || failedDrained) {
                int redacted = redactEventOutboxPayloadPort.redactPublishedBefore(
                    publishedThreshold,
                    now,
                    batchSize
                );
                total += redacted;
                publishedDrained = redacted < batchSize;
            } else {
                int redacted = redactEventOutboxPayloadPort.redactFailedBefore(failedThreshold, now, batchSize);
                total += redacted;
                failedDrained = redacted < batchSize;
            }
            publishedTurn = !publishedTurn;
        }
        return total;
    }

    private void validateInputs(
        Instant now,
        Duration publishedRetention,
        Duration failedRetention,
        int batchSize,
        int maxBatches
    ) {
        if (now == null) {
            throw new IllegalArgumentException("event outbox retention 기준 시각은 필수입니다.");
        }
        if (publishedRetention == null || publishedRetention.isNegative() || publishedRetention.isZero()) {
            throw new IllegalArgumentException("event outbox PUBLISHED retention은 양수여야 합니다.");
        }
        if (failedRetention == null || failedRetention.isNegative() || failedRetention.isZero()) {
            throw new IllegalArgumentException("event outbox FAILED retention은 양수여야 합니다.");
        }
        if (batchSize <= 0 || maxBatches < 2) {
            throw new IllegalArgumentException(
                "event outbox retention batch size는 양수이고 "
                    + "실행당 최대 batch 수는 2 이상이어야 합니다."
            );
        }
    }
}
