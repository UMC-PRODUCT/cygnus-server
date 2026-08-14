package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.demoday.domain.DemodayBooth;

public interface DemodayBoothJpaRepository extends JpaRepository<DemodayBooth, Long> {

    List<DemodayBooth> findAllByPollId(Long pollId, Sort sort);
}
