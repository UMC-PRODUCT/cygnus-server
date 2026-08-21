package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.demoday.domain.DemodayEntryCode;

public interface DemodayEntryCodeJpaRepository extends JpaRepository<DemodayEntryCode, Long> {

    Optional<DemodayEntryCode> findByCodeHash(String codeHash);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT entryCode FROM DemodayEntryCode entryCode WHERE entryCode.codeHash = :codeHash")
    Optional<DemodayEntryCode> findByCodeHashForUpdate(@Param("codeHash") String codeHash);

    List<DemodayEntryCode> findAllByPollIdAndRedeemedAtIsNotNullOrderByRedeemedAtAscIdAsc(Long pollId);
}
