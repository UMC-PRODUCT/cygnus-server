package com.umc.product.member.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.member.application.port.out.LoadMemberSystemRolePort;
import com.umc.product.member.domain.MemberSystemRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberSystemRolePersistenceAdapter implements LoadMemberSystemRolePort {

    private final MemberSystemRoleJpaRepository memberSystemRoleJpaRepository;

    @Override
    public List<MemberSystemRole> listByMemberId(Long memberId) {
        return memberSystemRoleJpaRepository.findAllByMemberId(memberId);
    }
}
