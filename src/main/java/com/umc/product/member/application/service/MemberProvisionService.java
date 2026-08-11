package com.umc.product.member.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.port.in.command.CredentialAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.RegisterCredentialByEmailCommand;
import com.umc.product.member.application.port.in.command.ProvisionMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.ProvisionMemberCommand;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberProvisionService implements ProvisionMemberUseCase {

    private final SaveMemberPort saveMemberPort;
    private final LoadMemberPort loadMemberPort;
    private final CredentialAuthenticationUseCase credentialAuthenticationUseCase;

    @Override
    public Long provision(ProvisionMemberCommand command) {
        if (loadMemberPort.existsByEmail(command.email())) {
            throw new MemberDomainException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Member member = saveMemberPort.save(Member.create(
            command.name(),
            command.nickname(),
            command.email(),
            command.schoolId(),
            null
        ));
        credentialAuthenticationUseCase.registerCredentialByEmail(
            RegisterCredentialByEmailCommand.of(member.getId(), command.rawPassword())
        );
        return member.getId();
    }
}
