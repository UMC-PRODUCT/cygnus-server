package com.umc.product.member.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.member.domain.MemberSystemRole;

public interface MemberSystemRoleJpaRepository extends JpaRepository<MemberSystemRole, Long> {

    List<MemberSystemRole> findAllByMemberId(Long memberId);
}
