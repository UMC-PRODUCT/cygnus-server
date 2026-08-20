package com.umc.product.test.application.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.member.application.port.in.query.CheckMemberExistenceUseCase;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.test.application.port.in.command.CreateSeedMemberSystemRoleUseCase;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberSystemRoleCommand;
import com.umc.product.test.application.port.in.command.dto.CreateSeedMemberSystemRoleResult;
import com.umc.product.test.application.port.out.AssignSeedMemberSystemRolePort;

import lombok.RequiredArgsConstructor;

@Service
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class MemberSystemRoleSeedService implements CreateSeedMemberSystemRoleUseCase {

    private final CheckMemberExistenceUseCase checkMemberExistenceUseCase;
    private final AssignSeedMemberSystemRolePort assignSeedMemberSystemRolePort;
    private final EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @Override
    @Transactional
    public CreateSeedMemberSystemRoleResult create(CreateSeedMemberSystemRoleCommand command) {
        if (!checkMemberExistenceUseCase.existsById(command.memberId())) {
            throw new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND);
        }

        boolean created = assignSeedMemberSystemRolePort.assignIfAbsent(
            command.memberId(), command.roleType());

        // 기존 행이 있더라도 이전 테스트에서 남은 instance-local snapshot을 제거한다.
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(command.memberId());

        return CreateSeedMemberSystemRoleResult.of(
            command.memberId(), command.roleType(), created);
    }
}
