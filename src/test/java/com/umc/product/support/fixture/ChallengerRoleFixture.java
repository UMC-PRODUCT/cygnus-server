package com.umc.product.support.fixture;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@Component
public class ChallengerRoleFixture extends FixtureSupport {

    private final SaveChallengerRolePort saveChallengerRolePort;

    public ChallengerRoleFixture(SaveChallengerRolePort saveChallengerRolePort) {
        this.saveChallengerRolePort = saveChallengerRolePort;
    }

    public ChallengerRole 중앙운영사무국_총괄(Long challengerId, Long gisuId) {
        return saveChallengerRolePort.save(
            ChallengerRole.create(challengerId, ChallengerRoleType.CENTRAL_PRESIDENT, null, null, gisuId));
    }

    public ChallengerRole 중앙운영사무국_부총괄(Long challengerId, Long gisuId) {
        return saveChallengerRolePort.save(
            ChallengerRole.create(challengerId, ChallengerRoleType.CENTRAL_VICE_PRESIDENT, null, null, gisuId));
    }

    public ChallengerRole 중앙운영사무국_일반_운영진(Long challengerId, Long gisuId) {
        return saveChallengerRolePort.save(
            ChallengerRole.create(challengerId, ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, null, null, gisuId));
    }

    // 지부

    public ChallengerRole 지부장(Long challengerId, Long chapterId, Long gisuId) {
        return saveChallengerRolePort.save(
            ChallengerRole.create(challengerId, ChallengerRoleType.CHAPTER_PRESIDENT, chapterId, null, gisuId));
    }

    // 학교

    public ChallengerRole 학교_회장(Long challengerId, Long schoolId, Long gisuId) {
        return saveChallengerRolePort.save(
            ChallengerRole.create(challengerId, ChallengerRoleType.SCHOOL_PRESIDENT, schoolId, null, gisuId));
    }

    public ChallengerRole 학교_부회장(Long challengerId, Long schoolId, Long gisuId) {
        return saveChallengerRolePort.save(
            ChallengerRole.create(challengerId, ChallengerRoleType.SCHOOL_VICE_PRESIDENT, schoolId, null, gisuId));
    }

    public ChallengerRole 학교_파트장(Long challengerId, ChallengerPart part, Long schoolId, Long gisuId) {
        return saveChallengerRolePort.save(
            ChallengerRole.create(challengerId, ChallengerRoleType.SCHOOL_PART_LEADER, schoolId, part, gisuId));
    }

    public ChallengerRole 학교_기타_운영진(Long challengerId, ChallengerPart part, Long schoolId, Long gisuId) {
        return saveChallengerRolePort.save(
            ChallengerRole.create(challengerId, ChallengerRoleType.SCHOOL_ETC_ADMIN, schoolId, part, gisuId));
    }
}
