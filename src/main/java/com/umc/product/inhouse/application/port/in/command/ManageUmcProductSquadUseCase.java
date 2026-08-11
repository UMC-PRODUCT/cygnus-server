package com.umc.product.inhouse.application.port.in.command;

import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductSquadCommand;
import com.umc.product.inhouse.application.port.in.command.dto.CreateUmcProductSquadParticipantCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductSquadCommand;
import com.umc.product.inhouse.application.port.in.command.dto.UpdateUmcProductSquadParticipantCommand;

public interface ManageUmcProductSquadUseCase {

    Long create(CreateUmcProductSquadCommand command);

    void update(UpdateUmcProductSquadCommand command);

    void delete(Long squadId, Long requesterMemberId);

    Long createParticipant(CreateUmcProductSquadParticipantCommand command);

    void updateParticipant(UpdateUmcProductSquadParticipantCommand command);

    void deleteParticipant(Long squadId, Long participantId, Long requesterMemberId);
}
