package com.umc.product.audit.application.service.command;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.audit.application.port.in.command.SaveAuditLogUseCase;
import com.umc.product.audit.application.port.out.SaveAuditLogPort;
import com.umc.product.audit.domain.AuditDetailsPolicy;
import com.umc.product.audit.domain.AuditLog;
import com.umc.product.audit.domain.AuditLogEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogCommandService implements SaveAuditLogUseCase {

    private static final TypeReference<Map<String, Object>> DETAILS_MAP_TYPE =
        new TypeReference<>() {
        };

    private final SaveAuditLogPort saveAuditLogPort;
    private final ObjectMapper objectMapper;
    private final AuditLogRecordingMonitor recordingMonitor;

    @Override
    public void save(AuditLogEvent event) {
        String detailsJson = serializeDetails(event);

        AuditLog auditLog = AuditLog.from(event, detailsJson, event.ipAddress());
        saveAuditLogPort.save(auditLog);

        log.debug("[AUDIT] domain={}, action={}, target={}:{}, actor={}",
            event.domain(), event.action(), event.targetType(), event.targetId(), event.actorMemberId());
    }

    private String serializeDetails(AuditLogEvent event) {
        if (event.details() == null || event.details().isEmpty()) {
            return null;
        }
        try {
            JsonNode structuredTree = objectMapper.valueToTree(event.details());
            Map<String, Object> structuredDetails = objectMapper.convertValue(
                structuredTree,
                DETAILS_MAP_TYPE
            );
            Map<String, Object> sanitized = AuditDetailsPolicy.sanitizeForStorage(structuredDetails);
            return sanitized.isEmpty() ? null : objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("감사 로그 details 직렬화 실패: {}", e.getMessage());
            recordingMonitor.onProcessingFailure(event, "details_serialization", e);
            return null;
        }
    }
}
