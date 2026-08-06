package com.umc.product.authorization.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "challenger_role")
public class ChallengerRole extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "challenger_id")
    private Long challengerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "role_type")
    private ChallengerRoleType challengerRoleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "organization_type")
    private OrganizationType organizationType;

    // CENTRAL일 경우: null, CHAPTER: chapterId, SCHOOL: schoolId
    @Column(name = "organization_id")
    private Long organizationId;

    // 중앙운영사무국 교육국 소속이거나, 교내 파트장인 경우 명시함.
    @Enumerated(EnumType.STRING)
    @Column(name = "responsible_part")
    private ChallengerPart responsiblePart;

    // 어떤 기수에 부여받은 권한인지 명시 (필수)
    @Column(nullable = false, name = "gisu_id")
    private Long gisuId;


    /**
     * ChallengerRole을 생성합니다.
     * <p>
     * {@code organizationType}은 {@code roleType}에서 자동으로 결정됩니다.
     *
     * @param challengerId    챌린저 ID
     * @param roleType        역할 타입
     * @param organizationId  조직 ID (CENTRAL이면 null, CHAPTER이면 chapterId, SCHOOL이면 schoolId)
     * @param responsiblePart 담당 파트 (CENTRAL_OPERATING_TEAM_MEMBER, CENTRAL_EDUCATION_TEAM_MEMBER, SCHOOL_PART_LEADER의
     *                        경우)
     * @param gisuId          기수 ID
     */
    public static ChallengerRole create(
        Long challengerId, ChallengerRoleType roleType,
        Long organizationId, ChallengerPart responsiblePart, Long gisuId) {
        // TODO: null check

        OrganizationType orgType = roleType.organizationType();

        if (orgType != OrganizationType.CENTRAL && organizationId == null) {
            // 중앙운영사무국이 아닌 이상 organizationId는 필수입니다.
            throw new IllegalArgumentException(roleType + " 역할은 organizationId가 필수입니다.");
        }

        return ChallengerRole.builder()
            .challengerId(challengerId)
            .challengerRoleType(roleType)
            .organizationType(orgType)
            .organizationId(organizationId)
            .responsiblePart(responsiblePart)
            .gisuId(gisuId)
            .build();
    }

    /**
     * ChallengerRole의 역할 타입, 조직 ID, 담당 파트를 수정합니다.
     */
    public void update(ChallengerRoleType roleType, Long organizationId, ChallengerPart responsiblePart) {
        // 조직 유형은 생성하고자 하는 역할의 유형에 따라 자동으로 주어짐
        OrganizationType orgType = roleType.organizationType();

        if (orgType != OrganizationType.CENTRAL && organizationId == null) {
            throw new IllegalArgumentException(roleType + " 역할은 organizationId가 필수입니다.");
        }

        this.challengerRoleType = roleType;
        this.organizationType = orgType;
        this.organizationId = organizationId;
        this.responsiblePart = responsiblePart;
    }
}
