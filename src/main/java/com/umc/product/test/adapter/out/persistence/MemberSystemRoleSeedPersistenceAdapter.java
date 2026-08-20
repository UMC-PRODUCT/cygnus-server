package com.umc.product.test.adapter.out.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.umc.product.member.domain.MemberSystemRoleType;
import com.umc.product.test.application.port.out.AssignSeedMemberSystemRolePort;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class MemberSystemRoleSeedPersistenceAdapter implements AssignSeedMemberSystemRolePort {

    private final EntityManager entityManager;

    @Override
    public boolean assignIfAbsent(Long memberId, MemberSystemRoleType roleType) {
        int inserted = entityManager.createNativeQuery("""
                INSERT INTO member_system_role (created_at, updated_at, member_id, role_type)
                VALUES (now(), now(), :memberId, :roleType)
                ON CONFLICT (member_id, role_type) DO NOTHING
                """)
            .setParameter("memberId", memberId)
            .setParameter("roleType", roleType.name())
            .executeUpdate();

        return inserted == 1;
    }
}
