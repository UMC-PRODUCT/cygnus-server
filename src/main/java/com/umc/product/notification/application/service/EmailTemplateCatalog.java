package com.umc.product.notification.application.service;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.umc.product.notification.application.port.in.dto.SendTemplateEmailCommand;
import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

@Component
public class EmailTemplateCatalog {

    private static final int MAX_RECIPIENT_LENGTH = 320;
    private static final Map<String, Integer> VARIABLE_LIMITS = Map.of(
        "applicantName", 100,
        "contactSnapshot", 2_000,
        "actionUrl", 2_048,
        "interviewDate", 50,
        "interviewTime", 100,
        "location", 500,
        "acceptedTrack", 100
    );

    private static final Map<EmailTemplateType, TemplateDefinition> DEFINITIONS = definitions();

    private final Set<String> allowedActionOrigins;

    @Autowired
    public EmailTemplateCatalog(EmailTemplateProperties properties) {
        this.allowedActionOrigins = normalizeAllowedOrigins(properties == null
            ? List.of(EmailTemplateProperties.SERVICE_ORIGIN)
            : properties.allowedActionOrigins());
    }

    public EmailTemplateCatalog(List<String> allowedActionOrigins) {
        this(new EmailTemplateProperties(allowedActionOrigins));
    }

    public TemplateDefinition definition(EmailTemplateType type) {
        return definitionOrThrow(type);
    }

    public String subject(EmailTemplateType type) {
        return definition(type).subject();
    }

    public String templateResourcePath(EmailTemplateType type) {
        return definition(type).resourcePath();
    }

    public Set<String> requiredVariableKeys(EmailTemplateType type) {
        return definition(type).requiredVariableKeys();
    }

    public SendTemplateEmailCommand validate(SendTemplateEmailCommand command) {
        if (command == null) {
            throw emailError(EmailErrorCode.EMAIL_TEMPLATE_VARIABLES_INVALID);
        }
        TemplateDefinition definition = definitionOrThrow(command.templateType());
        validateRecipient(command.recipient());
        validateVariables(definition, command.variables());
        return command;
    }

    private TemplateDefinition definitionOrThrow(EmailTemplateType type) {
        if (type == null || !DEFINITIONS.containsKey(type)) {
            throw emailError(EmailErrorCode.EMAIL_TEMPLATE_TYPE_INVALID);
        }
        return DEFINITIONS.get(type);
    }

    private void validateRecipient(String recipient) {
        if (recipient == null || recipient.isBlank() || recipient.length() > MAX_RECIPIENT_LENGTH) {
            throw emailError(EmailErrorCode.EMAIL_RECIPIENT_INVALID);
        }
    }

    private void validateVariables(TemplateDefinition definition, Map<String, String> variables) {
        if (variables == null || !definition.requiredVariableKeys().equals(variables.keySet())) {
            throw emailError(EmailErrorCode.EMAIL_TEMPLATE_VARIABLES_INVALID);
        }
        for (String key : definition.requiredVariableKeys()) {
            String value = variables.get(key);
            int maxLength = VARIABLE_LIMITS.get(key);
            if (value == null || value.isBlank() || value.length() > maxLength) {
                throw emailError(EmailErrorCode.EMAIL_TEMPLATE_VARIABLE_INVALID);
            }
            if (key.equals("actionUrl")) {
                validateActionUrl(value);
            }
        }
    }

    private void validateActionUrl(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw emailError(EmailErrorCode.EMAIL_TEMPLATE_ACTION_URL_INVALID);
        }
        if (!uri.isAbsolute()
            || uri.getScheme() == null
            || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))
            || uri.getUserInfo() != null
            || uri.getHost() == null
            || uri.getRawAuthority() == null
            || uri.getRawAuthority().endsWith(":")) {
            throw emailError(EmailErrorCode.EMAIL_TEMPLATE_ACTION_URL_INVALID);
        }
        String origin = originOf(uri);
        if (!allowedActionOrigins.contains(origin)) {
            throw emailError(EmailErrorCode.EMAIL_TEMPLATE_ACTION_ORIGIN_NOT_ALLOWED);
        }
    }

    private Set<String> normalizeAllowedOrigins(List<String> origins) {
        return (origins == null || origins.isEmpty() ? List.of(EmailTemplateProperties.SERVICE_ORIGIN) : origins).stream()
            .map(origin -> {
                if (origin == null || origin.isBlank()) {
                    throw new IllegalArgumentException("allowed action origin must not be blank");
                }
                URI uri;
                try {
                    uri = URI.create(origin.strip());
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("invalid allowed action origin", exception);
                }
                if (!uri.isAbsolute() || uri.getScheme() == null
                    || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))
                    || uri.getRawPath() != null && !uri.getRawPath().isEmpty() || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || uri.getUserInfo() != null || uri.getHost() == null) {
                    throw new IllegalArgumentException("allowed action origin must be an origin");
                }
                return originOf(uri);
            })
            .collect(Collectors.toUnmodifiableSet());
    }

    private String originOf(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        if (port < 0) {
            port = scheme.equals("https") ? 443 : 80;
        }
        return scheme + "://" + host + ":" + port;
    }

    private EmailDomainException emailError(EmailErrorCode errorCode) {
        return new EmailDomainException(errorCode);
    }

    private static Map<EmailTemplateType, TemplateDefinition> definitions() {
        return Map.of(
            EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST, new TemplateDefinition(
                "[UMC] 서류 전형 합격 및 면접 가능 시간 제출 안내",
                "email/recruitment/document-passed-interview-availability-request",
                Set.of("applicantName", "contactSnapshot", "actionUrl")),
            EmailTemplateType.RECRUITMENT_INTERVIEW_CONFIRMATION, new TemplateDefinition(
                "[UMC] 면접 일정이 확정되었습니다",
                "email/recruitment/interview-confirmation",
                Set.of("applicantName", "interviewDate", "interviewTime", "location", "contactSnapshot")),
            EmailTemplateType.RECRUITMENT_FINAL_PASSED, new TemplateDefinition(
                "[UMC] 최종 합격을 축하드립니다",
                "email/recruitment/final-passed",
                Set.of("applicantName", "acceptedTrack")),
            EmailTemplateType.RECRUITMENT_FINAL_FAILED, new TemplateDefinition(
                "[UMC] 최종 전형 결과를 안내드립니다",
                "email/recruitment/final-failed",
                Set.of("applicantName"))
        );
    }

    public record TemplateDefinition(
        String subject,
        String resourcePath,
        Set<String> requiredVariableKeys
    ) {
        public TemplateDefinition {
            requiredVariableKeys = Set.copyOf(requiredVariableKeys);
        }
    }
}
