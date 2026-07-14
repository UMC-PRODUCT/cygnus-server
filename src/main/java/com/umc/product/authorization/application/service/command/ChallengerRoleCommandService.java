package com.umc.product.authorization.application.service.command;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.authorization.application.port.in.command.ManageChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.DeleteChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.UpdateChallengerRoleCommand;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengerRoleCommandService implements ManageChallengerRoleUseCase {

    private final LoadChallengerRolePort loadChallengerRolePort;
    private final SaveChallengerRolePort saveChallengerRolePort;
    private final GetChallengerUseCase getChallengerUseCase;
    private final EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final RecordAuditLogUseCase recordAuditLogUseCase;

    @Audited(
        domain = Domain.AUTHORIZATION,
        action = AuditAction.CREATE,
        targetType = "ChallengerRole",
        targetId = "#result",
        description = "'ChallengerRole을 생성했습니다.'"
    )
    @Override
    public Long createChallengerRole(CreateChallengerRoleCommand command) {
        RoleMembers members = getRoleMembers(command.challengerId(), command.actorMemberId());
        ChallengerRole challengerRole = command.toEntity();
        ChallengerRole savedRole = saveChallengerRolePort.save(challengerRole);
        evictAuthoritySnapshot(savedRole);
        recordAuditLogUseCase.record(ChallengerRoleAuditEventFactory.created(
            ChallengerRoleAuditEventFactory.snapshot(savedRole, members.target()),
            command.actorMemberId(),
            members.actor()
        ));
        return savedRole.getId();
    }

    @Override
    public List<Long> createChallengerRoleBulk(List<CreateChallengerRoleCommand> commands) {
        if (commands.isEmpty()) {
            return List.of();
        }
        Map<Long, ChallengerInfo> challengers = getChallengers(commands);
        Map<Long, MemberInfo> members = getMembers(commands, challengers);
        List<ChallengerRole> challengerRoles = commands.stream()
            .map(CreateChallengerRoleCommand::toEntity)
            .toList();

        List<ChallengerRole> savedRoles = saveChallengerRolePort.saveAll(challengerRoles);
        challengers.values().stream()
            .map(ChallengerInfo::memberId)
            .distinct()
            .forEach(evictAuthoritySnapshotCacheUseCase::evictByMemberId);
        List<RecordAuditLogCommand> auditCommands = new ArrayList<>(savedRoles.size());
        for (int index = 0; index < savedRoles.size(); index++) {
            CreateChallengerRoleCommand command = commands.get(index);
            Long targetMemberId = challengers.get(command.challengerId()).memberId();
            MemberInfo actor = command.actorMemberId() == null
                ? null
                : members.get(command.actorMemberId());
            auditCommands.add(ChallengerRoleAuditEventFactory.created(
                ChallengerRoleAuditEventFactory.snapshot(savedRoles.get(index), members.get(targetMemberId)),
                command.actorMemberId(),
                actor
            ));
        }
        auditCommands.forEach(recordAuditLogUseCase::record);
        return savedRoles.stream().map(ChallengerRole::getId).toList();
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
        RoleMembers members = getRoleMembers(challengerRole.getChallengerId(), command.actorMemberId());
        ChallengerRoleAuditEventFactory.RoleSnapshot before =
            ChallengerRoleAuditEventFactory.snapshot(challengerRole, members.target());
        challengerRole.update(command.roleType(), command.organizationId(), command.responsiblePart());
        saveChallengerRolePort.save(challengerRole);
        evictAuthoritySnapshot(challengerRole);
        recordAuditLogUseCase.record(ChallengerRoleAuditEventFactory.updated(
            before,
            ChallengerRoleAuditEventFactory.snapshot(challengerRole, members.target()),
            command.actorMemberId(),
            members.actor()
        ));
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
        RoleMembers members = getRoleMembers(challengerRole.getChallengerId(), command.actorMemberId());
        ChallengerRoleAuditEventFactory.RoleSnapshot deleted =
            ChallengerRoleAuditEventFactory.snapshot(challengerRole, members.target());
        saveChallengerRolePort.delete(challengerRole);
        evictAuthoritySnapshot(challengerRole);
        recordAuditLogUseCase.record(ChallengerRoleAuditEventFactory.deleted(
            deleted,
            command.actorMemberId(),
            members.actor()
        ));
    }

    private void evictAuthoritySnapshot(ChallengerRole challengerRole) {
        Long memberId = getChallengerUseCase.getById(challengerRole.getChallengerId()).memberId();
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(memberId);
    }

    private RoleMembers getRoleMembers(Long challengerId, Long actorMemberId) {
        ChallengerInfo challenger = getChallengerUseCase.getById(challengerId);
        MemberInfo target = getMemberUseCase.getById(challenger.memberId());
        MemberInfo actor = actorMemberId == null
            ? null
            : actorMemberId.equals(target.id()) ? target : getMemberUseCase.getById(actorMemberId);
        return new RoleMembers(actor, target);
    }

    private Map<Long, ChallengerInfo> getChallengers(List<CreateChallengerRoleCommand> commands) {
        Set<Long> challengerIds = commands.stream()
            .map(CreateChallengerRoleCommand::challengerId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return getChallengerUseCase.batchGetByIds(challengerIds).stream()
            .collect(Collectors.toMap(ChallengerInfo::challengerId, Function.identity()));
    }

    private Map<Long, MemberInfo> getMembers(
        List<CreateChallengerRoleCommand> commands,
        Map<Long, ChallengerInfo> challengers
    ) {
        Set<Long> memberIds = commands.stream()
            .map(command -> challengers.get(command.challengerId()).memberId())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        commands.stream()
            .map(CreateChallengerRoleCommand::actorMemberId)
            .filter(java.util.Objects::nonNull)
            .forEach(memberIds::add);
        return getMemberUseCase.batchGetByIds(memberIds);
    }

    private record RoleMembers(MemberInfo actor, MemberInfo target) {
    }
}
