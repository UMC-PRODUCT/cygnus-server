package com.umc.product.member.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.member.domain.Member;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    @Query("select m.id from Member m where m.schoolId = :schoolId")
    Set<Long> findAllIdsBySchoolId(Long schoolId);

    @Query("select m.schoolId as schoolId, m.id as memberId from Member m where m.schoolId in :schoolIds")
    List<SchoolMemberIdRow> findAllIdsBySchoolIds(@Param("schoolIds") Collection<Long> schoolIds);

    @Query("""
        select m.id
        from Member m
        where m.id in :candidateMemberIds
          and lower(m.name) like lower(concat('%', :name, '%'))
        """)
    Set<Long> searchIdsByName(
        @Param("candidateMemberIds") Collection<Long> candidateMemberIds,
        @Param("name") String name
    );

    @Query("SELECT m.id FROM Member m WHERE m.id > :lastId ORDER BY m.id ASC")
    List<Long> findIdsCursor(@Param("lastId") Long lastId, Pageable pageable);

    long countByIdIn(Collection<Long> ids);

    interface SchoolMemberIdRow {
        Long getSchoolId();

        Long getMemberId();
    }
}
