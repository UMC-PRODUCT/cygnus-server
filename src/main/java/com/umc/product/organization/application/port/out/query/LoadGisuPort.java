package com.umc.product.organization.application.port.out.query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.organization.domain.Gisu;

public interface LoadGisuPort {

    Gisu getActiveGisu();

    /**
     * 활성 기수가 없을 수도 있는 경우(휴지기 등)에 사용합니다.
     */
    Optional<Gisu> findActiveGisu();

    Optional<Gisu> findActiveGisuWithLock();

    Gisu getById(Long gisuId);

    Gisu getByIdForUpdate(Long gisuId);

    List<Gisu> listByIds(Set<Long> gisuIds);

    List<Gisu> listByGenerations(Set<Long> generations);

    List<Gisu> findAll();

    Page<Gisu> findAll(Pageable pageable);

    boolean existsByGeneration(Long generation);

    Optional<Gisu> findGisuByDate(Instant targetDate);
}
