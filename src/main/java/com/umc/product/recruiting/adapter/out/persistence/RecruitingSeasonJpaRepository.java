package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingSeason;

public interface RecruitingSeasonJpaRepository extends JpaRepository<RecruitingSeason, Long> {

    Optional<RecruitingSeason> findByGisuIdAndSchoolId(Long gisuId, Long schoolId);

    boolean existsByGisuIdAndSchoolId(Long gisuId, Long schoolId);

    List<RecruitingSeason> findAllByGisuIdOrderBySchoolIdAscIdAsc(Long gisuId);
}
