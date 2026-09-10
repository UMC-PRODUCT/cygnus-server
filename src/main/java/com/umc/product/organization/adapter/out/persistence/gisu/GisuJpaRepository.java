package com.umc.product.organization.adapter.out.persistence.gisu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.umc.product.organization.domain.Gisu;

import jakarta.persistence.LockModeType;

public interface GisuJpaRepository extends Repository<Gisu, Long> {

    @Query("SELECT g FROM Gisu g WHERE g.isActive = true")
    Optional<Gisu> findByIsActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Gisu g WHERE g.isActive = true")
    Optional<Gisu> findActiveWithLock();

    Optional<Gisu> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Gisu g WHERE g.id = :id")
    Optional<Gisu> findByIdForUpdate(Long id);

    List<Gisu> findAllByOrderByGenerationDesc();

    Page<Gisu> findAllByOrderByGenerationDesc(Pageable pageable);

    List<Gisu> findByIdIn(Collection<Long> ids);

    List<Gisu> findByGenerationIn(Collection<Long> generations);

    Gisu save(Gisu gisu);

    boolean existsByGeneration(Long generation);

    void delete(Gisu gisu);
}
