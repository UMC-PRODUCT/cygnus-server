package com.umc.product.curriculum.application.port.out;

import java.util.Optional;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.domain.Curriculum;

public interface LoadCurriculumPort {

    Optional<Curriculum> findById(Long id);

    Optional<CurriculumProjection> findByGisuIdAndPart(Long gisuId, ChallengerPart part);

    CurriculumProjection getByGisuIdAndPart(Long gisuId, ChallengerPart part);

    boolean existsByGisuIdAndPart(Long gisuId, ChallengerPart part);

    Optional<CurriculumProjection> findByGisuIdAndTrack(Long gisuId, ChallengerTrack track);

    CurriculumProjection getByGisuIdAndTrack(Long gisuId, ChallengerTrack track);

    boolean existsByGisuIdAndTrack(Long gisuId, ChallengerTrack track);
}
