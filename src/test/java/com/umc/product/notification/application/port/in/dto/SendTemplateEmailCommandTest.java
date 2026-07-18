package com.umc.product.notification.application.port.in.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.notification.domain.EmailTemplateType;

class SendTemplateEmailCommandTest {

    @Test
    @DisplayName("template email command는 공개 입력만 소유하고 문자열과 map을 snapshot한다")
    void 공개_입력과_불변_snapshot을_보장한다() {
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
    @DisplayName("command는 subject·template path·HTML을 호출자 입력으로 노출하지 않는다")
    void raw_rendering_input을_노출하지_않는다() {
        assertThat(SendTemplateEmailCommand.class.getRecordComponents())
            .extracting(component -> component.getName())
            .containsExactly("eventId", "recipient", "templateType", "variables", "availableAt");
        assertThat(SendTemplateEmailCommand.class.getDeclaredFields())
            .noneMatch(field -> field.getName().equals("subject"))
            .noneMatch(field -> field.getName().equals("templateResourcePath"))
            .noneMatch(field -> field.getName().equals("htmlContent"));
    }

    @Test
    @DisplayName("null map은 command 단계에서 NPE가 아니라 catalog가 명시적 오류로 처리할 수 있다")
    void null_map을_보존한다() {
        SendTemplateEmailCommand command = new SendTemplateEmailCommand(
            UUID.randomUUID(), "recipient@test.umc.local", EmailTemplateType.RECRUITMENT_FINAL_FAILED, null, null
        );

        assertThat(command.variables()).isNull();
        assertThat(command.availableAt()).isNull();
    }
}
