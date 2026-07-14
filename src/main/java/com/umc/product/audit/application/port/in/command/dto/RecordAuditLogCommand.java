package com.umc.product.audit.application.port.in.command.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;

public record RecordAuditLogCommand(
    Domain domain,
    AuditAction action,
    String targetType,
    String targetId,
    Long actorMemberId,
    String description,
    Map<String, Object> details,
    String ipAddress,
    AuditOutcome outcome,
    AuditSource source,
    String requestId,
    String traceId
) {

    private static final int MAX_TARGET_TYPE_LENGTH = 100;
    private static final int MAX_TARGET_ID_LENGTH = 255;
    private static final int MAX_IP_ADDRESS_LENGTH = 45;
    private static final int MAX_CORRELATION_ID_LENGTH = 100;
    private static final int DETAILS_SCHEMA_VERSION = 1;

    public RecordAuditLogCommand {
        domain = Objects.requireNonNull(domain, "감사 로그 domain은 필수입니다.");
        action = Objects.requireNonNull(action, "감사 로그 action은 필수입니다.");
        targetType = requireText(targetType, "감사 로그 targetType은 필수입니다.");
        outcome = Objects.requireNonNull(outcome, "감사 로그 outcome은 필수입니다.");
        source = Objects.requireNonNull(source, "감사 로그 source는 필수입니다.");
        if (source == AuditSource.ANNOTATION) {
            throw new IllegalArgumentException("명시 감사 기록은 ANNOTATION source를 사용할 수 없습니다.");
        }
        requireMaxLength(targetType, MAX_TARGET_TYPE_LENGTH, "targetType");
        requireMaxLength(targetId, MAX_TARGET_ID_LENGTH, "targetId");
        requireMaxLength(ipAddress, MAX_IP_ADDRESS_LENGTH, "ipAddress");
        requireMaxLength(requestId, MAX_CORRELATION_ID_LENGTH, "requestId");
        requireMaxLength(traceId, MAX_CORRELATION_ID_LENGTH, "traceId");
        details = immutableCopy(details);
    }

    public static RecordAuditLogCommand of(
        Domain domain,
        AuditAction action,
        String targetType,
        String targetId,
        Long actorMemberId,
        String description,
        Map<String, Object> details,
        String ipAddress,
        AuditOutcome outcome,
        AuditSource source,
        String requestId,
        String traceId
    ) {
        return new RecordAuditLogCommand(
            domain,
            action,
            targetType,
            targetId,
            actorMemberId,
            description,
            details,
            ipAddress,
            outcome,
            source,
            requestId,
            traceId
        );
    }

    public static RecordAuditLogCommand success(
        Domain domain,
        AuditAction action,
        String targetType,
        String targetId,
        Long actorMemberId,
        String description,
        Map<String, Object> details
    ) {
        return of(
            domain,
            action,
            targetType,
            targetId,
            actorMemberId,
            description,
            details,
            null,
            AuditOutcome.SUCCESS,
            AuditSource.EXPLICIT_RECORDER,
            null,
            null
        );
    }

    public static RecordAuditLogCommand authenticationFailure(
        AuditAction action,
        String targetType,
        String targetId,
        String description,
        Map<String, Object> details
    ) {
        return failure(
            Domain.AUTHENTICATION,
            action,
            targetType,
            targetId,
            description,
            details,
            AuditSource.AUTHENTICATION_SERVICE
        );
    }

    public static RecordAuditLogCommand authorizationFailure(
        AuditAction action,
        String targetType,
        String targetId,
        Long actorMemberId,
        String description,
        Map<String, Object> details
    ) {
        return of(
            Domain.AUTHORIZATION,
            action,
            targetType,
            targetId,
            actorMemberId,
            description,
            details,
            null,
            AuditOutcome.FAILURE,
            AuditSource.AUTHORIZATION_ASPECT,
            null,
            null
        );
    }

    public static Map<String, Object> structuredDetails(
        Map<String, ?> actor,
        Map<String, ?> target,
        Map<String, ?> context,
        Map<String, ?> before,
        Map<String, ?> after
    ) {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("schemaVersion", DETAILS_SCHEMA_VERSION);
        structured.put("actor", copySection(actor));
        structured.put("target", copySection(target));
        structured.put("context", copySection(context));
        structured.put("before", copySection(before));
        structured.put("after", copySection(after));
        return Collections.unmodifiableMap(structured);
    }

    private static RecordAuditLogCommand failure(
        Domain domain,
        AuditAction action,
        String targetType,
        String targetId,
        String description,
        Map<String, Object> details,
        AuditSource source
    ) {
        return of(
            domain,
            action,
            targetType,
            targetId,
            null,
            description,
            details,
            null,
            AuditOutcome.FAILURE,
            source,
            null,
            null
        );
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static void requireMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(
                "감사 로그 %s은(는) %d자를 초과할 수 없습니다.".formatted(fieldName, maxLength)
            );
        }
    }

    private static Map<String, Object> immutableCopy(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    private static Map<String, Object> copySection(Map<String, ?> section) {
        if (section == null || section.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        section.forEach(copy::put);
        return Collections.unmodifiableMap(copy);
    }
}
