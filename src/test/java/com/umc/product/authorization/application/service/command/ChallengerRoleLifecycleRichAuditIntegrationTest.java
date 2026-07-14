package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.authorization.application.port.in.command.dto.DeleteChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.UpdateChallengerRoleCommand;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@DisplayName("역할 수명주기 rich audit PostgreSQL 통합 계약")
class ChallengerRoleLifecycleRichAuditIntegrationTest extends ChallengerRoleAuditIntegrationSupport {

    @Test
    @DisplayName("역할 부여는 actor와 대상 회원 및 역할명 snapshot을 저장한다")
    void storesActorTargetRoleSnapshotOnCreate() throws Exception {
        // given
        RoleContext context = roleContext("역할부여");

        // when
        Long roleId = manageChallengerRoleUseCase.createChallengerRole(
            createRoleCommand(context, ChallengerRoleType.SCHOOL_PRESIDENT)
        );

        // then
        awaitExplicitRowCount("ChallengerRole", 1);
        AuditRow row = explicitRow("ChallengerRole", roleId, "CREATE");
        JsonNode details = details(row);
        assertRoleSnapshot(row, context, new RoleExpectation(roleId, "SCHOOL_PRESIDENT"));
        assertThat(details.path("before").isEmpty()).isTrue();
        assertThat(details.path("after")).isEqualTo(details.path("target"));
        assertThat(roleCount(roleId)).isOne();
    }

    @Test
    @DisplayName("역할 변경은 before와 after 역할명 snapshot을 저장한다")
    void storesBeforeAfterRoleSnapshotOnUpdate() throws Exception {
        // given
        RoleContext context = roleContext("역할변경");
        Long roleId = manageChallengerRoleUseCase.createChallengerRole(
            createRoleCommand(context, ChallengerRoleType.SCHOOL_PRESIDENT)
        );
        awaitExplicitRowCount("ChallengerRole", 1);
        UpdateChallengerRoleCommand command = UpdateChallengerRoleCommand.builder()
            .challengerRoleId(roleId)
            .roleType(ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .organizationId(context.school().getId())
            .actorMemberId(context.actor().getId())
            .build();

        // when
        manageChallengerRoleUseCase.updateChallengerRole(command);

        // then
        awaitExplicitRowCount("ChallengerRole", 2);
        AuditRow row = explicitRow("ChallengerRole", roleId, "UPDATE");
        JsonNode details = details(row);
        assertRoleSnapshot(row, context, new RoleExpectation(roleId, "SCHOOL_VICE_PRESIDENT"));
        assertThat(details.path("before").path("roleName").asText()).isEqualTo("SCHOOL_PRESIDENT");
        assertThat(details.path("after").path("roleName").asText())
            .isEqualTo("SCHOOL_VICE_PRESIDENT");
        assertThat(roleType(roleId)).isEqualTo("SCHOOL_VICE_PRESIDENT");
    }

    @Test
    @DisplayName("역할 해제는 삭제 전 snapshot을 저장하고 역할 row를 삭제한다")
    void storesBeforeSnapshotOnDelete() throws Exception {
        // given
        RoleContext context = roleContext("역할해제");
        Long roleId = manageChallengerRoleUseCase.createChallengerRole(
            createRoleCommand(context, ChallengerRoleType.SCHOOL_PRESIDENT)
        );
        awaitExplicitRowCount("ChallengerRole", 1);
        manageChallengerRoleUseCase.updateChallengerRole(UpdateChallengerRoleCommand.builder()
            .challengerRoleId(roleId)
            .roleType(ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .organizationId(context.school().getId())
            .actorMemberId(context.actor().getId())
            .build());
        awaitExplicitRowCount("ChallengerRole", 2);
        DeleteChallengerRoleCommand command = DeleteChallengerRoleCommand.builder()
            .challengerRoleId(roleId)
            .actorMemberId(context.actor().getId())
            .build();

        // when
        manageChallengerRoleUseCase.deleteChallengerRole(command);

        // then
        awaitExplicitRowCount("ChallengerRole", 3);
        AuditRow row = explicitRow("ChallengerRole", roleId, "DELETE");
        JsonNode details = details(row);
        assertRoleSnapshot(row, context, new RoleExpectation(roleId, "SCHOOL_VICE_PRESIDENT"));
        assertThat(details.path("before")).isEqualTo(details.path("target"));
        assertThat(details.path("after").isEmpty()).isTrue();
        assertThat(roleCount(roleId)).isZero();
    }
}
