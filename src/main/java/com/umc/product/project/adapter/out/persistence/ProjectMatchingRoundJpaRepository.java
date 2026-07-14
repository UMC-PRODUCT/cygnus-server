package com.umc.product.project.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.project.domain.ProjectMatchingRound;

import jakarta.persistence.LockModeType;

public interface ProjectMatchingRoundJpaRepository extends JpaRepository<ProjectMatchingRound, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ProjectMatchingRound r where r.id = :id")
    Optional<ProjectMatchingRound> findByIdForUpdate(@Param("id") Long id);

    List<ProjectMatchingRound> findAllByChapterIdOrderByStartsAtAsc(Long chapterId);

    List<ProjectMatchingRound> findAllByOrderByStartsAtAsc();

    @Query("""
        select r
        from ProjectMatchingRound r
        where (:gisuId is null or r.gisuId = :gisuId)
          and (:chapterId is null or r.chapterId = :chapterId)
          and r.startsAt <= coalesce(:time, r.startsAt)
          and r.endsAt >= coalesce(:time, r.endsAt)
        order by r.startsAt asc
        """)
    List<ProjectMatchingRound> findByFilters(
        @Param("gisuId") Long gisuId,
        @Param("chapterId") Long chapterId,
        @Param("time") Instant time
    );

    @Query("""
        select r
        from ProjectMatchingRound r
        where r.chapterId = :chapterId
          and r.startsAt <= :time
          and r.endsAt >= :time
        order by r.startsAt asc
        """)
    List<ProjectMatchingRound> findOpenAt(
        @Param("chapterId") Long chapterId,
        @Param("time") Instant time
    );

    @Query("""
        select r
        from ProjectMatchingRound r
        where r.chapterId = :chapterId
          and r.startsAt <= :decisionDeadline
          and r.decisionDeadline >= :startsAt
        order by r.startsAt asc
        """)
    List<ProjectMatchingRound> findOverlapping(
        @Param("chapterId") Long chapterId,
        @Param("startsAt") Instant startsAt,
        @Param("decisionDeadline") Instant decisionDeadline
    );

    @Query("""
        select r
        from ProjectMatchingRound r
        where r.id <> :id
          and r.chapterId = :chapterId
          and r.startsAt <= :decisionDeadline
          and r.decisionDeadline >= :startsAt
        order by r.startsAt asc
        """)
    List<ProjectMatchingRound> findOverlappingExceptId(
        @Param("id") Long id,
        @Param("chapterId") Long chapterId,
        @Param("startsAt") Instant startsAt,
        @Param("decisionDeadline") Instant decisionDeadline
    );

    List<ProjectMatchingRound> findAllByAutoDecisionExecutedAtIsNullOrderByDecisionDeadlineAsc();
}
