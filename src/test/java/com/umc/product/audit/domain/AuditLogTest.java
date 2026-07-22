package com.umc.product.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.audit.application.port.in.query.dto.AuditLogInfo;
import com.umc.product.global.exception.constant.Domain;

@DisplayName("AuditLog")
class AuditLogTest {

    @Test
    @DisplayName("이벤트의 모든 감사 속성을 불변 로그와 조회 DTO로 변환한다")
    void 이벤트를_로그와_조회_DTO로_변환한다() {
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.UPDATE)
            .targetType("Member")
            .targetId("10")
            .actorMemberId(20L)
            .description("회원 수정")
            .details(Map.of("field", "name"))
            .ipAddress("127.0.0.1")
            .build();

        AuditLog log = AuditLog.from(event, "{\"field\":\"name\"}", event.ipAddress());
        Instant createdAt = Instant.parse("2026-07-22T00:00:00Z");
        ReflectionTestUtils.setField(log, "id", 1L);
        ReflectionTestUtils.setField(log, "createdAt", createdAt);
        AuditLogInfo info = AuditLogInfo.from(log);

        assertThat(info).isEqualTo(new AuditLogInfo(
            1L,
            Domain.MEMBER,
            AuditAction.UPDATE,
            "Member",
            "10",
            20L,
            "회원 수정",
            "{\"field\":\"name\"}",
            "127.0.0.1",
            createdAt
        ));
    }
}
