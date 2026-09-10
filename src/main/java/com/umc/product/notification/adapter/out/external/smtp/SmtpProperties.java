package com.umc.product.notification.adapter.out.external.smtp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.notification.email.smtp")
@ConditionalOnProperty(prefix = "app.notification.email", name = "provider", havingValue = "smtp")
public record SmtpProperties(
    @DefaultValue("smtp.gmail.com") @NotBlank String host,
    @DefaultValue("587") @Min(1) @Max(65535) int port,
    @NotBlank String username,
    @NotBlank String password
) {

    @Override
    public String toString() {
        return "SmtpProperties[credentials=REDACTED]";
    }
}
