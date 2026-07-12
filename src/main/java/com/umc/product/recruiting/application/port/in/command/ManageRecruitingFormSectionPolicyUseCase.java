package com.umc.product.recruiting.application.port.in.command;

import com.umc.product.recruiting.application.port.in.command.dto.AddRecruitingFormSectionPolicyCommand;

public interface ManageRecruitingFormSectionPolicyUseCase {

    Long addPolicy(AddRecruitingFormSectionPolicyCommand command);
}
