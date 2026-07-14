package com.umc.product.challenger.application.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@Component
public class ChallengerRecordAuditSnapshotFactory {

    public Map<String, Object> createdDetails(ChallengerRecord record, MemberInfo creator) {
        return details(
            memberSnapshot(record.getCreatedMemberId(), creator),
            recordTargetSnapshot(record)
        );
    }

    public Map<String, Object> consumedDetails(ChallengerRecord record, MemberInfo memberInfo) {
        return details(
            memberSnapshot(memberInfo.id(), memberInfo),
            consumedTargetSnapshot(record, memberInfo)
        );
    }

    private Map<String, Object> consumedTargetSnapshot(ChallengerRecord record, MemberInfo memberInfo) {
        Map<String, Object> target = recordTargetSnapshot(record);
        target.put("memberId", memberInfo.id());
        target.put("name", memberInfo.name());
        target.put("nickname", memberInfo.nickname());
        target.put("schoolName", memberInfo.schoolName());
        return target;
    }

    private Map<String, Object> recordTargetSnapshot(ChallengerRecord record) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("type", "ChallengerRecord");
        target.put("id", record.getId());
        target.put("name", record.getMemberName());
        target.put("term", record.getGisuId());
        target.put("recordType", record.isAdminRecord() ? "CHALLENGER_ROLE" : "CHALLENGER");
        return target;
    }

    private Map<String, Object> memberSnapshot(Long memberId, MemberInfo memberInfo) {
        Map<String, Object> actor = new LinkedHashMap<>();
        actor.put("type", "Member");
        actor.put("memberId", memberId);
        if (memberInfo != null) {
            actor.put("name", memberInfo.name());
            actor.put("nickname", memberInfo.nickname());
            actor.put("schoolName", memberInfo.schoolName());
        }
        return actor;
    }

    private Map<String, Object> details(
        Map<String, Object> actor,
        Map<String, Object> target
    ) {
        return RecordAuditLogCommand.structuredDetails(
            actor,
            target,
            Map.of(),
            Map.of(),
            Map.of()
        );
    }
}
