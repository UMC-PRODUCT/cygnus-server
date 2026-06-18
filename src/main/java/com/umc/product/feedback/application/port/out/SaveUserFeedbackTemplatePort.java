package com.umc.product.feedback.application.port.out;

import com.umc.product.feedback.domain.UserFeedbackTemplate;

public interface SaveUserFeedbackTemplatePort {

    UserFeedbackTemplate save(UserFeedbackTemplate template);
}
