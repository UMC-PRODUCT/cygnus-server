package com.umc.product.common.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("ChallengerRoleType")
class ChallengerRoleTypeTest {

    @Test
    @DisplayName("SUPER_ADMIN은 challenger role 유형에 포함되지 않는다")
    void excludes_super_admin() {
        assertThat(Arrays.stream(ChallengerRoleType.values()).map(Enum::name))
            .doesNotContain("SUPER_ADMIN");
    }

    @Test
    @DisplayName("SUPER_ADMIN challenger role 요청은 역직렬화되지 않는다")
    void rejects_super_admin_json() {
        ObjectMapper objectMapper = new ObjectMapper();

        assertThatThrownBy(() -> objectMapper.readValue("\"SUPER_ADMIN\"", ChallengerRoleType.class))
            .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("모든 운영진 역할은 중앙·지부·학교 조직으로 빠짐없이 분류된다")
    void 모든_역할의_조직_유형을_분류한다() {
        assertThat(EnumSet.of(
            ChallengerRoleType.CENTRAL_PRESIDENT,
            ChallengerRoleType.CENTRAL_VICE_PRESIDENT,
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER,
            ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER
        )).allSatisfy(role -> assertThat(role.organizationType()).isEqualTo(OrganizationType.CENTRAL));
        assertThat(ChallengerRoleType.CHAPTER_PRESIDENT.organizationType()).isEqualTo(OrganizationType.CHAPTER);
        assertThat(EnumSet.of(
            ChallengerRoleType.SCHOOL_PRESIDENT,
            ChallengerRoleType.SCHOOL_VICE_PRESIDENT,
            ChallengerRoleType.SCHOOL_PART_LEADER,
            ChallengerRoleType.SCHOOL_ETC_ADMIN
        )).allSatisfy(role -> assertThat(role.organizationType()).isEqualTo(OrganizationType.SCHOOL));
    }

    @Test
    @DisplayName("역할 계층 predicate는 각 경계 역할만 허용한다")
    void 역할_계층을_판정한다() {
        assertThat(ChallengerRoleType.CENTRAL_PRESIDENT.isAtLeastCentralCore()).isTrue();
        assertThat(ChallengerRoleType.CENTRAL_VICE_PRESIDENT.isAtLeastCentralCore()).isTrue();
        assertThat(ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER.isAtLeastCentralCore()).isFalse();
        assertThat(ChallengerRoleType.CENTRAL_EDUCATION_TEAM_MEMBER.isAtLeastCentralMember()).isTrue();
        assertThat(ChallengerRoleType.CHAPTER_PRESIDENT.isAtLeastCentralMember()).isFalse();
        assertThat(ChallengerRoleType.SCHOOL_PRESIDENT.isAtLeastSchoolCore()).isTrue();
        assertThat(ChallengerRoleType.SCHOOL_VICE_PRESIDENT.isAtLeastSchoolCore()).isTrue();
        assertThat(ChallengerRoleType.SCHOOL_PART_LEADER.isAtLeastSchoolCore()).isFalse();
        assertThat(ChallengerRoleType.SCHOOL_ETC_ADMIN.isAtLeastSchoolAdmin()).isTrue();
        assertThat(ChallengerRoleType.CHAPTER_PRESIDENT.isAtLeastSchoolAdmin()).isFalse();
    }
}
