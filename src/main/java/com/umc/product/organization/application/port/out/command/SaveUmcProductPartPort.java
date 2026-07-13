package com.umc.product.organization.application.port.out.command;

import com.umc.product.organization.domain.UmcProductPart;

public interface SaveUmcProductPartPort {

    UmcProductPart save(UmcProductPart part);

    void delete(UmcProductPart part);
}
