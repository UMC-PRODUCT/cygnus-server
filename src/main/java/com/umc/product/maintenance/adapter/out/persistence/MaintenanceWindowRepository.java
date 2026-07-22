package com.umc.product.maintenance.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.maintenance.domain.MaintenanceWindow;

public interface MaintenanceWindowRepository extends JpaRepository<MaintenanceWindow, Long> {

    @Query(value = """
        SELECT /* p6spy:exclude */ *
        FROM maintenance_window
        WHERE forced_ended_at IS NULL
          AND start_at <= :now
          AND end_at > :now
        ORDER BY start_at DESC
        """, nativeQuery = true)
    List<MaintenanceWindow> findActiveAt(@Param("now") Instant now);

    @Query("""
        SELECT w FROM MaintenanceWindow w
        WHERE w.forcedEndedAt IS NULL
          AND w.startAt > :now
        ORDER BY w.startAt ASC
        """)
    List<MaintenanceWindow> findUpcoming(@Param("now") Instant now);

    @Query("""
        SELECT w FROM MaintenanceWindow w
        WHERE w.forcedEndedAt IS NULL
          AND w.startAt < :endAt
          AND w.endAt > :startAt
        """)
    List<MaintenanceWindow> findOverlapping(
        @Param("startAt") Instant startAt,
        @Param("endAt") Instant endAt
    );

    List<MaintenanceWindow> findAllByOrderByCreatedAtDesc();

    @Override
    Optional<MaintenanceWindow> findById(Long id);
}
