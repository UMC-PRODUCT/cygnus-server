package com.umc.product.member.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.member.application.port.in.query.ListMemberSystemRoleUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberSystemRoleInfo;
import com.umc.product.member.application.port.out.LoadMemberSystemRolePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSystemRoleQueryService implements ListMemberSystemRoleUseCase {

    private final LoadMemberSystemRolePort loadMemberSystemRolePort;

    @Override
    public List<MemberSystemRoleInfo> listByMemberId(Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return loadMemberSystemRolePort.listByMemberId(memberId).stream()
            .map(MemberSystemRoleInfo::from)
            .toList();
    }
}
