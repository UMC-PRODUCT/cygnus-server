package com.umc.product.demoday.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.demoday.domain.DemodayBooth;

public interface LoadDemodayBoothPort {

    Optional<DemodayBooth> findById(Long boothId);

    Optional<DemodayBooth> findByStampCredentialHash(String stampCredentialHash);

    List<DemodayBooth> listByPollId(Long pollId);
}
