package com.umc.product.inhouse.application.port.out.command;

import com.umc.product.inhouse.domain.UmcProductSquad;

public interface SaveUmcProductSquadPort {

    UmcProductSquad save(UmcProductSquad squad);

    void delete(UmcProductSquad squad);
}
