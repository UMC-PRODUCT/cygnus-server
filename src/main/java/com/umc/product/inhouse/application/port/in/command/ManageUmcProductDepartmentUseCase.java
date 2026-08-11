package com.umc.product.inhouse.application.port.in.command;

import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductDepartmentCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductDepartmentParticipantCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductDepartmentCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductDepartmentParticipantCommand;

public interface ManageUmcProductDepartmentUseCase {

    Long create(CreateUmcProductDepartmentCommand command);

    void update(UpdateUmcProductDepartmentCommand command);

    void delete(Long departmentId, Long requesterMemberId);

    Long createParticipant(CreateUmcProductDepartmentParticipantCommand command);

    void updateParticipant(UpdateUmcProductDepartmentParticipantCommand command);

    void deleteParticipant(Long departmentId, Long participantId, Long requesterMemberId);
}
