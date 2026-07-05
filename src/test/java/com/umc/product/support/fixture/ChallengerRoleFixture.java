package com.umc.product.support.fixture;

import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.adapter.out.persistence.MemberSystemRoleJpaRepository;
import com.umc.product.member.domain.MemberSystemRole;
import com.umc.product.member.domain.MemberSystemRoleType;

@Component
public class ChallengerRoleFixture extends FixtureSupport {

    private final SaveChallengerRolePort saveChallengerRolePort;
    private final LoadChallengerPort loadChallengerPort;
    private final MemberSystemRoleJpaRepository memberSystemRoleJpaRepository;

    public ChallengerRoleFixture(
        SaveChallengerRolePort saveChallengerRolePort,
        LoadChallengerPort loadChallengerPort,
        MemberSystemRoleJpaRepository memberSystemRoleJpaRepository
    ) {
        this.saveChallengerRolePort = saveChallengerRolePort;
        this.loadChallengerPort = loadChallengerPort;
        this.memberSystemRoleJpaRepository = memberSystemRoleJpaRepository;
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

    @Deprecated(since = "v1.6.0", forRemoval = true)
    @SuppressWarnings("removal")
    public ChallengerRole 슈퍼_관리자(Long challengerId, Long gisuId) {
        Challenger challenger = loadChallengerPort.getById(challengerId);
        boolean exists = memberSystemRoleJpaRepository.findAllByMemberId(challenger.getMemberId()).stream()
            .anyMatch(role -> role.getRoleType() == MemberSystemRoleType.SUPER_ADMIN);
        if (!exists) {
            memberSystemRoleJpaRepository.save(
                MemberSystemRole.create(challenger.getMemberId(), MemberSystemRoleType.SUPER_ADMIN));
        }

        return detachedSuperAdminRole(challengerId, gisuId);
    }

    @SuppressWarnings("removal")
    private ChallengerRole detachedSuperAdminRole(Long challengerId, Long gisuId) {
        try {
            var constructor = ChallengerRole.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ChallengerRole role = constructor.newInstance();
            ReflectionTestUtils.setField(role, "challengerId", challengerId);
            ReflectionTestUtils.setField(role, "challengerRoleType", ChallengerRoleType.SUPER_ADMIN);
            ReflectionTestUtils.setField(role, "organizationType", OrganizationType.CENTRAL);
            ReflectionTestUtils.setField(role, "gisuId", gisuId);
            return role;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
