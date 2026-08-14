package com.umc.product.demoday.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.demoday.domain.DemodayPoll;

public interface DemodayPollJpaRepository extends JpaRepository<DemodayPoll, Long> {
}
