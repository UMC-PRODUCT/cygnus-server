package com.umc.product.authorization.application.service.command;

import java.util.LinkedHashMap;
import java.util.Map;

import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

final class ChallengerRoleAuditEventFactory {

    private ChallengerRoleAuditEventFactory() {
    }

    static RoleSnapshot snapshot(ChallengerRole role, MemberInfo targetMember) {
        Map<String, Object> values = memberValues(targetMember);
        values.put("id", role.getId());
        values.put("roleName", role.getChallengerRoleType().name());
        return new RoleSnapshot(values);
    }

    static RecordAuditLogCommand created(
        RoleSnapshot created,
        Long actorMemberId,
        MemberInfo actorMember
    ) {
        return event(
            AuditAction.CREATE,
            created,
            actorMemberId,
            actorMember,
            Map.of(),
            created.values(),
            "챌린저 역할을 부여했습니다."
        );
    }

    static RecordAuditLogCommand updated(
        RoleSnapshot before,
        RoleSnapshot after,
        Long actorMemberId,
        MemberInfo actorMember
    ) {
        return event(
            AuditAction.UPDATE,
            after,
            actorMemberId,
            actorMember,
            before.values(),
            after.values(),
            "챌린저 역할을 변경했습니다."
        );
    }

    static RecordAuditLogCommand deleted(
        RoleSnapshot deleted,
        Long actorMemberId,
        MemberInfo actorMember
    ) {
        return event(
            AuditAction.DELETE,
            deleted,
            actorMemberId,
            actorMember,
            deleted.values(),
            Map.of(),
            "챌린저 역할을 해제했습니다."
        );
    }

    private static RecordAuditLogCommand event(
        AuditAction action,
        RoleSnapshot target,
        Long actorMemberId,
        MemberInfo actorMember,
        Map<String, Object> before,
        Map<String, Object> after,
        String description
    ) {
        Map<String, Object> details = RecordAuditLogCommand.structuredDetails(
            actor(actorMemberId, actorMember),
            target.values(),
            Map.of(
                "outcome", "SUCCESS",
                "source", "EXPLICIT_RECORDER",
                "resourceType", "ChallengerRole",
                "resourceId", target.id()
            ),
            before,
            after
        );
        return RecordAuditLogCommand.success(
            Domain.AUTHORIZATION,
            action,
            "ChallengerRole",
            String.valueOf(target.id()),
            actorMemberId,
            description,
            details
        );
    }

    private static Map<String, Object> actor(Long actorMemberId, MemberInfo actorMember) {
        if (actorMemberId == null || actorMember == null) {
            return Map.of();
        }
        Map<String, Object> values = memberValues(actorMember);
        values.put("memberId", actorMemberId);
        return values;
    }

    private static Map<String, Object> memberValues(MemberInfo member) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "Member");
        values.put("memberId", member.id());
        values.put("name", member.name());
        values.put("nickname", member.nickname());
        values.put("schoolName", member.schoolName());
        values.put("status", member.status() == null ? null : member.status().name());
        return values;
    }

    record RoleSnapshot(Map<String, Object> values) {

        Long id() {
            return (Long) values.get("id");
        }

    }
}
