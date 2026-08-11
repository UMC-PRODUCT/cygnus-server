package com.umc.product.inhouse.application.port.out.query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.umc.product.inhouse.domain.UmcProductSquad;

public interface LoadUmcProductSquadPort {

    UmcProductSquad getById(Long squadId);

    UmcProductSquad getByIdWithLock(Long squadId);

    List<UmcProductSquad> listAll(Boolean active, LocalDate activeOn);

    List<UmcProductSquad> listByIds(Collection<Long> ids);

    boolean existsByCode(String code, Long excludedSquadId);
}
