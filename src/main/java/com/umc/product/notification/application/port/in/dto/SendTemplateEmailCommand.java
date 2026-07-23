package com.umc.product.notification.application.port.in.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

import lombok.Builder;

@Builder
public record SendTemplateEmailCommand(
    UUID eventId,
    String recipient,
    EmailTemplateType templateType,
    Map<String, String> variables,
    Instant availableAt
) {

    public SendTemplateEmailCommand {
        recipient = normalizeRecipient(recipient);
        variables = snapshot(variables);
    }

    private static String normalizeRecipient(String value) {
        if (value != null && value.chars().anyMatch(Character::isISOControl)) {
            throw new EmailDomainException(EmailErrorCode.EMAIL_RECIPIENT_INVALID);
        }
        return stripOrNull(value);
    }

    private static String stripOrNull(String value) {
        return value == null ? null : value.strip();
    }

    private static Map<String, String> snapshot(Map<String, String> source) {
        if (source == null) {
            return null;
        }
        Map<String, String> snapshot = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = stripOrNull(key);
            if (snapshot.containsKey(normalizedKey)) {
                throw new EmailDomainException(EmailErrorCode.EMAIL_TEMPLATE_VARIABLES_INVALID);
            }
            snapshot.put(normalizedKey, stripOrNull(value));
        });
        return Collections.unmodifiableMap(snapshot);
    }
}
