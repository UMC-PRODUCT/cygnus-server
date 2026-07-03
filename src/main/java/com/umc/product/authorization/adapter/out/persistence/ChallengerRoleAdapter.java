package com.umc.product.authorization.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.application.port.out.SaveChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.common.domain.enums.ChallengerRoleType;

import lombok.RequiredArgsConstructor;

/**
 * ChallengerRole Persistence Adapter
 */
@Component
@RequiredArgsConstructor
public class ChallengerRoleAdapter implements LoadChallengerRolePort, SaveChallengerRolePort {

    private final ChallengerRoleJpaRepository jpaRepository;
    private final ChallengerRoleQueryRepository queryRepository;

    @Override
    public List<ChallengerRole> findByMemberId(Long memberId) {
        return queryRepository.findByMemberId(memberId);
    }

    @Override
    public List<ChallengerRole> findRolesByMemberIdAndGisuId(Long memberId, Long gisuId) {
        return queryRepository.findByMemberIdAndGisuId(memberId, gisuId);
    }

    @Override
    public List<ChallengerRole> findByChallengerIdIn(Set<Long> challengerIds) {
        return jpaRepository.findByChallengerIdIn(challengerIds);
    }

    @Override
    public Optional<ChallengerRole> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public ChallengerRole getById(Long id) {
        return jpaRepository.findById(id)
            .orElseThrow(() -> new AuthorizationDomainException(AuthorizationErrorCode.CHALLENGER_ROLE_NOT_FOUND));
    }

    @Override
    public boolean existsByChallengerRole(
        Long challengerId, ChallengerRoleType roleType, Long organizationId, Long gisuId
    ) {
        return queryRepository.existsByChallengerRole(challengerId, roleType, organizationId, gisuId);
    }

    @Override
    public ChallengerRole save(ChallengerRole challengerRole) {
        return jpaRepository.save(challengerRole);
    }

    @Override
    public List<ChallengerRole> saveAll(List<ChallengerRole> challengerRoles) {
        return jpaRepository.saveAll(challengerRoles);
    }

    @Override
    public void delete(ChallengerRole challengerRole) {
        jpaRepository.delete(challengerRole);
    }
}
