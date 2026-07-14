package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.member.application.port.in.command.RegisterOAuthMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.OAuthRegisterMemberCommand;
import com.umc.product.organization.domain.School;
import com.umc.product.support.fixture.SchoolFixture;

@DisplayName("OAuth bulk 회원가입 rich audit PostgreSQL 계약")
class OAuthBulkMemberAuditIntegrationTest extends MemberRoleAuditDatabaseSupport {

    @Autowired
    RegisterOAuthMemberUseCase registerOAuthMemberUseCase;

    @Autowired
    SchoolFixture schoolFixture;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("bulk 가입 성공은 생성된 회원별 snapshot을 커밋 후 저장한다")
    void bulkRegistrationStoresOneCommittedSnapshotPerMember() throws Exception {
        School school = schoolFixture.학교("OAuthBulk감사대학교");
        List<OAuthRegisterMemberCommand> commands = List.of(
            command(school.getId(), 1),
            command(school.getId(), 2)
        );

        List<Long> memberIds = registerOAuthMemberUseCase.batchRegister(commands);

        awaitExplicitRowCount("Member", 2);
        assertThat(memberIds).hasSize(2);
        for (int index = 0; index < memberIds.size(); index++) {
            AuditRow row = explicitRow("Member", memberIds.get(index), "REGISTER");
            JsonNode details = details(row);
            assertThat(details.path("target").path("memberId").asLong())
                .isEqualTo(memberIds.get(index));
            assertThat(details.path("target").path("schoolName").asText())
                .isEqualTo(school.getName());
            assertThat(row.detailsJson()).doesNotContain(
                commands.get(index).email(),
                commands.get(index).providerId()
            );
        }
    }

    @Test
    @DisplayName("bulk 가입 외부 트랜잭션 롤백은 회원과 SUCCESS 감사를 모두 남기지 않는다")
    void bulkRegistrationRollbackLeavesNoMemberOrSuccessAudit() {
        School school = schoolFixture.학교("OAuthBulk롤백대학교");
        List<OAuthRegisterMemberCommand> commands = List.of(
            command(school.getId(), 3),
            command(school.getId(), 4)
        );

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager)
            .executeWithoutResult(status -> {
                registerOAuthMemberUseCase.batchRegister(commands);
                throw new IllegalStateException("bulk rollback probe");
            })).isInstanceOf(IllegalStateException.class);

        relayAuditEvents();
        assertThat(successAuditCount("Member", "REGISTER")).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM member WHERE email IN (?, ?)",
            Long.class,
            commands.get(0).email(),
            commands.get(1).email()
        )).isZero();
    }

    private OAuthRegisterMemberCommand command(Long schoolId, int suffix) {
        return OAuthRegisterMemberCommand.builder()
            .provider(OAuthProvider.GOOGLE)
            .providerId("bulk-provider-subject-" + suffix)
            .name("Bulk가입자" + suffix)
            .nickname("Bulk닉네임" + suffix)
            .email("bulk-audit-" + suffix + "@example.test")
            .schoolId(schoolId)
            .termConsents(List.of())
            .build();
    }
}
