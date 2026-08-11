package com.umc.product.inhouse.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.umc.product.inhouse.application.port.out.command.SaveUmcProductMemberAccountPort;
import com.umc.product.inhouse.application.port.out.query.LoadUmcProductMemberAccountPort;
import com.umc.product.inhouse.domain.UmcProductMemberAccount;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UmcProductMemberAccountPersistenceAdapter
    implements LoadUmcProductMemberAccountPort, SaveUmcProductMemberAccountPort {

    private static final Map<String, InhouseErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
        "uk_umc_product_member_account_member_id",
        InhouseErrorCode.UMC_PRODUCT_ACCOUNT_ALREADY_LINKED
    );

    private final UmcProductMemberAccountJpaRepository repository;

    @Override
    public Optional<UmcProductMemberAccount> findByMemberId(Long memberId) {
        return repository.findByMemberId(memberId);
    }

    @Override
    public List<UmcProductMemberAccount> listByUmcProductMemberId(Long umcProductMemberId) {
        return repository.findAllByUmcProductMemberId(umcProductMemberId);
    }

    @Override
    public boolean existsByMemberId(Long memberId) {
        return memberId != null && repository.existsByMemberId(memberId);
    }

    @Override
    public boolean existsByUmcProductMemberIdAndMemberId(Long umcProductMemberId, Long memberId) {
        return umcProductMemberId != null
            && memberId != null
            && repository.existsByUmcProductMemberIdAndMemberId(umcProductMemberId, memberId);
    }

    @Override
    public UmcProductMemberAccount save(UmcProductMemberAccount account) {
        try {
            return repository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw UmcProductConstraintViolationTranslator.translate(exception, CONSTRAINT_ERROR_CODES);
        }
    }

    @Override
    public void delete(UmcProductMemberAccount account) {
        repository.delete(account);
    }

    @Override
    public void deleteAllByUmcProductMemberId(Long umcProductMemberId) {
        repository.deleteAllByUmcProductMemberId(umcProductMemberId);
    }
}
