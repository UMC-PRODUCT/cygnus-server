package com.umc.product.inhouse.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.inhouse.domain.enums.UmcProductMemberAccountType;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "umc_product_member_account",
    indexes = @Index(name = "ix_umc_product_member_account_umc_member", columnList = "umc_product_member_id")
)
public class UmcProductMemberAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "umc_product_member_id", nullable = false, updatable = false)
    private UmcProductMember umcProductMember;

    @Column(name = "member_id", nullable = false, unique = true, updatable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 32)
    private UmcProductMemberAccountType accountType;

    @Builder(access = AccessLevel.PRIVATE)
    private UmcProductMemberAccount(
        UmcProductMember umcProductMember,
        Long memberId,
        UmcProductMemberAccountType accountType
    ) {
        if (umcProductMember == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_MEMBER_REQUIRED);
        }
        if (memberId == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_MEMBER_ID_REQUIRED);
        }
        if (accountType == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACCOUNT_TYPE_REQUIRED);
        }
        this.umcProductMember = umcProductMember;
        this.memberId = memberId;
        this.accountType = accountType;
    }

    public static UmcProductMemberAccount create(
        UmcProductMember umcProductMember,
        Long memberId,
        UmcProductMemberAccountType accountType
    ) {
        return UmcProductMemberAccount.builder()
            .umcProductMember(umcProductMember)
            .memberId(memberId)
            .accountType(accountType)
            .build();
    }

    public boolean isProvisioned() {
        return accountType == UmcProductMemberAccountType.PROVISIONED;
    }
}
