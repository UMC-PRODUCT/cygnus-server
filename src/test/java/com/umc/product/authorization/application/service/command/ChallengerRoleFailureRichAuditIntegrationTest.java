package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.application.port.in.command.dto.CreateChallengerRoleCommand;
import com.umc.product.authorization.application.port.in.command.dto.UpdateChallengerRoleCommand;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@DisplayName("역할 실패 rich audit PostgreSQL 통합 계약")
class ChallengerRoleFailureRichAuditIntegrationTest extends ChallengerRoleAuditIntegrationSupport {

    @Test
    @DisplayName("실패한 역할 변경은 기존 역할을 유지하고 성공 감사 로그를 남기지 않는다")
    void doesNotPersistSuccessAuditOnFailedRoleUpdate() {
        // given
        RoleContext context = roleContext("역할실패");
        Long roleId = manageChallengerRoleUseCase.createChallengerRole(
            createRoleCommand(context, ChallengerRoleType.SCHOOL_PRESIDENT)
        );
        awaitExplicitRowCount("ChallengerRole", 1);
        UpdateChallengerRoleCommand command = UpdateChallengerRoleCommand.builder()
            .challengerRoleId(roleId)
            .roleType(ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .organizationId(null)
            .actorMemberId(context.actor().getId())
            .build();

        // when & then
        assertThatThrownBy(() -> manageChallengerRoleUseCase.updateChallengerRole(command))
            .isInstanceOf(IllegalArgumentException.class);
        relayAuditEvents();
        assertThat(explicitRows("ChallengerRole")).hasSize(1);
        assertThat(successAuditCount("ChallengerRole", "UPDATE")).isZero();
        assertThat(roleType(roleId)).isEqualTo("SCHOOL_PRESIDENT");
    }

    @Test
    @DisplayName("roleType이 없는 역할 command는 역할과 성공 감사 로그를 만들지 않는다")
    void doesNotPersistSuccessAuditWithoutRoleType() {
        // given
        RoleContext context = roleContext("역할입력검증");

        // when & then
        assertThatThrownBy(() -> CreateChallengerRoleCommand.builder()
            .challengerId(context.challenger().getId())
            .roleType(null)
            .organizationId(context.school().getId())
            .gisuId(context.gisu().getId())
            .actorMemberId(context.actor().getId())
            .build())
            .isInstanceOf(IllegalArgumentException.class);
        relayAuditEvents();
        assertNoRoleOrSuccessAudit();
    }

    @Test
    @DisplayName("존재하지 않는 챌린저 역할 생성은 역할과 성공 감사 로그를 만들지 않는다")
    void doesNotPersistSuccessAuditWhenChallengerMissing() {
        // given
        RoleContext context = roleContext("역할missing챌린저");
        CreateChallengerRoleCommand command = CreateChallengerRoleCommand.builder()
            .challengerId(Long.MAX_VALUE)
            .roleType(ChallengerRoleType.SCHOOL_PRESIDENT)
            .organizationId(context.school().getId())
            .gisuId(context.gisu().getId())
            .actorMemberId(context.actor().getId())
            .build();

        // when & then
        assertThatThrownBy(() -> manageChallengerRoleUseCase.createChallengerRole(command))
            .isInstanceOf(RuntimeException.class);
        relayAuditEvents();
        assertNoRoleOrSuccessAudit();
    }

    @Test
    @DisplayName("존재하지 않는 actor 역할 생성은 역할과 성공 감사 로그를 만들지 않는다")
    void doesNotPersistSuccessAuditWhenActorMissing() {
        // given
        RoleContext context = roleContext("역할missing회원");
        CreateChallengerRoleCommand command = CreateChallengerRoleCommand.builder()
            .challengerId(context.challenger().getId())
            .roleType(ChallengerRoleType.SCHOOL_PRESIDENT)
            .organizationId(context.school().getId())
            .gisuId(context.gisu().getId())
            .actorMemberId(Long.MAX_VALUE)
            .build();

        // when & then
        assertThatThrownBy(() -> manageChallengerRoleUseCase.createChallengerRole(command))
            .isInstanceOf(RuntimeException.class);
        relayAuditEvents();
        assertNoRoleOrSuccessAudit();
    }

    private void assertNoRoleOrSuccessAudit() {
        assertThat(explicitRows("ChallengerRole")).isEmpty();
        assertThat(successAuditCount("ChallengerRole")).isZero();
        assertThat(totalRoleCount()).isZero();
    }
}
