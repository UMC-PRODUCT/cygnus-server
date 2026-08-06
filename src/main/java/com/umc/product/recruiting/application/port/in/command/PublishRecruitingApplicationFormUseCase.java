package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;

public interface PublishRecruitingApplicationFormUseCase {

    void publish(PublishRecruitingApplicationFormCommand command);
}
