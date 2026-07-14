package com.umc.product.audit.application.port.in.command.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.global.exception.constant.Domain;

@DisplayName("명시 감사 기록 command 계약")
class RecordAuditLogCommandTest {

    @Test
    @DisplayName("of factory는 명시 감사 기록에 필요한 전체 필드를 보존한다")
    void of_factory는_전체_필드를_보존한다() {
        // given
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", "invalid_credentials");

        // when
        RecordAuditLogCommand command = command(details, AuditOutcome.FAILURE, AuditSource.SYSTEM);
        details.put("token", "late-secret");

        // then
        assertThat(command.domain()).isEqualTo(Domain.AUTHENTICATION);
        assertThat(command.action()).isEqualTo(AuditAction.LOGIN);
        assertThat(command.targetType()).isEqualTo("Authentication");
        assertThat(command.targetId()).isEqualTo("member-7");
        assertThat(command.actorMemberId()).isEqualTo(7L);
        assertThat(command.description()).isEqualTo("로그인 실패");
        assertThat(command.details()).containsEntry("reason", "invalid_credentials");
        assertThat(command.details()).doesNotContainKey("token");
        assertThat(command.ipAddress()).isEqualTo("198.51.100.7");
        assertThat(command.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(command.source()).isEqualTo(AuditSource.SYSTEM);
        assertThat(command.requestId()).isEqualTo("request-7");
        assertThat(command.traceId()).isEqualTo("trace-7");
        assertThatThrownBy(() -> command.details().put("status", "MUTATED"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("필수 enum과 targetType의 null 또는 blank 입력을 거부한다")
    void 필수값_null과_blank를_거부한다() {
        // when & then
        assertThatThrownBy(() -> RecordAuditLogCommand.of(
            null, AuditAction.LOGIN, "Authentication", null, null, null, null, null,
            AuditOutcome.FAILURE, AuditSource.SYSTEM, null, null
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RecordAuditLogCommand.of(
            Domain.AUTHENTICATION, null, "Authentication", null, null, null, null, null,
            AuditOutcome.FAILURE, AuditSource.SYSTEM, null, null
        )).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RecordAuditLogCommand.of(
            Domain.AUTHENTICATION, AuditAction.LOGIN, " ", null, null, null, null, null,
            AuditOutcome.FAILURE, AuditSource.SYSTEM, null, null
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> command(Map.of(), null, AuditSource.SYSTEM))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> command(Map.of(), AuditOutcome.FAILURE, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("명시 recorder는 ANNOTATION source와 알 수 없는 enum 문자열을 거부한다")
    void 잘못된_source와_enum_문자열을_거부한다() {
        // when & then
        assertThatThrownBy(() -> command(Map.of(), AuditOutcome.SUCCESS, AuditSource.ANNOTATION))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditOutcome.valueOf("UNKNOWN"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditSource.valueOf("UNKNOWN"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("DB 컬럼 길이를 넘는 경계 입력을 거부한다")
    void 컬럼_길이_초과를_거부한다() {
        // when & then
        assertThatThrownBy(() -> RecordAuditLogCommand.of(
            Domain.AUTHENTICATION,
            AuditAction.LOGIN,
            "T".repeat(101),
            "member-7",
            7L,
            "로그인 실패",
            Map.of(),
            "198.51.100.7",
            AuditOutcome.FAILURE,
            AuditSource.SYSTEM,
            "request-7",
            "trace-7"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static RecordAuditLogCommand command(
        Map<String, Object> details,
        AuditOutcome outcome,
        AuditSource source
    ) {
        return RecordAuditLogCommand.of(
            Domain.AUTHENTICATION,
            AuditAction.LOGIN,
            "Authentication",
            "member-7",
            7L,
            "로그인 실패",
            details,
            "198.51.100.7",
            outcome,
            source,
            "request-7",
            "trace-7"
        );
    }
}
