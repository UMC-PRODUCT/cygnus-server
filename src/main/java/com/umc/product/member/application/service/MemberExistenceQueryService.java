package com.umc.product.member.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.member.application.port.in.query.CheckMemberExistenceUseCase;
import com.umc.product.member.application.port.out.LoadMemberPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberExistenceQueryService implements CheckMemberExistenceUseCase {

    private final LoadMemberPort loadMemberPort;

    @Override
    public boolean existsById(Long memberId) {
        return memberId != null && loadMemberPort.existsById(memberId);
    }
}
