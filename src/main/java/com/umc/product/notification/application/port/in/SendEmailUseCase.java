package com.umc.product.notification.application.port.in;

import com.umc.product.notification.application.port.in.dto.SendTemplateEmailCommand;
import com.umc.product.notification.application.port.in.dto.SendVerificationEmailCommand;
import com.umc.product.notification.application.port.in.dto.TemplateEmailRequestInfo;

public interface SendEmailUseCase {
    void sendVerificationEmail(SendVerificationEmailCommand command);

    TemplateEmailRequestInfo requestTemplateEmail(SendTemplateEmailCommand command);
}
