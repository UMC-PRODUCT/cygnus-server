package com.umc.product.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.exception.constant.Domain;

@DisplayName("감사 description 개인정보 경계")
class AuditDescriptionPrivacyTest {

    @Test
    @DisplayName("hostile name nickname school token password body와 개행을 저장하지 않는다")
    void hostileDescriptionValuesAreReplacedAtPersistenceBoundary() {
        String hostile = "name=홍길동 nickname=길동 school=비밀대학교\r\n"
            + "token=raw-token password=raw-password body=raw-body";
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.REGISTER)
            .targetType("Member")
            .description(hostile)
            .build();

        AuditLog auditLog = AuditLog.from(event, null, null);

        assertThat(auditLog.getDescription())
            .isEqualTo("감사 이벤트를 기록했습니다.")
            .doesNotContain(
                "홍길동",
                "길동",
                "비밀대학교",
                "raw-token",
                "raw-password",
                "raw-body",
                "\r",
                "\n"
            );
    }
}
