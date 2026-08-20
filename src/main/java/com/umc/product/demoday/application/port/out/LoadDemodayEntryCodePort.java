package com.umc.product.demoday.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.demoday.domain.DemodayEntryCode;

public interface LoadDemodayEntryCodePort {

    Optional<DemodayEntryCode> findById(Long entryCodeId);

    Optional<DemodayEntryCode> findByCodeHash(String codeHash);

    List<DemodayEntryCode> listRedeemedByPollId(Long pollId);
}
