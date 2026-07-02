package com.umc.product.challenger.adapter.out.persistence;

import com.umc.product.challenger.domain.ChallengerRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengerRecordJpaRepository extends JpaRepository<ChallengerRecord, Long> {

    Optional<ChallengerRecord> findByCode(String code);

    boolean existsByCode(String code);

    List<ChallengerRecord> findBySchoolId(Long schoolId);

    List<ChallengerRecord> findByChapterId(Long chapterId);
}
