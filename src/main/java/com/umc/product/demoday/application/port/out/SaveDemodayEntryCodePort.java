package com.umc.product.demoday.application.port.out;

import java.util.List;

import com.umc.product.demoday.domain.DemodayEntryCode;

public interface SaveDemodayEntryCodePort {

    DemodayEntryCode save(DemodayEntryCode entryCode);

    List<DemodayEntryCode> saveAll(Iterable<DemodayEntryCode> entryCodes);
}
