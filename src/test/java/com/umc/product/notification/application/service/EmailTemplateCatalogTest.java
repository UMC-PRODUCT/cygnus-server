package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.notification.application.port.in.dto.SendTemplateEmailCommand;
import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

class EmailTemplateCatalogTest {

    private static final String ACTION_URL = "https://university.neordinary.com/path?x=1#section";
    private static final Instant AVAILABLE_AT = Instant.parse("2026-07-18T00:00:00Z");

    private EmailTemplateCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new EmailTemplateCatalog(new EmailTemplateProperties(List.of(
            " https://university.neordinary.com ",
            "http://localhost"
        )));
    }

    @Test
    @DisplayName("4종 템플릿은 catalog가 제목·경로·정확한 필수 키를 소유한다")
    void catalog가_템플릿_메타데이터를_소유한다() {
        assertThat(catalog.requiredVariableKeys(EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST))
            .containsExactlyInAnyOrder("applicantName", "contactSnapshot", "actionUrl");
        assertThat(catalog.subject(EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST))
            .isEqualTo("[UMC] 서류 전형 합격 및 면접 가능 시간 제출 안내");
        assertThat(catalog.templateResourcePath(EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST))
            .isEqualTo("email/recruitment/document-passed-interview-availability-request");

        assertThat(catalog.requiredVariableKeys(EmailTemplateType.RECRUITMENT_INTERVIEW_CONFIRMATION))
            .containsExactlyInAnyOrder("applicantName", "interviewDate", "interviewTime", "location", "contactSnapshot");
        assertThat(catalog.subject(EmailTemplateType.RECRUITMENT_INTERVIEW_CONFIRMATION))
            .isEqualTo("[UMC] 면접 일정이 확정되었습니다");

        assertThat(catalog.requiredVariableKeys(EmailTemplateType.RECRUITMENT_FINAL_PASSED))
            .containsExactlyInAnyOrder("applicantName", "acceptedTrack");
        assertThat(catalog.subject(EmailTemplateType.RECRUITMENT_FINAL_PASSED))
            .isEqualTo("[UMC] 최종 합격을 축하드립니다");

        assertThat(catalog.requiredVariableKeys(EmailTemplateType.RECRUITMENT_FINAL_FAILED))
            .containsExactly("applicantName");
        assertThat(catalog.subject(EmailTemplateType.RECRUITMENT_FINAL_FAILED))
            .isEqualTo("[UMC] 최종 전형 결과를 안내드립니다");
    }

    @Test
    @DisplayName("각 템플릿의 최소 정상 요청은 허용하고 값은 strip된 snapshot으로 보존한다")
    void 최소_정상_요청을_허용한다() {
        for (EmailTemplateType type : EmailTemplateType.values()) {
            SendTemplateEmailCommand command = command(type, values(type));

            SendTemplateEmailCommand validated = catalog.validate(command);

            assertThat(validated).isSameAs(command);
            assertThat(validated.recipient()).isEqualTo("applicant@test.umc.local");
            assertThat(validated.variables()).containsEntry("applicantName", "홍길동");
        }
    }

    @Test
    @DisplayName("URL은 허용 origin의 path·query·fragment를 포함할 수 있다")
    void action_url_정상_origin_검증() {
        SendTemplateEmailCommand command = command(
            EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST,
            values(EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST)
        );

        assertThat(catalog.validate(command).variables().get("actionUrl")).isEqualTo(ACTION_URL);
        assertThat(catalog.validate(command(
            EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST,
            Map.of(" applicantName ", " 지원자 ", " contactSnapshot ", " 문의 ", " actionUrl ", "http://localhost/path")
        )).variables().get("actionUrl")).isEqualTo("http://localhost/path");
        assertThat(catalog.validate(command(
            EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST,
            Map.of("applicantName", "지원자", "contactSnapshot", "문의", "actionUrl",
                "HTTPS://UNIVERSITY.NEORDINARY.COM:443/path")
        )).variables().get("actionUrl")).isEqualTo("HTTPS://UNIVERSITY.NEORDINARY.COM:443/path");
    }

    @Test
    @DisplayName("누락·추가·null·blank 변수는 명시적인 EMAIL 오류로 거부한다")
    void 변수_schema_위반을_거부한다() {
        Map<String, String> missing = values(EmailTemplateType.RECRUITMENT_FINAL_PASSED);
        missing.remove("acceptedTrack");
        assertEmailError(() -> catalog.validate(command(EmailTemplateType.RECRUITMENT_FINAL_PASSED, missing)),
            EmailErrorCode.EMAIL_TEMPLATE_VARIABLES_INVALID);

        Map<String, String> unknown = values(EmailTemplateType.RECRUITMENT_FINAL_FAILED);
        unknown.put("reason", "내부 사유");
        assertEmailError(() -> catalog.validate(command(EmailTemplateType.RECRUITMENT_FINAL_FAILED, unknown)),
            EmailErrorCode.EMAIL_TEMPLATE_VARIABLES_INVALID);

        Map<String, String> nullValue = values(EmailTemplateType.RECRUITMENT_FINAL_FAILED);
        nullValue.put("applicantName", null);
        assertEmailError(() -> catalog.validate(command(EmailTemplateType.RECRUITMENT_FINAL_FAILED, nullValue)),
            EmailErrorCode.EMAIL_TEMPLATE_VARIABLE_INVALID);

        Map<String, String> blankValue = values(EmailTemplateType.RECRUITMENT_FINAL_FAILED);
        blankValue.put("applicantName", "   ");
        assertEmailError(() -> catalog.validate(command(EmailTemplateType.RECRUITMENT_FINAL_FAILED, blankValue)),
            EmailErrorCode.EMAIL_TEMPLATE_VARIABLE_INVALID);
    }

    @Test
    @DisplayName("malicious URL 변형은 exact origin 정책으로 거부한다")
    void action_url_공격_변형을_거부한다() {
        assertActionUrlRejected("https://university.neordinary.com.evil.test/path",
            EmailErrorCode.EMAIL_TEMPLATE_ACTION_ORIGIN_NOT_ALLOWED);
        assertActionUrlRejected("https://user@university.neordinary.com/path",
            EmailErrorCode.EMAIL_TEMPLATE_ACTION_URL_INVALID);
        assertActionUrlRejected("https://university.neordinary.com:8443/path",
            EmailErrorCode.EMAIL_TEMPLATE_ACTION_ORIGIN_NOT_ALLOWED);
        assertActionUrlRejected("javascript:alert(1)", EmailErrorCode.EMAIL_TEMPLATE_ACTION_URL_INVALID);
    }

    @Test
    @DisplayName("수신자와 각 변수의 최대 길이 경계를 검증한다")
    void 길이_경계를_검증한다() {
        assertThat(catalog.validate(new SendTemplateEmailCommand(
            UUID.randomUUID(), "a".repeat(320), EmailTemplateType.RECRUITMENT_FINAL_FAILED,
            Map.of("applicantName", "지원자"), AVAILABLE_AT)).recipient()).hasSize(320);
        assertEmailError(() -> catalog.validate(new SendTemplateEmailCommand(
            UUID.randomUUID(), "a".repeat(321), EmailTemplateType.RECRUITMENT_FINAL_FAILED,
            Map.of("applicantName", "지원자"), AVAILABLE_AT)), EmailErrorCode.EMAIL_RECIPIENT_INVALID);
        assertVariableLength(EmailTemplateType.RECRUITMENT_FINAL_FAILED, "applicantName", 100);
        assertVariableLength(EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST,
            "contactSnapshot", 2_000);
        assertVariableLength(EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST,
            "actionUrl", 2_048);
        assertVariableLength(EmailTemplateType.RECRUITMENT_INTERVIEW_CONFIRMATION, "interviewDate", 50);
        assertVariableLength(EmailTemplateType.RECRUITMENT_INTERVIEW_CONFIRMATION, "interviewTime", 100);
        assertVariableLength(EmailTemplateType.RECRUITMENT_INTERVIEW_CONFIRMATION, "location", 500);
        assertVariableLength(EmailTemplateType.RECRUITMENT_FINAL_PASSED, "acceptedTrack", 100);
        assertEmailError(() -> catalog.validate(new SendTemplateEmailCommand(
            UUID.randomUUID(), "receiver@test.local", EmailTemplateType.RECRUITMENT_FINAL_FAILED, null, AVAILABLE_AT
        )), EmailErrorCode.EMAIL_TEMPLATE_VARIABLES_INVALID);
    }

    @Test
    @DisplayName("외부 map 변경과 catalog key set 변경은 snapshot에 영향을 주지 않는다")
    void map_snapshot은_불변이다() {
        Map<String, String> values = new LinkedHashMap<>(values(EmailTemplateType.RECRUITMENT_FINAL_FAILED));
        SendTemplateEmailCommand command = command(EmailTemplateType.RECRUITMENT_FINAL_FAILED, values);
        values.put("reason", "mutated");

        assertThat(command.variables()).doesNotContainKey("reason");
        assertThatThrownBy(() -> command.variables().put("reason", "mutated"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> catalog.requiredVariableKeys(EmailTemplateType.RECRUITMENT_FINAL_FAILED).add("reason"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private void assertActionUrlRejected(String actionUrl, EmailErrorCode expected) {
        Map<String, String> values = values(EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST);
        values.put("actionUrl", actionUrl);
        assertEmailError(() -> catalog.validate(command(
            EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST, values)),
            expected);
    }

    private void assertVariableLength(EmailTemplateType type, String key, int maxLength) {
        Map<String, String> values = values(type);
        String actionPrefix = "https://university.neordinary.com/";
        String value = key.equals("actionUrl") ? actionPrefix + "a".repeat(maxLength - actionPrefix.length()) : "a".repeat(maxLength);
        values.put(key, value);
        assertThat(catalog.validate(command(type, values)).variables().get(key)).hasSize(maxLength);
        values.put(key, value + "a");
        assertEmailError(() -> catalog.validate(command(type, values)), EmailErrorCode.EMAIL_TEMPLATE_VARIABLE_INVALID);
    }

    private SendTemplateEmailCommand command(EmailTemplateType type, Map<String, String> variables) {
        return new SendTemplateEmailCommand(UUID.randomUUID(), " applicant@test.umc.local ", type, variables, AVAILABLE_AT);
    }

    private Map<String, String> values(EmailTemplateType type) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("applicantName", " 홍길동 ");
        switch (type) {
            case RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST -> {
                values.put("contactSnapshot", " 문의 담당자 ");
                values.put("actionUrl", " " + ACTION_URL + " ");
            }
            case RECRUITMENT_INTERVIEW_CONFIRMATION -> {
                values.put("interviewDate", " 2026년 7월 20일 ");
                values.put("interviewTime", " 오후 2시 ");
                values.put("location", " 온라인 ");
                values.put("contactSnapshot", " 문의 담당자 ");
            }
            case RECRUITMENT_FINAL_PASSED -> values.put("acceptedTrack", " PLAN ");
            case RECRUITMENT_FINAL_FAILED -> {
            }
        }
        return values;
    }

    private void assertEmailError(ThrowingCallable action, EmailErrorCode expected) {
        assertThatThrownBy(action)
            .isInstanceOf(EmailDomainException.class)
            .extracting("baseCode")
            .isEqualTo(expected);
    }
}
