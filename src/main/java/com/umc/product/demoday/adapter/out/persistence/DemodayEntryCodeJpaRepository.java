package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.demoday.domain.DemodayEntryCode;

public interface DemodayEntryCodeJpaRepository extends JpaRepository<DemodayEntryCode, Long> {

    Optional<DemodayEntryCode> findByCodeHash(String codeHash);

    List<DemodayEntryCode> findAllByPollIdAndRedeemedAtIsNotNullOrderByRedeemedAtAscIdAsc(Long pollId);
}
