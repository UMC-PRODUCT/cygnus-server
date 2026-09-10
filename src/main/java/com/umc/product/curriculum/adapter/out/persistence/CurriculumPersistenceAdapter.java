package com.umc.product.curriculum.adapter.out.persistence;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.SaveCurriculumPort;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.global.persistence.ConstraintViolationInspector;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurriculumPersistenceAdapter implements LoadCurriculumPort, SaveCurriculumPort {

    private final CurriculumJpaRepository curriculumJpaRepository;
    private final CurriculumQueryRepository curriculumQueryRepository;

    @Override
    public Optional<Curriculum> findById(Long id) {
        return curriculumJpaRepository.findById(id);
    }

    @Override
    public Optional<CurriculumProjection> findByGisuIdAndPart(Long gisuId, ChallengerPart part) {
        return curriculumQueryRepository.findByGisuIdAndPart(gisuId, part);
    }

    @Override
    public CurriculumProjection getByGisuIdAndPart(Long gisuId, ChallengerPart part) {
        return findByGisuIdAndPart(gisuId, part)
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_NOT_FOUND));
    }

    @Override
    public boolean existsByGisuIdAndPart(Long gisuId, ChallengerPart part) {
        return curriculumJpaRepository.existsByGisuIdAndPart(gisuId, part);
    }

    @Override
    public Optional<CurriculumProjection> findByGisuIdAndTrack(Long gisuId, ChallengerTrack track) {
        return curriculumQueryRepository.findByGisuIdAndTrack(gisuId, track);
    }

    @Override
    public CurriculumProjection getByGisuIdAndTrack(Long gisuId, ChallengerTrack track) {
        return findByGisuIdAndTrack(gisuId, track)
            .orElseThrow(() -> new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_NOT_FOUND));
    }

    @Override
    public boolean existsByGisuIdAndTrack(Long gisuId, ChallengerTrack track) {
        return curriculumJpaRepository.existsByGisuIdAndTrack(gisuId, track);
    }

    @Override
    public Curriculum save(Curriculum curriculum) {
        try {
            return curriculumJpaRepository.saveAndFlush(curriculum);
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolationInspector.matches(e, "uk_curriculum_gisu_id_track")) {
                throw new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_ALREADY_EXISTS, e);
            }
            throw e;
        }
    }

    @Override
    public void delete(Curriculum curriculum) {
        curriculumJpaRepository.delete(curriculum);
    }
}
