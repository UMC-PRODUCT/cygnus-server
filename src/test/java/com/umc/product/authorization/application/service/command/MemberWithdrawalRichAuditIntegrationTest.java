package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.member.application.port.in.command.ManageMemberUseCase;
import com.umc.product.member.application.port.in.command.RegisterEmailMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.DeleteMemberCommand;
import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.organization.domain.School;
import com.umc.product.support.fixture.SchoolFixture;

@DisplayName("회원 탈퇴 rich audit PostgreSQL 통합 계약")
class MemberWithdrawalRichAuditIntegrationTest extends MemberRoleAuditDatabaseSupport {

    private static final String EMAIL = "withdraw-sensitive@audit.test";
    private static final String RAW_PASSWORD = "Password123!";
    private static final String ACCESS_TOKEN = "sensitive-oauth-access-token";

    @Autowired
    private RegisterEmailMemberUseCase registerEmailMemberUseCase;

    @Autowired
    private ManageMemberUseCase manageMemberUseCase;

    @Autowired
    private SchoolFixture schoolFixture;

    @Test
    @DisplayName("회원 탈퇴는 row 삭제 후에도 탈퇴 전 회원 snapshot을 PostgreSQL JSON에 보존한다")
    void preservesMemberSnapshotAfterWithdrawal() throws Exception {
        // given
        School school = schoolFixture.학교("탈퇴감사대학교");
        Long memberId = registerEmailMemberUseCase.register(EmailRegisterMemberCommand.builder()
            .rawPassword(RAW_PASSWORD)
            .name("탈퇴회원")
            .nickname("탈퇴닉네임")
            .email(EMAIL)
            .schoolId(school.getId())
            .termConsents(List.of())
            .build());
        awaitExplicitRowCount("Member", 1);
        DeleteMemberCommand command = DeleteMemberCommand.builder()
            .memberId(memberId)
            .googleAccessToken(ACCESS_TOKEN)
            .kakaoAccessToken("kakao-sensitive-token")
            .build();

        // when
        manageMemberUseCase.deleteMember(command);

        // then
        awaitExplicitRowCount("Member", 2);
        assertThat(memberCount(memberId)).isZero();
        AuditRow row = explicitRow("Member", memberId, "WITHDRAW");
        JsonNode details = details(row);
        assertThat(details.path("schemaVersion").asInt()).isOne();
        assertThat(details.path("target").path("id").asLong()).isEqualTo(memberId);
        assertThat(details.path("target").path("memberId").asLong()).isEqualTo(memberId);
        assertThat(details.path("target").path("name").asText()).isEqualTo("탈퇴회원");
        assertThat(details.path("target").path("nickname").asText()).isEqualTo("탈퇴닉네임");
        assertThat(details.path("target").path("schoolName").asText()).isEqualTo(school.getName());
        assertThat(details.path("target").path("status").asText()).isEqualTo("ACTIVE");
        assertThat(details.path("before")).isEqualTo(details.path("target"));
        assertThat(details.path("after").isEmpty()).isTrue();
        assertThat(row.detailsJson()).doesNotContain(
            EMAIL,
            RAW_PASSWORD,
            ACCESS_TOKEN,
            "kakao-sensitive-token",
            "email",
            "password",
            "token"
        );
        assertThat(row.description())
            .isEqualTo("회원 탈퇴를 기록했습니다.")
            .doesNotContain("탈퇴회원", "탈퇴닉네임", school.getName());
    }
}
