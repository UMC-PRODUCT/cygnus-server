package com.umc.product.authorization.application.service.dto;

import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.SystemRoleType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import java.util.List;
import java.util.Set;

public record AuthoritySnapshotCacheDto(
    Long memberId,
    Long schoolId,
    List<GisuChallengerInfoDto> gisuChallengerInfos,
    List<RoleAttributeDto> challengerRoles,
    Set<SystemRoleType> systemRoles
) {

    public AuthoritySnapshotCacheDto {
        gisuChallengerInfos = gisuChallengerInfos == null ? List.of() : List.copyOf(gisuChallengerInfos);
        challengerRoles = challengerRoles == null ? List.of() : List.copyOf(challengerRoles);
        systemRoles = systemRoles == null ? Set.of() : Set.copyOf(systemRoles);
    }

    public static AuthoritySnapshotCacheDto from(AuthoritySnapshot snapshot) {
        return new AuthoritySnapshotCacheDto(
            snapshot.memberId(),
            snapshot.schoolId(),
            snapshot.gisuChallengerInfos().stream()
                .map(GisuChallengerInfoDto::from)
                .toList(),
            snapshot.challengerRoles().stream()
                .map(RoleAttributeDto::from)
                .toList(),
            snapshot.systemRoles()
        );
    }

    public AuthoritySnapshot toDomain() {
        return AuthoritySnapshot.of(
            memberId,
            schoolId,
            gisuChallengerInfos.stream()
                .map(GisuChallengerInfoDto::toDomain)
                .toList(),
            challengerRoles.stream()
                .map(RoleAttributeDto::toDomain)
                .toList(),
            systemRoles
        );
    }

    public record GisuChallengerInfoDto(
        Long gisuId,
        Long chapterId,
        ChallengerPart part,
        Long challengerId
    ) {

        private static GisuChallengerInfoDto from(GisuChallengerInfo info) {
            return new GisuChallengerInfoDto(
                info.gisuId(),
                info.chapterId(),
                info.part(),
                info.challengerId()
            );
        }

        private GisuChallengerInfo toDomain() {
            return GisuChallengerInfo.builder()
                .gisuId(gisuId)
                .chapterId(chapterId)
                .part(part)
                .challengerId(challengerId)
                .build();
        }
    }

    public record RoleAttributeDto(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId,
        ChallengerPart responsiblePart,
        Long gisuId
    ) {

        private static RoleAttributeDto from(RoleAttribute roleAttribute) {
            return new RoleAttributeDto(
                roleAttribute.roleType(),
                roleAttribute.organizationType(),
                roleAttribute.organizationId(),
                roleAttribute.responsiblePart(),
                roleAttribute.gisuId()
            );
        }

        private RoleAttribute toDomain() {
            return new RoleAttribute(
                roleType,
                organizationType,
                organizationId,
                responsiblePart,
                gisuId
            );
        }
    }
}
