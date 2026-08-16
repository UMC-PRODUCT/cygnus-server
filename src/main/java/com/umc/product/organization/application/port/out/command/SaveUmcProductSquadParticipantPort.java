package com.umc.product.organization.application.port.out.command;

import com.umc.product.organization.domain.UmcProductSquadParticipant;

public interface SaveUmcProductSquadParticipantPort {

    UmcProductSquadParticipant save(UmcProductSquadParticipant participant);

    void delete(UmcProductSquadParticipant participant);

    void deleteAllBySquadId(Long squadId);

    void deleteAllByUmcProductMemberId(Long umcProductMemberId);
}
