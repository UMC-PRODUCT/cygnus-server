package com.umc.product.notification.application.port.in.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.notification.application.service.EmailTemplateCatalog;
import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

class SendTemplateEmailCommandTest {

    @Test
    @DisplayName("template email command는 공개 입력만 소유하고 문자열과 map을 snapshot한다")
    void testCase001() {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put(" applicantName ", " 지원자 ");

        SendTemplateEmailCommand command = new SendTemplateEmailCommand(
            UUID.randomUUID(),
            " recipient@test.umc.local ",
            EmailTemplateType.RECRUITMENT_FINAL_FAILED,
            variables,
            Instant.parse("2026-07-18T00:00:00Z")
        );
        variables.put("reason", "outside mutation");

        assertThat(command.recipient()).isEqualTo("recipient@test.umc.local");
        assertThat(command.variables()).containsEntry("applicantName", "지원자");
        assertThat(command.variables()).doesNotContainKey("reason");
        assertThat(command.variables()).isUnmodifiable();
    }

    @Test
    @DisplayName("rendering control 변수는 stable EMAIL code로 거부한다")
    void testCase002() {
        EmailTemplateCatalog catalog = new EmailTemplateCatalog(List.of("https://university.neordinary.com"));

        for (String renderingControlKey : List.of("subject", "templateResourcePath", "htmlContent")) {
            SendTemplateEmailCommand command = new SendTemplateEmailCommand(
                UUID.randomUUID(),
                "recipient@test.umc.local",
                EmailTemplateType.RECRUITMENT_FINAL_FAILED,
                Map.of("applicantName", "지원자", renderingControlKey, "caller-controlled"),
                Instant.parse("2026-07-18T00:00:00Z")
            );

            assertThatThrownBy(() -> catalog.validate(command))
                .isInstanceOfSatisfying(EmailDomainException.class, exception -> {
                    assertThat(exception.getBaseCode()).isEqualTo(EmailErrorCode.EMAIL_TEMPLATE_VARIABLES_INVALID);
                    assertThat(exception.getBaseCode().getCode()).isEqualTo("EMAIL-0008");
                });
        }
    }

    @Test
    @DisplayName("null map은 command 단계에서 NPE가 아니라 catalog가 명시적 오류로 처리할 수 있다")
    void testCase003() {
        SendTemplateEmailCommand command = new SendTemplateEmailCommand(
            UUID.randomUUID(), "recipient@test.umc.local", EmailTemplateType.RECRUITMENT_FINAL_FAILED, null, null
        );

        assertThat(command.variables()).isNull();
        assertThat(command.availableAt()).isNull();
    }
}
