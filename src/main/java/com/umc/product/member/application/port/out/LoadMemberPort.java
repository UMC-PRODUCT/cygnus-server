package com.umc.product.member.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.umc.product.member.domain.Member;

public interface LoadMemberPort {
    Optional<Member> findById(Long id);

    Optional<Member> findByIdForUpdate(Long id);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByNickname(String nickname);

    List<Member> findAllByIds(Set<Long> ids);

    Set<Long> searchIdsByName(Set<Long> candidateMemberIds, String name);

    Set<Long> listIdsBySchoolId(Long schoolId);

    Map<Long, Set<Long>> listIdsBySchoolIds(Set<Long> schoolIds);

    boolean existsById(Long id);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    List<Long> findAllIdsCursor(Long lastId, Pageable pageable);

    long countMembersByIds(Set<Long> memberIds);

    long countAllMembers();
}
