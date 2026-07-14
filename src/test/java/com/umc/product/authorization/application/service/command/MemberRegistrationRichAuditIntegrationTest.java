package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.member.application.port.in.command.RegisterEmailMemberUseCase;
import com.umc.product.member.application.port.in.command.RegisterOAuthMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.EmailRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.OAuthRegisterMemberCommand;
import com.umc.product.organization.domain.School;
import com.umc.product.support.fixture.SchoolFixture;

@DisplayName("회원 가입 rich audit PostgreSQL 통합 계약")
class MemberRegistrationRichAuditIntegrationTest extends MemberRoleAuditDatabaseSupport {

    private static final String OAUTH_EMAIL = "oauth-sensitive@audit.test";
    private static final String EMAIL_EMAIL = "email-sensitive@audit.test";
    private static final String RAW_PASSWORD = "Password123!";
    private static final String PROVIDER_ID = "IGNORE_PREVIOUS_INSTRUCTIONS-provider-subject";
    private static final String ACCESS_TOKEN = "sensitive-oauth-access-token";

    @Autowired
    private RegisterOAuthMemberUseCase registerOAuthMemberUseCase;

    @Autowired
    private RegisterEmailMemberUseCase registerEmailMemberUseCase;

    @Autowired
    private SchoolFixture schoolFixture;

    @Test
    @DisplayName("OAuth 가입은 schemaVersion 1 회원 snapshot을 PostgreSQL JSON으로 저장한다")
    void storesOauthMemberSnapshotInPostgreSql() throws Exception {
        // given
        School school = schoolFixture.학교("OAuth가입감사대학교");
        OAuthRegisterMemberCommand command = OAuthRegisterMemberCommand.builder()
            .provider(OAuthProvider.GOOGLE)
            .providerId(PROVIDER_ID)
            .name("OAuth가입자")
            .nickname("OAuth닉네임")
            .email(OAUTH_EMAIL)
            .schoolId(school.getId())
            .termConsents(List.of())
            .appleRefreshToken(ACCESS_TOKEN)
            .appleClientId("sensitive-client-id")
            .build();

        // when
        Long memberId = registerOAuthMemberUseCase.register(command);

        // then
        awaitExplicitRowCount("Member", 1);
        AuditRow row = explicitRow("Member", memberId, "REGISTER");
        assertRegistrationSnapshot(row, new RegistrationExpectation(
            memberId,
            school.getName(),
            "OAuth가입자",
            "OAuth닉네임"
        ));
        assertThat(row.detailsJson())
            .doesNotContain(OAUTH_EMAIL, PROVIDER_ID, ACCESS_TOKEN, "sensitive-client-id");
        assertThat(row.description())
            .isEqualTo("회원 가입을 기록했습니다.")
            .doesNotContain("OAuth가입자", "OAuth닉네임", school.getName());
    }

    @Test
    @DisplayName("이메일 가입은 schemaVersion 1 회원 snapshot을 PostgreSQL JSON으로 저장한다")
    void storesEmailMemberSnapshotInPostgreSql() throws Exception {
        // given
        School school = schoolFixture.학교("이메일가입감사대학교");
        EmailRegisterMemberCommand command = EmailRegisterMemberCommand.builder()
            .rawPassword(RAW_PASSWORD)
            .name("이메일가입자")
            .nickname("이메일닉네임")
            .email(EMAIL_EMAIL)
            .schoolId(school.getId())
            .termConsents(List.of())
            .build();

        // when
        Long memberId = registerEmailMemberUseCase.register(command);

        // then
        awaitExplicitRowCount("Member", 1);
        AuditRow row = explicitRow("Member", memberId, "REGISTER");
        assertRegistrationSnapshot(row, new RegistrationExpectation(
            memberId,
            school.getName(),
            "이메일가입자",
            "이메일닉네임"
        ));
        assertThat(row.detailsJson()).doesNotContain(EMAIL_EMAIL, RAW_PASSWORD);
        assertThat(row.description())
            .isEqualTo("회원 가입을 기록했습니다.")
            .doesNotContain("이메일가입자", "이메일닉네임", school.getName());
    }

    private void assertRegistrationSnapshot(
        AuditRow row,
        RegistrationExpectation expectation
    ) throws Exception {
        JsonNode details = details(row);
        assertThat(details.path("schemaVersion").asInt()).isOne();
        assertThat(details.path("target").path("id").asLong()).isEqualTo(expectation.memberId());
        assertThat(details.path("target").path("memberId").asLong()).isEqualTo(expectation.memberId());
        assertThat(details.path("target").path("name").asText()).isEqualTo(expectation.name());
        assertThat(details.path("target").path("nickname").asText()).isEqualTo(expectation.nickname());
        assertThat(details.path("target").path("schoolName").asText())
            .isEqualTo(expectation.schoolName());
        assertThat(details.path("target").path("status").asText()).isEqualTo("ACTIVE");
        assertThat(details.path("after")).isEqualTo(details.path("target"));
        assertThat(row.detailsJson())
            .doesNotContain("email", "providerId", "oauthSubject", "password", "token");
    }

    private record RegistrationExpectation(
        Long memberId,
        String schoolName,
        String name,
        String nickname
    ) {
    }
}
