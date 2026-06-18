package com.umc.product.feedback.application.port.in.command;

import com.umc.product.feedback.application.port.in.command.dto.CreateUserFeedbackTemplateCommand;
import com.umc.product.feedback.application.port.in.command.dto.UpdateUserFeedbackTemplateCommand;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateDetailInfo;

public interface ManageUserFeedbackTemplateUseCase {

    UserFeedbackTemplateDetailInfo create(CreateUserFeedbackTemplateCommand command);

    UserFeedbackTemplateDetailInfo update(UpdateUserFeedbackTemplateCommand command);

    void delete(Long templateId);
}
