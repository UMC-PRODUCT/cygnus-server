package com.umc.product.inhouse.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "umc_product_member")
public class UmcProductMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "introduction", length = 2000)
    private String introduction;

    @Column(name = "profile_image_id")
    private String profileImageId;

    @Builder(access = AccessLevel.PRIVATE)
    private UmcProductMember(Long memberId, String introduction, String profileImageId) {
        validateMemberId(memberId);
        this.memberId = memberId;
        this.introduction = normalizeIntroduction(introduction);
        this.profileImageId = normalizeNullable(profileImageId);
    }

    public static UmcProductMember create(Long memberId, String introduction, String profileImageId) {
        return UmcProductMember.builder()
            .memberId(memberId)
            .introduction(introduction)
            .profileImageId(profileImageId)
            .build();
    }

    public void updateProfile(String introduction, String profileImageId) {
        this.introduction = normalizeIntroduction(introduction);
        this.profileImageId = normalizeNullable(profileImageId);
    }

    private static void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_MEMBER_ID_REQUIRED);
        }
    }

    private static String normalizeIntroduction(String introduction) {
        return introduction == null ? "" : introduction.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
