package com.umc.product.inhouse.application.port.out.query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.umc.product.inhouse.domain.UmcProductSquadParticipant;

public interface LoadUmcProductSquadParticipantPort {

    UmcProductSquadParticipant getById(Long squadParticipantId);

    List<UmcProductSquadParticipant> listBySquadId(Long squadId);

    List<UmcProductSquadParticipant> listByUmcProductMemberId(Long umcProductMemberId);

    List<UmcProductSquadParticipant> listByUmcProductMemberIds(Collection<Long> umcProductMemberIds);

    boolean existsBySquadId(Long squadId);

    boolean existsByMemberActivityPeriodId(Long memberActivityPeriodId);

    boolean existsOverlappingMemberInSquad(
        Long squadId,
        Long umcProductMemberId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedSquadParticipantId
    );

    boolean existsOverlappingSquadLead(
        Long squadId,
        LocalDate startDate,
        LocalDate endDate,
        Long excludedSquadParticipantId
    );
}
