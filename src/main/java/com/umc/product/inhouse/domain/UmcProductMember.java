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

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "introduction", length = 2000)
    private String introduction;

    @Column(name = "profile_image_id")
    private String profileImageId;

    @Builder(access = AccessLevel.PRIVATE)
    private UmcProductMember(
        String name,
        String nickname,
        Long schoolId,
        String introduction,
        String profileImageId
    ) {
        validateName(name);
        validateNickname(nickname);
        this.name = name.trim();
        this.nickname = nickname.trim();
        this.schoolId = schoolId;
        this.introduction = normalizeIntroduction(introduction);
        this.profileImageId = normalizeNullable(profileImageId);
    }

    public static UmcProductMember create(
        String name,
        String nickname,
        Long schoolId,
        String introduction,
        String profileImageId
    ) {
        return UmcProductMember.builder()
            .name(name)
            .nickname(nickname)
            .schoolId(schoolId)
            .introduction(introduction)
            .profileImageId(profileImageId)
            .build();
    }

    public void updateProfile(String introduction, String profileImageId) {
        this.introduction = normalizeIntroduction(introduction);
        this.profileImageId = normalizeNullable(profileImageId);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_NAME_REQUIRED);
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_NICKNAME_REQUIRED);
        }
    }

    private static String normalizeIntroduction(String introduction) {
        return introduction == null ? "" : introduction.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
