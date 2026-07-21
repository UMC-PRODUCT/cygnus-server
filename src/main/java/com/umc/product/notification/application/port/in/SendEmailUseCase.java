package com.umc.product.notification.application.port.in;

import com.umc.product.notification.application.port.in.dto.SendHtmlEmailCommand;
import com.umc.product.notification.application.port.in.dto.SendVerificationEmailCommand;

public interface SendEmailUseCase {

    void sendVerificationEmail(SendVerificationEmailCommand command);

    void sendHtmlEmail(SendHtmlEmailCommand command);
}
