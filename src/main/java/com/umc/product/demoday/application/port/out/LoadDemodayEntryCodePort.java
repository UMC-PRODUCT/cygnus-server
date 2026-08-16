package com.umc.product.demoday.application.port.out;

import java.util.Optional;

import com.umc.product.demoday.domain.DemodayEntryCode;

public interface LoadDemodayEntryCodePort {

    Optional<DemodayEntryCode> findById(Long entryCodeId);

    Optional<DemodayEntryCode> findByCodeHash(String codeHash);
}
