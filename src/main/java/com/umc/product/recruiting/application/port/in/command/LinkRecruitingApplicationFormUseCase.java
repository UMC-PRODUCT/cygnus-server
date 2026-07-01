package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.LinkRecruitingApplicationFormCommand;

public interface LinkRecruitingApplicationFormUseCase {

    Long link(LinkRecruitingApplicationFormCommand command);
}
