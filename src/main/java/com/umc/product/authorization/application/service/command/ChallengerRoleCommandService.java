package com.umc.product.authorization.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.DeleteChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.UpdateChallengerRoleCommand;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.application.service.AuthoritySnapshotCacheKeys;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheNamespace;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengerRoleCommandService implements ManageChallengerRoleUseCase {

    private final LoadChallengerRolePort loadChallengerRolePort;
    private final SaveChallengerRolePort saveChallengerRolePort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final CacheUseCase cacheUseCase;

    @Audited(
        domain = Domain.AUTHORIZATION,
        action = AuditAction.CREATE,
        targetType = "ChallengerRole",
        targetId = "#result",
        description = "'ChallengerRole을 생성했습니다.'"
    )
    @Override
    public Long createChallengerRole(CreateChallengerRoleCommand command) {
        ChallengerRole challengerRole = command.toEntity();
        ChallengerRole savedRole = saveChallengerRolePort.save(challengerRole);
        evictAuthoritySnapshot(savedRole);
        return savedRole.getId();
    }

    @Override
    public List<Long> createChallengerRoleBulk(List<CreateChallengerRoleCommand> commands) {
        List<ChallengerRole> challengerRoles = commands.stream()
            .map(CreateChallengerRoleCommand::toEntity)
            .toList();

        List<ChallengerRole> savedRoles = saveChallengerRolePort.saveAll(challengerRoles);
        savedRoles.forEach(this::evictAuthoritySnapshot);

        return savedRoles.stream()
            .map(ChallengerRole::getId)
            .toList();
    }

    @Audited(
        domain = Domain.AUTHORIZATION,
        action = AuditAction.UPDATE,
        targetType = "ChallengerRole",
        targetId = "#command.challengerRoleId()",
        description = "'ChallengerRole을 수정했습니다.'"
    )
    @Override
    public void updateChallengerRole(UpdateChallengerRoleCommand command) {
        ChallengerRole challengerRole = loadChallengerRolePort.getById(command.challengerRoleId());
        challengerRole.update(command.roleType(), command.organizationId(), command.responsiblePart());
        saveChallengerRolePort.save(challengerRole);
        evictAuthoritySnapshot(challengerRole);
    }

    @Audited(
        domain = Domain.AUTHORIZATION,
        action = AuditAction.DELETE,
        targetType = "ChallengerRole",
        targetId = "#command.challengerRoleId()",
        description = "'ChallengerRole을 삭제했습니다.'"
    )
    @Override
    public void deleteChallengerRole(DeleteChallengerRoleCommand command) {
        ChallengerRole challengerRole = loadChallengerRolePort.getById(command.challengerRoleId());
        saveChallengerRolePort.delete(challengerRole);
        evictAuthoritySnapshot(challengerRole);
    }

    private void evictAuthoritySnapshot(ChallengerRole challengerRole) {
        Long memberId = getChallengerUseCase.getById(challengerRole.getChallengerId()).memberId();
        cacheUseCase.evict(CacheNamespace.AUTHORITY_SNAPSHOT, AuthoritySnapshotCacheKeys.member(memberId));
    }
}
