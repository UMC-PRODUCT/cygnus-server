package com.umc.product.curriculum.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.domain.Curriculum;

public interface CurriculumJpaRepository extends JpaRepository<Curriculum, Long> {

    Optional<Curriculum> findByGisuIdAndPart(Long gisuId, ChallengerPart part);

    boolean existsByGisuIdAndPart(Long gisuId, ChallengerPart part);

    boolean existsByGisuIdAndTrack(Long gisuId, ChallengerTrack track);
}
