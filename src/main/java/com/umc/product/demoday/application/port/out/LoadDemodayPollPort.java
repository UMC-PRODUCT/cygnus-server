package com.umc.product.demoday.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.demoday.domain.DemodayPoll;

public interface LoadDemodayPollPort {

    Optional<DemodayPoll> findById(Long pollId);

    List<DemodayPoll> listAll();
}
