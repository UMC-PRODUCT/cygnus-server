package com.umc.product.member.application.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.member.domain.Member;

final class MemberAuditEventFactory {

    private MemberAuditEventFactory() {
    }

    static MemberSnapshot snapshot(Member member, String schoolName) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "Member");
        values.put("id", member.getId());
        values.put("memberId", member.getId());
        values.put("name", member.getName());
        values.put("nickname", member.getNickname());
        values.put("schoolName", schoolName);
        values.put("status", member.getStatus().name());
        return new MemberSnapshot(values);
    }

    static RecordAuditLogCommand registered(MemberSnapshot member) {
        return event(
            AuditAction.REGISTER,
            member,
            Map.of(),
            member.values(),
            "회원 가입을 기록했습니다."
        );
    }

    static RecordAuditLogCommand withdrawn(MemberSnapshot member) {
        return event(
            AuditAction.WITHDRAW,
            member,
            member.values(),
            Map.of(),
            "회원 탈퇴를 기록했습니다."
        );
    }

    private static RecordAuditLogCommand event(
        AuditAction action,
        MemberSnapshot member,
        Map<String, Object> before,
        Map<String, Object> after,
        String description
    ) {
        Map<String, Object> details = RecordAuditLogCommand.structuredDetails(
            Map.of(),
            member.values(),
            Map.of(
                "outcome", "SUCCESS",
                "source", "EXPLICIT_RECORDER",
                "resourceType", "Member",
                "resourceId", member.id()
            ),
            before,
            after
        );
        return RecordAuditLogCommand.success(
            Domain.MEMBER,
            action,
            "Member",
            String.valueOf(member.id()),
            null,
            description,
            details
        );
    }

    record MemberSnapshot(Map<String, Object> values) {

        Long id() {
            return (Long) values.get("id");
        }

    }
}
